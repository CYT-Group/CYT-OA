"""B4 员工反馈迁移：Portal Postgres（bot.employee_feedback）→ OA PostgreSQL（portal_feedback）。

用法：
    python scripts/migrate_portal_feedback.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_feedback.py             # 真正写入

行 id 原样保留；PostgreSQL 已存在的 id 跳过。口径出处：
docs/Portal管理面迁移契约.md §一「员工反馈」、§二「类型转换口径」、§四 批次 4。
时间换算见 portal_time.py；JSONB（image_urls）文本原样；wecom_userid → username。
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

SRC_TABLE = "bot.employee_feedback"
DST_TABLE = "portal_feedback"


def _jsonb(value):
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    rows = await pg.fetch("select * from %s order by id" % SRC_TABLE)
    await pg.close()
    return rows


def build(rows, stats):
    out = []
    for r in rows:
        rid = r["id"]
        out.append((rid, {
            "id": rid, "username": r["wecom_userid"], "reporter_name": r["reporter_name"],
            "department": r["department"], "category": r["category"],
            "sub_category": r["sub_category"], "brand": r["brand"],
            "product_name": r["product_name"], "sentiment": r["sentiment"],
            "priority": r["priority"], "raw_message": r["raw_message"],
            "image_urls": _jsonb(r["image_urls"]), "ai_summary": r["ai_summary"],
            "status": r["status"], "assigned_to": r["assigned_to"],
            "resolved_note": r["resolved_note"],
            "created_at": clock(r["created_at"], stats),
            "updated_at": clock(r["updated_at"], stats),
        }))
    return out


def clock(value, stats):
    converted = pt.server_clock(value)
    if converted != value:
        stats["utc_era"] += 1
    if pt.is_cutover_day(value):
        stats["cutover_day"] += 1
    return converted


def migrate(rows, dry_run):
    my = connect_target(readonly=dry_run)
    try:
        cur = my.cursor()
        stats = {"utc_era": 0, "cutover_day": 0}
        data = build(rows, stats)

        cur.execute("select id from %s" % DST_TABLE)
        done = {r[0] for r in cur.fetchall()}
        todo = [row for rid, row in data if rid not in done]
        for start in range(0, len(todo), 500):
            chunk = todo[start:start + 500]
            cols = list(chunk[0].keys())
            sql = "INSERT INTO %s (%s) VALUES (%s)" % (DST_TABLE, ", ".join(cols), ", ".join(["%s"] * len(cols)))
            if not dry_run:
                cur.executemany(sql, [tuple(row[c] for c in cols) for row in chunk])

        if dry_run:
            my.rollback()
        else:
            advance_target_sequences(my)
            my.commit()
    finally:
        my.close()

    print("\n%s员工反馈迁移结果" % ("[dry-run 未写库] " if dry_run else ""))
    print("  %-20s %d 写入 / %d 已存在跳过 / PG %d" % (DST_TABLE, len(todo), len(data) - len(todo), len(data)))
    print("  %-20s %d" % ("按 UTC 段 +8h 的时间值", stats["utc_era"]))
    print("  %-20s %d" % ("落在切换当天(需复核)", stats["cutover_day"]))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true", help="只统计将要写入的行数，事务回滚")
    args = ap.parse_args()
    rows = asyncio.run(fetch_portal())
    migrate(rows, dry_run=args.dry_run)


if __name__ == "__main__":
    sys.exit(main())
