"""B2 内容域迁移：Portal Postgres → OA PostgreSQL（分类树 / 公司动态 / 知识库 / 页面埋点）。

用法：
    python scripts/migrate_portal_content.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_content.py             # 真正写入

行 id 一律原样保留：点赞和评论是按 news_id 引用动态的，重新编号等于把关系打断。
PostgreSQL 里已存在的 id 直接跳过，所以中途失败可以重复执行同一命令接着跑。

口径出处：docs/Portal管理面迁移契约.md §一「内容管理」、§二「类型转换口径」、§四 批次 2。
时间换算见 portal_time.py（Portal 的 PG 服务端时钟在 2026-07-23 从 UTC 切到 Asia/Shanghai）。
"""

import argparse
import asyncio
import json
import os
import sys
from datetime import timedelta

import asyncpg
from portal_target import connect_target, advance_target_sequences

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal_time as pt  # noqa: E402
from migrate_portal_identity import read_pg_dsn  # noqa: E402

# PG 表名 → PostgreSQL 表名：点赞/评论在 Portal 侧是复数，OA 建表按单数收口
TABLES = [
    ("portal_categories", "portal_categories"),
    ("portal_news", "portal_news"),
    ("portal_news_likes", "portal_news_like"),
    ("portal_news_comments", "portal_news_comment"),
    ("portal_wiki", "portal_wiki"),
    ("portal_page_views", "portal_page_views"),
]


def _json(value):
    """ARRAY(String) → JSON 数组，jsonb 文本原样交给 PostgreSQL 的 JSON 列校验。

    数组元素统一转字符串：Portal 侧 tags / pending_notify_dept_scope 声明的就是 ARRAY(String)，
    存进 JSON 后 Java 端按 List<String> 读，不能被 PG 里的 "423" 变成数字 423 带跑。
    """
    if value is None:
        return None
    if isinstance(value, (list, tuple)):
        return json.dumps([None if x is None else str(x) for x in value], ensure_ascii=False)
    return value


def _flag(value):
    """Portal boolean → OA SMALLINT，保持 0/1 状态存储约定。"""
    return None if value is None else int(bool(value))


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    out = {}
    for src, _ in TABLES:
        out[src] = await pg.fetch("select * from %s order by id" % src)
    await pg.close()
    return out


def _write(cur, table, rows, dry_run, report, label):
    """rows 是 (id, 列名字典) 的列表；PostgreSQL 里已有的 id 跳过，父行先插（self-FK 不吃未来 id）。"""
    # parent_id 为空的排前面：只有 categories / comments 用得上，其余表 key 恒为 False
    rows.sort(key=lambda r: (r[1].get("parent_id") is not None, r[0]))
    cur.execute("select id from %s" % table)
    done = {r[0] for r in cur.fetchall()}
    todo = [row for rid, row in rows if rid not in done]
    # 含长 user_agent 的记录按 500 行分批写入，限制每批内存占用。
    for start in range(0, len(todo), 500):
        chunk = todo[start:start + 500]
        cols = list(chunk[0].keys())
        sql = "INSERT INTO %s (%s) VALUES (%s)" % (table, ", ".join(cols), ", ".join(["%s"] * len(cols)))
        if not dry_run:
            cur.executemany(sql, [tuple(row[c] for c in cols) for row in chunk])
    report[label] = "%d 写入 / %d 已存在跳过 / PG %d" % (len(todo), len(rows) - len(todo), len(rows))


