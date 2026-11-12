"""B3 展会日历 / 新品动态迁移：Portal Postgres → OA PostgreSQL（批次 3-2）。

用法：
    python scripts/migrate_portal_events_products.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_events_products.py             # 真正写入

行 id 一律原样保留；PostgreSQL 里已存在的 id 直接跳过，中途失败可以重复执行同一命令接着跑。

口径出处：docs/Portal管理面迁移契约.md 批次 3-2。
时间换算见 portal_time.py（Portal 的 PG 服务端时钟在 2026-07-23 从 UTC 切到 Asia/Shanghai）。
"""

import argparse
import asyncio
import os
import sys

import asyncpg
from portal_target import connect_target, advance_target_sequences

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal_time as pt  # noqa: E402
from migrate_portal_identity import read_pg_dsn  # noqa: E402

# PG 表名与 PostgreSQL 表名一致（两张表都没在 Portal 侧改过名）
TABLES = [
    ("portal_events", "portal_events"),
    ("portal_products", "portal_products"),
]


def _flag(value):
    """Portal boolean → OA SMALLINT，保留 Java 既有 0/1 状态约定。"""
    return None if value is None else int(bool(value))


def _date(value):
    """DATE 列：Portal 返回 datetime.date，转成 'YYYY-MM-DD' 字符串交给 PostgreSQL。"""
    return None if value is None else str(value)


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    out = {}
    for src, _ in TABLES:
        out[src] = await pg.fetch("select * from %s order by id" % src)
    await pg.close()
    return out


def _write(cur, table, rows, dry_run, report, label):
    """rows 是 (id, 列名字典) 的列表；PostgreSQL 里已有的 id 跳过，按 500 行一批送。"""
    rows.sort(key=lambda r: r[0])
    cur.execute("select id from %s" % table)
    done = {r[0] for r in cur.fetchall()}
    todo = [row for rid, row in rows if rid not in done]
    for start in range(0, len(todo), 500):
        chunk = todo[start:start + 500]
        cols = list(chunk[0].keys())
        sql = "INSERT INTO %s (%s) VALUES (%s)" % (table, ", ".join(cols), ", ".join(["%s"] * len(cols)))
        if not dry_run:
            cur.executemany(sql, [tuple(row[c] for c in cols) for row in chunk])
    report[label] = "%d 写入 / %d 已存在跳过 / PG %d" % (len(todo), len(rows) - len(todo), len(rows))


def build(src, table_rows):
    """按目标表做列映射与类型换算，返回 (id, 列名字典) 列表和一份体检小计。"""
    stats = {"utc_era": 0, "cutover_day": 0}

    def clock(value):
        converted = pt.server_clock(value)
        if converted != value:
            stats["utc_era"] += 1
        if pt.is_cutover_day(value):
            stats["cutover_day"] += 1
        return converted

    out = []
    for r in table_rows:
        rid = r["id"]
        if src == "portal_events":
            out.append((rid, {
                "id": rid, "title": r["title"], "description": r["description"],
                "location": r["location"], "start_date": _date(r["start_date"]),
                "end_date": _date(r["end_date"]), "event_type": r["event_type"],
                "status": r["status"], "created_at": clock(r["created_at"]),
                "updated_at": clock(r["updated_at"]),
            }))
        elif src == "portal_products":
            out.append((rid, {
                "id": rid, "name": r["name"], "brand": r["brand"],
                "category": r["category"], "description": r["description"],
                "image_url": r["image_url"], "link_url": r["link_url"],
                "launch_date": _date(r["launch_date"]), "status": r["status"],
                "is_featured": _flag(r["is_featured"]),
                "created_at": clock(r["created_at"]), "updated_at": clock(r["updated_at"]),
            }))
    return out, stats


def migrate(portal, dry_run):
    my = connect_target(readonly=dry_run)
    try:
        cur = my.cursor()
        report = {}
        all_stats = {"utc_era": 0, "cutover_day": 0}

        for src, dst in TABLES:
            rows, stats = build(src, portal[src])
            _write(cur, dst, rows, dry_run, report, dst)
            for k in ("utc_era", "cutover_day"):
                all_stats[k] += stats[k]

        report["按 UTC 段 +8h 的时间值"] = all_stats["utc_era"]
        report["落在切换当天(需复核)"] = all_stats["cutover_day"]

        if dry_run:
            my.rollback()
        else:
            advance_target_sequences(my)
            my.commit()
    except Exception:
        my.rollback()  # 半途失败不留半套内容数据
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
