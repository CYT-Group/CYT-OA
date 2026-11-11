"""B3 培训中心迁移：Portal Postgres → OA MySQL（课程 / 课时 / 课时进度 / 课程级进度）。

用法：
    python scripts/migrate_portal_training.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_training.py             # 真正写入

行 id 一律原样保留：课时按 course_id 引用课程、课时进度按 lesson_id 引用课时，重新编号等于把关系打断。
MySQL 里已存在的 id 直接跳过，中途失败可重复执行接着跑。category_id 引用的 portal_categories
已在批次 2 迁入（id 保留），FK 天然满足。

口径出处：docs/Portal管理面迁移契约.md §一「培训中心」、§二「类型转换口径」、§四 批次 3。
时间换算见 portal_time.py；ARRAY(String)→JSON（dept_scope 元素字符串化）、JSONB 文本原样（pages）。
"""

import argparse
import asyncio
import json
import os
import sys

import asyncpg
import pymysql

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal_time as pt  # noqa: E402
from migrate_portal_identity import read_mysql_conf, read_pg_dsn  # noqa: E402

# 插入顺序即外键依赖顺序：先课程、再课时、再课时进度、再课程级进度（课程级进度无 FK，排最后无妨）
TABLES = [
    ("portal_training", "portal_training"),
    ("portal_training_lessons", "portal_training_lessons"),
    ("portal_training_lesson_progress", "portal_training_lesson_progress"),
    ("portal_training_progress", "portal_training_progress"),
]


def _json_array(value):
    """ARRAY(String) → JSON 数组字符串化（dept_scope）。

    元素统一转字符串：Java 端按 List<String> 读，不能让 PG 里的数字/空值带跑类型。
    """
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps([None if x is None else str(x) for x in value], ensure_ascii=False)


def _pages(value):
    """JSONB 文本原样（pages）：asyncpg 读回的是已解析的 list，重新 dumps 成 JSON 文本交给 MySQL JSON 列校验。"""
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    out = {}
    for src, _ in TABLES:
        out[src] = await pg.fetch("select * from %s order by id" % src)
    await pg.close()
    return out


def build(src, table_rows, stats):
    """按目标表做列映射与时间换算，返回 (id, 列名字典) 列表。

    created_at / updated_at / started_at 走 server_clock（func.now() 系列，按写入时刻分段）；
    completed_at 是应用层 datetime.utcnow() 写的，走 app_utc（恒 +8h）。
    """
    out = []
    for r in table_rows:
        rid = r["id"]
        if src == "portal_training":
            out.append((rid, {
                "id": rid, "title": r["title"], "description": r["description"],
                "department": r["department"], "level": r["level"],
                "duration_minutes": r["duration_minutes"], "instructor": r["instructor"],
                "doc_url": r["doc_url"], "video_url": r["video_url"],
                "cover_image": r["cover_image"], "status": r["status"],
                "sort_order": r["sort_order"], "category": r["category"],
                "category_id": r["category_id"], "source_prefix": r["source_prefix"],
                "dept_scope": _json_array(r["dept_scope"]),
                "author_username": r["author_username"],
                "created_at": clock(r["created_at"], stats),
                "updated_at": clock(r["updated_at"], stats),
            }))
        elif src == "portal_training_lessons":
            out.append((rid, {
                "id": rid, "course_id": r["course_id"], "sort_order": r["sort_order"],
                "title": r["title"], "kind": r["kind"], "object_key": r["object_key"],
                "pdf_key": r["pdf_key"], "pages": _pages(r["pages"]), "url": r["url"],
                "duration_sec": r["duration_sec"], "size_bytes": r["size_bytes"],
                "source_prefix": r["source_prefix"],
                "created_at": clock(r["created_at"], stats),
                "updated_at": clock(r["updated_at"], stats),
            }))
        elif src == "portal_training_lesson_progress":
            out.append((rid, {
                "id": rid, "username": r["username"], "lesson_id": r["lesson_id"],
                "status": r["status"], "position": r["position"], "percent": r["percent"],
                "duration_sec": r["duration_sec"],
                "updated_at": clock(r["updated_at"], stats),
                "completed_at": pt.app_utc(r["completed_at"]),
            }))
        elif src == "portal_training_progress":
            out.append((rid, {
                "id": rid, "username": r["username"], "training_id": r["training_id"],
                "status": r["status"],
                "started_at": clock(r["started_at"], stats),
                "completed_at": pt.app_utc(r["completed_at"]),
            }))
    return out


def clock(value, stats):
    converted = pt.server_clock(value)
    if converted != value:
        stats["utc_era"] += 1
    if pt.is_cutover_day(value):
        stats["cutover_day"] += 1
    return converted


def _write(cur, table, rows, dry_run, report, label):
    """rows 是 (id, 列名字典) 的列表；MySQL 里已有的 id 跳过；500 行一批送（防 max_allowed_packet 顶爆）。"""
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


def migrate(portal, dry_run):
    my = pymysql.connect(**read_mysql_conf())
    cur = my.cursor()
    report = {}
    stats = {"utc_era": 0, "cutover_day": 0}

    for src, dst in TABLES:
        rows = build(src, portal[src], stats)
        _write(cur, dst, rows, dry_run, report, dst)

    report["按 UTC 段 +8h 的时间值"] = stats["utc_era"]
    report["落在切换当天(需复核)"] = stats["cutover_day"]
    report["completed_at 恒 +8h(应用层 utcnow)"] = "是"

    try:
        if dry_run:
            my.rollback()
        else:
            my.commit()
    except Exception:
        my.rollback()  # 半途失败不留半套培训数据
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