def build(src, table_rows):
    """按目标表做列映射与类型换算，返回 (id, 列名字典) 列表和一份体检小计。"""
    stats = {"utc_era": 0, "cutover_day": 0, "seeded_news": [], "published_before_created": []}

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
        if src == "portal_categories":
            out.append((rid, {
                "id": rid, "scope": r["scope"], "name": r["name"], "code": r["code"],
                "parent_id": r["parent_id"], "depth": r["depth"], "sort_order": r["sort_order"],
                "attrs": _json(r["attrs"]) or "{}",
                "archived_at": pt.app_utc(r["archived_at"]), "archived_by": r["archived_by"],
                "created_by": r["created_by"], "created_at": clock(r["created_at"]),
                "updated_at": clock(r["updated_at"]),
            }))
        elif src == "portal_news":
            # 人工种入的行：published_at 与 created_at 逐微秒相等且是整点零微秒，
            # 那是人手写的北京时间（展会战报 18:00 发），+8h 会把发布日推到第二天，原样搬。
            # updated_at 带微秒，是后来走 ORM 改稿时 func.now() 落的服务端时间，照常规口径换算。
            seeded = (r["published_at"] == r["created_at"]
                      and r["created_at"].second == 0 and r["created_at"].microsecond == 0)
            published_at = r["published_at"] if seeded else pt.app_utc(r["published_at"])
            created_at = r["created_at"] if seeded else clock(r["created_at"])
            if seeded:
                stats["seeded_news"].append(rid)
            elif r["published_at"] is not None and published_at < created_at - timedelta(seconds=1):
                # 同一事务里 published_at 先写、created_at 后写，差十几毫秒是正常噪声，按 1 秒判
                stats["published_before_created"].append(rid)
            out.append((rid, {
                "id": rid, "title": r["title"], "summary": r["summary"], "content": r["content"],
                "category": r["category"], "cover_image": r["cover_image"],
                "template_type": r["template_type"], "blocks": _json(r["blocks"]),
                "author": r["author"], "author_username": r["author_username"],
                "status": r["status"], "is_featured": _flag(r["is_featured"]),
                "published_at": published_at,
                "pending_notify_wecom": _flag(r["pending_notify_wecom"]),
                "pending_notify_dept_scope": _json(r["pending_notify_dept_scope"]),
                "created_at": created_at, "updated_at": clock(r["updated_at"]),
            }))
        elif src == "portal_news_likes":
            out.append((rid, {
                "id": rid, "news_id": r["news_id"], "username": r["username"],
                "display_name": r["display_name"], "created_at": clock(r["created_at"]),
            }))
        elif src == "portal_news_comments":
            out.append((rid, {
                "id": rid, "news_id": r["news_id"], "username": r["username"],
                "display_name": r["display_name"], "avatar_url": r["avatar_url"],
                "content": r["content"], "parent_id": r["parent_id"],
                "is_deleted": _flag(r["is_deleted"]), "created_at": clock(r["created_at"]),
                "updated_at": clock(r["updated_at"]),
            }))
        elif src == "portal_wiki":
            out.append((rid, {
                "id": rid, "title": r["title"], "content": r["content"], "category": r["category"],
                "department": r["department"], "doc_url": r["doc_url"], "tags": _json(r["tags"]),
                "author_username": r["author_username"], "status": r["status"],
                "view_count": r["view_count"], "created_at": clock(r["created_at"]),
                "updated_at": clock(r["updated_at"]),
            }))
        elif src == "portal_page_views":
            out.append((rid, {
                "id": rid, "page_url": r["page_url"], "page_title": r["page_title"],
                "username": r["username"], "display_name": r["display_name"],
                "department": r["department"], "referrer": r["referrer"],
                "user_agent": r["user_agent"], "viewed_at": clock(r["viewed_at"]),
            }))
    return out, stats


def migrate(portal, dry_run):
    my = connect_target(readonly=dry_run)
    try:
        cur = my.cursor()
        report = {}
        all_stats = {"utc_era": 0, "cutover_day": 0, "seeded_news": [], "published_before_created": []}

        for src, dst in TABLES:
            rows, stats = build(src, portal[src])
            _write(cur, dst, rows, dry_run, report, dst)
            for k in ("utc_era", "cutover_day"):
                all_stats[k] += stats[k]
            all_stats["seeded_news"] += stats["seeded_news"]
            all_stats["published_before_created"] += stats["published_before_created"]

        report["按 UTC 段 +8h 的时间值"] = all_stats["utc_era"]
        report["落在切换当天(需复核)"] = all_stats["cutover_day"]
        report["人工种入未换算的动态"] = ",".join(str(i) for i in all_stats["seeded_news"]) or "无"
        report["published_at 早于 created_at"] = ",".join(str(i) for i in all_stats["published_before_created"]) or "无"

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
