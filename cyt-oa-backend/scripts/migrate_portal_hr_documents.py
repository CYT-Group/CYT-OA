"""B3 HR 文档 CMS 迁移：Portal Postgres → OA MySQL（主表 + 别名 + 入口绑定 + 发布版本 + 草稿版本）。

用法：
    python scripts/migrate_portal_hr_documents.py --dry-run   # 只报数，不写库
    python scripts/migrate_portal_hr_documents.py             # 真正写入

行 id 一律原样保留：别名 / 入口绑定 / 版本快照都按 document_id 引用主表，重新编号等于把
关系打断。MySQL 里已存在的 id 直接跳过，中途失败可重复执行接着跑。本模块 5 张表无外键依赖
其它批次，主表无前置。

口径出处：docs/Portal管理面迁移契约.md §一「HR 文档 CMS」、§二「类型转换口径」、§四 批次 3。
时间换算见 portal_time.py；ARRAY(String)→JSON（dept_scope/tags 元素字符串化）、JSONB 文本原样
（blocks/cover/signoff）。

时间口径（与 models.py 对齐）：
  - func.now() 系列（created_at/updated_at/versions.published_at/draft_versions.created_at）
    走 server_clock（PG 时钟 2026-07-23 从 UTC 切 Asia/Shanghai，按写入时刻分段）；
  - datetime.utcnow() 系列（主表 published_at/submitted_at/offline_at/deleted_at）
    走 app_utc（恒 +8h）。
"""

import argparse
import asyncio
import json
import os
import sys
from datetime import datetime, timezone

import asyncpg
import pymysql

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portal_time as pt  # noqa: E402
from migrate_portal_identity import read_mysql_conf, read_pg_dsn  # noqa: E402

# 插入顺序即外键依赖顺序：先主表，再别名 / 入口绑定 / 发布版本 / 草稿版本（后四者 FK 引用主表）
TABLES = [
    ("portal_hr_documents", "portal_hr_documents"),
    ("portal_hr_document_aliases", "portal_hr_document_aliases"),
    ("portal_hr_document_entry_bindings", "portal_hr_document_entry_bindings"),
    ("portal_hr_document_versions", "portal_hr_document_versions"),
    ("portal_hr_document_draft_versions", "portal_hr_document_draft_versions"),
]


def _json_array(value):
    """ARRAY(String) → JSON 数组字符串化（dept_scope / tags）。"""
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps([None if x is None else str(x) for x in value], ensure_ascii=False)


def _jsonb(value):
    """JSONB 文本原样（blocks / cover / signoff）：asyncpg 读回的是已解析对象，重新 dumps。"""
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False)


def _bool(value):
    """PG boolean → tinyint(1)。"""
    if value is None:
        return None
    return 1 if value else 0


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    out = {}
    for src, _ in TABLES:
        out[src] = await pg.fetch("select * from %s order by id" % src)
    await pg.close()
    return out


