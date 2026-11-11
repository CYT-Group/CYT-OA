"""B3 制度签收迁移：Portal Postgres（portal_handbook_signoff）→ OA MySQL（portal_handbook_signoff）。

用法：
    python scripts/migrate_portal_signoff.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_signoff.py             # 真正写入

行 id 原样保留；MySQL 已存在的 id 跳过。user_id 引用 portal_users.id（批次 1 已迁入 sys_user，id 保留），
document_id 引用 portal_hr_documents.id（批次 3-4 已迁入，id 保留），FK 天然满足。
口径出处：docs/Portal管理面迁移契约.md §一「制度签收」、§二「类型转换口径」、§四 批次 3。
时间换算见 portal_time.py（signed_at 是 func.now() 系列，走 server_clock）。
"""

import argparse
import asyncio
import os
import sys

import asyncpg
import pymysql

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal_time as pt  # noqa: E402
from migrate_portal_identity import read_mysql_conf, read_pg_dsn  # noqa: E402

TABLE = "portal_handbook_signoff"


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    rows = await pg.fetch("select * from %s order by id" % TABLE)
    await pg.close()
    return rows


def build(rows, stats):
    out = []
    for r in rows:
        rid = r["id"]
        out.append((rid, {
            "id": rid, "user_id": r["user_id"], "username": r["username"],
            "document_id": r["document_id"], "document": r["document"],
            "version": r["version"],
            "signed_at": clock(r["signed_at"], stats),
            "ip": r["ip"], "user_agent": r["user_agent"],
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
    my = pymysql.connect(**read_mysql_conf())
    cur = my.cursor()
    stats = {"utc_era": 0, "cutover_day": 0}
    data = build(rows, stats)

    cur.execute("select id from %s" % TABLE)
    done = {r[0] for r in cur.fetchall()}
    todo = [row for rid, row in data if rid not in done]
    for start in range(0, len(todo), 500):
        chunk = todo[start:start + 500]
        cols = list(chunk[0].keys())
        sql = "INSERT INTO %s (%s) VALUES (%s)" % (TABLE, ", ".join(cols), ", ".join(["%s"] * len(cols)))
        if not dry_run:
            cur.executemany(sql, [tuple(row[c] for c in cols) for row in chunk])

    try:
        if dry_run:
            my.rollback()
        else:
            my.commit()
    finally:
        my.close()

    print("\n%s制度签收迁移结果" % ("[dry-run 未写库] " if dry_run else ""))
    print("  %-20s %d 写入 / %d 已存在跳过 / PG %d" % (TABLE, len(todo), len(data) - len(todo), len(data)))
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
