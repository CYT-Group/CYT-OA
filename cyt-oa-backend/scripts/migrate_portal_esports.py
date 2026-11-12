"""B4 电竞室迁移：Portal Postgres → OA PostgreSQL（开放规则 / 预约单 / 审批快照 / 流水）。

用法：
    python scripts/migrate_portal_esports.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_esports.py             # 真正写入

行 id 一律原样保留：steps/logs 按 booking_id 引用预约单，重新编号等于把关系打断。
PostgreSQL 里已存在的 id 直接跳过，中途失败可重复执行接着跑。settings 单行表 id 恒 = 1。

口径出处：docs/Portal管理面迁移契约.md §三「电竞室」、§二「类型转换口径」、§四 批次 4。

时间换算（重要）：Portal 的 application/esports.py 所有时间列都用 datetime.utcnow() 显式写入
（create_booking L191/197、act_on_booking L263-279、add_log L116、update_settings L79），
所以 created_at / updated_at / acted_at / reviewed_at / cancelled_at / logs.created_at /
settings.updated_at 一律走 app_utc（恒 +8h），不走 server_clock —— 与 training 的 completed_at 同款。
"""

import argparse
import asyncio
import json
import os
import sys

import asyncpg
from portal_target import connect_target, advance_target_sequences

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal_time as pt  # noqa: E402
from migrate_portal_identity import read_pg_dsn  # noqa: E402

# 插入顺序即外键依赖顺序：settings 先行（无 FK），bookings 再行，steps/logs 引用 bookings
TABLES = [
    ("portal_esports_settings", "portal_esports_settings"),
    ("portal_esports_bookings", "portal_esports_bookings"),
    ("portal_esports_approval_steps", "portal_esports_approval_steps"),
    ("portal_esports_booking_logs", "portal_esports_booking_logs"),
]


def _json(value):
    """JSONB → JSON 文本：asyncpg 读回已解析的 list/dict，重新 dumps 交给 PostgreSQL JSON 列校验。"""
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


def _bool(value):
    """PG boolean → OA PostgreSQL SMALLINT 0/1。"""
    if value is None:
        return None
    return 1 if value else 0


def _time(value):
    """datetime.time / datetime.date 转成 ISO 文本，由 PostgreSQL 目标列解析。"""
    if value is None:
        return None
    return value.isoformat() if hasattr(value, "isoformat") else value


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    out = {}
    for src, _ in TABLES:
        out[src] = await pg.fetch("select * from %s order by id" % src)
    await pg.close()
    return out


def build(src, table_rows, stats):
    """按目标表做列映射与时间换算，返回 (id, 列名字典) 列表。所有时间列统一 app_utc（恒 +8h）。"""
    out = []
    for r in table_rows:
        rid = r["id"]
        if src == "portal_esports_settings":
            out.append((rid, {
                "id": rid, "is_open": _bool(r["is_open"]),
                "open_weekdays": _json(r["open_weekdays"]),
                "open_start_time": _time(r["open_start_time"]),
                "open_end_time": _time(r["open_end_time"]),
                "advance_days": r["advance_days"], "max_duration_minutes": r["max_duration_minutes"],
                "approver_user_id": r["approver_user_id"], "approval_mode": r["approval_mode"],
                "approval_stages": _json(r["approval_stages"]),
                "updated_by": r["updated_by"], "updated_at": pt.app_utc(r["updated_at"]),
            }))
        elif src == "portal_esports_bookings":
            out.append((rid, {
                "id": rid, "booking_date": _time(r["booking_date"]),
                "start_time": _time(r["start_time"]), "end_time": _time(r["end_time"]),
                "title": r["title"], "purpose": r["purpose"],
                "participant_count": r["participant_count"], "participants": _json(r["participants"]),
                "applicant_user_id": r["applicant_user_id"], "applicant_name": r["applicant_name"],
                "approver_user_id": r["approver_user_id"], "approver_name": r["approver_name"],
                "approval_mode": r["approval_mode"], "current_stage_index": r["current_stage_index"],
                "status": r["status"],
                "reviewed_by_user_id": r["reviewed_by_user_id"], "reviewed_by_name": r["reviewed_by_name"],
                "review_remark": r["review_remark"], "reviewed_at": pt.app_utc(r["reviewed_at"]),
                "cancelled_by_user_id": r["cancelled_by_user_id"], "cancelled_at": pt.app_utc(r["cancelled_at"]),
                "created_at": pt.app_utc(r["created_at"]), "updated_at": pt.app_utc(r["updated_at"]),
            }))
        elif src == "portal_esports_approval_steps":
            out.append((rid, {
                "id": rid, "booking_id": r["booking_id"], "stage_index": r["stage_index"],
                "approver_user_id": r["approver_user_id"], "approver_name": r["approver_name"],
                "status": r["status"],
                "acted_by_user_id": r["acted_by_user_id"], "acted_by_name": r["acted_by_name"],
                "acted_at": pt.app_utc(r["acted_at"]), "remark": r["remark"],
            }))
        elif src == "portal_esports_booking_logs":
            out.append((rid, {
                "id": rid, "booking_id": r["booking_id"], "operation": r["operation"],
                "actor_user_id": r["actor_user_id"], "actor_name": r["actor_name"],
                "remark": r["remark"], "created_at": pt.app_utc(r["created_at"]),
            }))
        stats["rows"] += 1
    return out


def _write(cur, table, rows, dry_run, report):
    """rows 是 (id, 列名字典) 的列表；PostgreSQL 里已有的 id 跳过；按 500 行分批写入，控制内存占用。"""
    cur.execute("select id from %s" % table)
    done = {r[0] for r in cur.fetchall()}
    todo = [row for rid, row in rows if rid not in done]
    for start in range(0, len(todo), 500):
        chunk = todo[start:start + 500]
        cols = list(chunk[0].keys())
        sql = "INSERT INTO %s (%s) VALUES (%s)" % (table, ", ".join(cols), ", ".join(["%s"] * len(cols)))
        if not dry_run:
            cur.executemany(sql, [tuple(row[c] for c in cols) for row in chunk])
    report[table] = "%d 写入 / %d 已存在跳过 / PG %d" % (len(todo), len(rows) - len(todo), len(rows))


def migrate(portal, dry_run):
    my = connect_target(readonly=dry_run)
    try:
        cur = my.cursor()
        report = {}
        stats = {"rows": 0}

        for src, dst in TABLES:
            rows = build(src, portal[src], stats)
            _write(cur, dst, rows, dry_run, report)

        report["时间换算（恒 +8h，应用层 utcnow）"] = "是"

        if dry_run:
            my.rollback()
        else:
            advance_target_sequences(my)
            my.commit()
    except Exception:
        my.rollback()  # 半途失败不留半套电竞室数据
        raise
    finally:
        my.close()
    return report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="只统计将要写入的行数，事务回滚")
    args = ap.parse_args()
    portal = asyncio.run(fetch_portal())
    report = migrate(portal, dry_run=args.dry_run)
    print("\n%s 迁移结果" % ("[dry-run 未写库] " if args.dry_run else ""))
    for k, v in report.items():
        print("  %-28s %s" % (k, v))


if __name__ == "__main__":
    sys.exit(main())