def build(src, table_rows, stats):
    """按目标表做列映射与时间换算，返回 (id, 列名字典) 列表。"""
    out = []
    for r in table_rows:
        rid = r["id"]
        if src == "portal_hr_documents":
            out.append((rid, {
                "id": rid, "slug": r["slug"], "title": r["title"],
                "category": r["category"], "topic": r["topic"],
                "content_html": r["content_html"], "draft_content": r["draft_content"],
                "content_blocks": _jsonb(r["content_blocks"]),
                "draft_blocks": _jsonb(r["draft_blocks"]),
                "version": r["version"], "draft_version": r["draft_version"],
                "status": r["status"], "requires_signoff": _bool(r["requires_signoff"]),
                "dept_scope": _json_array(r["dept_scope"]),
                "draft_title": r["draft_title"],
                "draft_requires_signoff": _bool(r["draft_requires_signoff"]),
                "draft_dept_scope": _json_array(r["draft_dept_scope"]),
                "intro": r["intro"], "draft_intro": r["draft_intro"],
                "cover_image": r["cover_image"], "draft_cover_image": r["draft_cover_image"],
                "theme": r["theme"], "draft_theme": r["draft_theme"],
                "tags": _json_array(r["tags"]), "draft_tags": _json_array(r["draft_tags"]),
                "toc_enabled": _bool(r["toc_enabled"]),
                "draft_toc_enabled": _bool(r["draft_toc_enabled"]),
                "toc_position": r["toc_position"], "draft_toc_position": r["draft_toc_position"],
                "cover": _jsonb(r["cover"]), "draft_cover": _jsonb(r["draft_cover"]),
                "download_pdf_enabled": _bool(r["download_pdf_enabled"]),
                "draft_download_pdf_enabled": _bool(r["draft_download_pdf_enabled"]),
                "signoff": _jsonb(r["signoff"]), "draft_signoff": _jsonb(r["draft_signoff"]),
                "updated_by": r["updated_by"],
                "updated_at": clock(r["updated_at"], stats),
                "submitted_by": r["submitted_by"],
                "submitted_at": pt.app_utc(r["submitted_at"]),
                "published_by": r["published_by"],
                "published_at": pt.app_utc(r["published_at"]),
                "offline_at": pt.app_utc(r["offline_at"]),
                "offline_by": r["offline_by"], "offline_reason": r["offline_reason"],
                "deleted_at": pt.app_utc(r["deleted_at"]), "deleted_by": r["deleted_by"],
                "created_at": clock(r["created_at"], stats),
            }))
        elif src == "portal_hr_document_aliases":
            out.append((rid, {
                "id": rid, "alias": r["alias"], "alias_type": r["alias_type"],
                "document_id": r["document_id"], "created_by": r["created_by"],
                "created_at": clock(r["created_at"], stats),
                "updated_at": clock(r["updated_at"], stats),
            }))
        elif src == "portal_hr_document_entry_bindings":
            out.append((rid, {
                "id": rid, "entry_key": r["entry_key"], "document_id": r["document_id"],
                "created_by": r["created_by"], "updated_by": r["updated_by"],
                "created_at": clock(r["created_at"], stats),
                "updated_at": clock(r["updated_at"], stats),
            }))
        elif src == "portal_hr_document_versions":
            out.append((rid, {
                "id": rid, "document_id": r["document_id"], "slug": r["slug"],
                "version": r["version"], "title": r["title"],
                "content_html": r["content_html"],
                "content_blocks": _jsonb(r["content_blocks"]),
                "published_at": clock(r["published_at"], stats),
                "published_by": r["published_by"], "approved_by": r["approved_by"],
                "submitted_by": r["submitted_by"],
            }))
        elif src == "portal_hr_document_draft_versions":
            out.append((rid, {
                "id": rid, "document_id": r["document_id"], "slug": r["slug"],
                "title": r["title"], "content_html": r["content_html"],
                "content_blocks": _jsonb(r["content_blocks"]),
                "saved_by": r["saved_by"],
                "created_at": clock(r["created_at"], stats),
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
    """rows 是 (id, 列名字典) 的列表；MySQL 里已有的 id 跳过；500 行一批送（防 max_allowed_packet）。"""
    cur.execute("select id from %s" % table)
    done = {r[0] for r in cur.fetchall()}
    todo = [row for rid, row in rows if rid not in done]
    for start in range(0, len(todo), 500):
        chunk = todo[start:start + 500]
        cols = list(chunk[0].keys())
        sql = "INSERT INTO %s (%s) VALUES (%s)" % (table, ", ".join(cols), ", ".join(["%s"] * len(cols)))
        if not dry_run:
            cur.executemany(sql, [tuple(row[c] for c in cols) for row in chunk])
    report[label] = "%d 写入 / %d 已存在跳过 / 准备记录 %d" % (len(todo), len(rows) - len(todo), len(rows))


def preserve_orphan_snapshots(portal):
    """旧 PG 存在已物理删除主文档的历史快照；重建回收站索引以保留证据和原 ID。

    这不是还原已发布文档：索引始终 draft + deleted，删除时间明确是本次迁移归档时间。
    原版本/草稿全文逐字保留，旧 slug 只在快照中保留，不抢占当前文档 URL。
    """
    documents = [dict(row) for row in portal["portal_hr_documents"]]
    known = {row["id"] for row in documents}
    referenced = {}
    for table, _ in TABLES[1:]:
        for row in portal[table]:
            if row["document_id"] not in known:
                previous = referenced.get(row["document_id"])
                if previous is None or (row.get("title") and row["id"] > previous["id"]):
                    referenced[row["document_id"]] = dict(row)
    now = datetime.now(timezone(pt.UTC_OFFSET)).replace(tzinfo=None)
    template = {key: None for key in documents[0]} if documents else {}
    for document_id, snapshot in sorted(referenced.items()):
        row = dict(template)
        row.update(id=document_id, slug=None, title=snapshot.get("title") or f"历史文档 {document_id}",
                   category="hr", topic=None, content_html=snapshot.get("content_html"),
                   content_blocks=snapshot.get("content_blocks"), draft_content=snapshot.get("content_html"),
                   draft_blocks=snapshot.get("content_blocks"), version=snapshot.get("version") or "v1.0",
                   status="draft", requires_signoff=False, toc_enabled=True, toc_position="left",
                   download_pdf_enabled=False, updated_by="portal-migration", deleted_by="portal-migration",
                   deleted_at=now - pt.UTC_OFFSET, created_at=snapshot.get("created_at") or snapshot.get("published_at") or now,
                   intro="源文档已缺失；本条为迁移时建立的历史快照回收站索引，归档时间不是原文档删除时间。")
        documents.append(row)
    portal = dict(portal)
    portal["portal_hr_documents"] = documents
    return portal, sorted(referenced)


def migrate(portal, dry_run):
    source_document_count = len(portal["portal_hr_documents"])
    portal, orphan_ids = preserve_orphan_snapshots(portal)
    my = pymysql.connect(**read_mysql_conf())
    cur = my.cursor()
    report = {}
    stats = {"utc_era": 0, "cutover_day": 0}

    try:
        for src, dst in TABLES:
            rows = build(src, portal[src], stats)
            _write(cur, dst, rows, dry_run, report, dst)
        report["历史快照孤立主键（回收站索引）"] = orphan_ids
        report["源HR主文档数（不含历史索引）"] = source_document_count
        report["按 UTC 段 +8h 的时间值"] = stats["utc_era"]
        report["落在切换当天(需复核)"] = stats["cutover_day"]
        report["published/submitted/offline/deleted_at 恒 +8h(utcnow)"] = "是"
        if dry_run:
            my.rollback()
        else:
            my.commit()
    except Exception:
        my.rollback()  # 半途失败不留半套 HR 文档数据
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
    print("\n%s迁移结果" % ("[dry-run 未写库] " if args.dry_run else ""))
    for k, v in report.items():
        print("  %-34s %s" % (k, v))


if __name__ == "__main__":
    sys.exit(main())
