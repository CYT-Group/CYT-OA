"""门户管理余项：品牌手册 / 合同主体 / 庆祝设置与执行记录 / 简历 → OA。

默认只搬数据库；--dry-run 不写数据库或附件。附件可用 --copy-assets 搬入本机私有目录，
--document-root 对应 OA 的 PORTAL_DOCUMENT_ROOT。本地原件从 --portal-upload-root 读取；
私有 GCS 简历需显式指定 --gcs-private-bucket，使用当前 ADC 只读下载。
简历面试流程与轮次保留原 ID 和状态；本脚本不删除旧 PG 数据。
"""
import argparse
import asyncio
import hashlib
import json
import os
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import urlopen

import asyncpg
import pymysql

from migrate_portal_identity import read_mysql_conf, read_pg_dsn
import portal_time as pt

TABLES = {
    "portal_brand_docs": ["id", "brand_code", "doc_type", "title", "file_url", "file_size", "status", "updated_by", "created_at", "updated_at"],
    "portal_brand_doc_versions": ["id", "doc_id", "file_url", "file_size", "uploaded_by", "created_at"],
    "portal_contract_entities": ["id", "name", "address", "legal_rep", "is_active", "created_by", "created_at", "updated_at"],
    "portal_celebration_settings": ["id", "enabled", "send_hour", "send_minute", "birthday_enabled", "birthday_card_enabled", "templates", "updated_by", "updated_at"],
    "portal_celebration_runs": ["id", "run_date", "kind", "status", "detail", "created_at"],
    "portal_resumes": ["id", "status", "source_filename", "file_ext", "file_size", "file_sha256", "storage_key", "name", "gender", "birth_date", "phone", "email", "current_city", "highest_degree", "years_of_experience", "current_company", "current_title", "expected_position", "expected_salary", "summary", "extracted", "ai_raw", "ai_model", "ai_tokens_in", "ai_tokens_out", "ai_request_id", "error_msg", "duplicate_of_id", "uploaded_by", "confirmed_by", "confirmed_at", "created_at", "updated_at"],
    "portal_resume_interview_processes": ["id", "resume_id", "position", "status", "result", "summary", "created_by", "updated_by", "created_at", "updated_at", "finished_at"],
    "portal_resume_interview_rounds": ["id", "process_id", "sequence", "name", "interviewer", "scheduled_at", "location", "status", "result", "feedback", "updated_by", "created_at", "updated_at"],
    "portal_celebration_cards": ["id", "token", "kind", "recipient_username", "recipient_name", "title", "message", "years", "created_by", "created_at", "sent_ok", "error_msg", "sent_at"],
    "portal_wecom_push_log": ["id", "trigger", "target_key", "content", "recipient_count", "failed_count", "pushed_by", "pushed_at", "error_msg"],
}
JSON_COLUMNS = {"templates", "extracted", "ai_raw"}
DATE_COLUMNS = {"run_date", "birth_date"}
TIME_COLUMNS = {"created_at", "updated_at", "confirmed_at", "finished_at", "scheduled_at", "sent_at", "pushed_at"}
BOOL_COLUMNS = {"is_active", "enabled", "birthday_enabled", "birthday_card_enabled", "sent_ok"}
CELEBRATION_TRIGGERS = {"celebration_birthday": "birthday", "celebration_anniversary": "anniversary", "celebration_hr_list": "daily_list", "celebration_monthly": "monthly_digest", "birthday_card": "birthday_card", "anniversary_card": "anniversary_card", "celebration_html": "celebration_html"}


def convert(row, columns, table=None):
    out = {}
    for key in columns:
        value = row.get(key)
        if value is not None:
            if key in JSON_COLUMNS and not isinstance(value, str):
                value = json.dumps(value, ensure_ascii=False)
            elif key in BOOL_COLUMNS:
                value = int(bool(value))
            elif key in DATE_COLUMNS:
                value = value.isoformat()
            elif key in TIME_COLUMNS:
                # 来源逐列核对旧 application/resumes.py、resume_interviews.py、celebrations.py：
                # 简历确认和面试全部时间由 utcnow 写；其余创建/更新时间由 PG func.now() 写。
                app_time = table in {"portal_resume_interview_processes", "portal_resume_interview_rounds", "portal_celebration_cards"} or key == "confirmed_at"
                if key == "pushed_at":
                    app_time = row.get("trigger") in CELEBRATION_TRIGGERS
                # push_service.py 用1970占位表示全失败可立即重试，不把它当真实发生时间换算。
                if key != "pushed_at" or value.year != 1970:
                    value = pt.app_utc(value) if app_time else pt.server_clock(value)
        out[key] = value
    return out


async def read_source():
    pg = await asyncpg.connect(read_pg_dsn())
    try:
        tables = {}
        for table in TABLES:
            tables[table] = [dict(r) for r in await pg.fetch("SELECT * FROM " + table + " ORDER BY id")]
        tables["preferences"] = [dict(r) for r in await pg.fetch("SELECT username,celebration_opt_out FROM portal_users WHERE celebration_opt_out=true")]
        return tables
    finally:
        await pg.close()


def safe_copy(source, target, dry_run):
    if not source.is_file():
        return False
    if not dry_run:
        target.parent.mkdir(parents=True, exist_ok=True)
        # 写入临时文件再替换；失败时不留下半份私有原件。
        temp = target.with_suffix(target.suffix + ".partial")
        with source.open("rb") as stream, temp.open("wb") as out:
            import shutil
            shutil.copyfileobj(stream, out)
        temp.replace(target)
    return True


def gcs_copy(bucket, object_key, target, dry_run):
    from google.cloud import storage
    blob = storage.Client().bucket(bucket).blob(object_key)
    if not blob.exists():
        return False
    if not dry_run:
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_suffix(target.suffix + ".partial")
        blob.download_to_filename(str(temporary))
        temporary.replace(target)
    return True


def public_copy(url, target, dry_run):
    """公开 GCS 品牌手册可匿名读取；绝不用于私有简历或任意外部 URL。"""
    parsed = urlparse(url)
    if parsed.scheme != "https" or parsed.hostname != "storage.googleapis.com":
        return False
    try:
        with urlopen(url, timeout=15) as response:
            if urlparse(response.url).hostname != "storage.googleapis.com":
                return False
            content = response.read(20 * 1024 * 1024 + 1)
            if len(content) > 20 * 1024 * 1024:
                return False
            if not dry_run:
                target.parent.mkdir(parents=True, exist_ok=True)
                temporary = target.with_suffix(target.suffix + ".partial")
                temporary.write_bytes(content)
                temporary.replace(target)
        return True
    except (OSError, ValueError):
        return False


def copy_assets(tables, args):
    root = Path(args.document_root).resolve()
    old_root = Path(args.portal_upload_root).resolve()
    report = {"copied_or_existing": 0, "missing": 0}
    for table in ("portal_brand_docs", "portal_brand_doc_versions"):
        for row in tables[table]:
            url = row["file_url"]
            parsed = urlparse(url)
            path = parsed.path
            marker = "brand-docs/"
            if marker not in path:
                report["missing"] += 1
                continue
            original_key = marker + path.split(marker, 1)[1]
            relative = Path(original_key)
            source = (old_root / relative).resolve()
            if not source.is_relative_to(old_root):
                raise ValueError("历史品牌附件路径越界")
            key = "brand-docs/" + hashlib.sha256(url.encode()).hexdigest()[:32] + ".html"
            target = root / key
            copied = target.is_file() or safe_copy(source, target, args.dry_run)
            if not copied:
                copied = public_copy(url, target, args.dry_run)
            if not copied and args.gcs_public_bucket and parsed.hostname == "storage.googleapis.com":
                copied = gcs_copy(args.gcs_public_bucket, original_key, target, args.dry_run)
            if copied:
                row["_original_file_url"] = url
                row["file_url"] = key
                report["copied_or_existing"] += 1
            else:
                report["missing"] += 1
    for row in tables["portal_resumes"]:
        key = row.get("storage_key")
        if not key:
            continue
        relative = Path(key)
        source = (old_root / relative).resolve()
        target = (root / relative).resolve()
        if not source.is_relative_to(old_root) or not target.is_relative_to(root):
            raise ValueError("历史简历附件路径越界")
        copied = target.is_file() or safe_copy(source, target, args.dry_run)
        if not copied and args.gcs_private_bucket:
            copied = gcs_copy(args.gcs_private_bucket, key, target, args.dry_run)
        report["copied_or_existing" if copied else "missing"] += 1
    return report


def migrate(tables, args):
    report = {}
    if args.copy_assets:
        report["附件"] = copy_assets(tables, args)
        if report["附件"]["missing"] and args.require_all_assets:
            raise RuntimeError("存在缺失附件，未写数据库；请核对附件根目录或 GCS 桶")
    mysql = pymysql.connect(**read_mysql_conf())
    try:
        with mysql.cursor() as cur:
            for table, columns in TABLES.items():
                cur.execute("SELECT id FROM " + table)
                done = {r[0] for r in cur.fetchall()}
                rows = [convert(r, columns, table) for r in tables[table] if r["id"] not in done]
                # schema 中的单行默认设置不算已迁移业务行，只覆盖从未由 OA 修改的种子。
                seed_override = 0
                if table == "portal_celebration_settings" and 1 in done and tables[table]:
                    cur.execute("SELECT updated_by FROM portal_celebration_settings WHERE id=1")
                    if cur.fetchone()[0] is None:
                        source = next((r for r in tables[table] if r["id"] == 1), None)
                        if source:
                            seed_override = 1
                            values = convert(source, columns, table)
                            values["updated_by"] = values["updated_by"] or "portal-migration"
                            mutable = [c for c in columns if c != "id"]
                            if not args.dry_run:
                                cur.execute("UPDATE portal_celebration_settings SET " + ",".join(c + "=%s" for c in mutable) + " WHERE id=1 AND updated_by IS NULL", [values[c] for c in mutable])
                if rows and not args.dry_run:
                    sql = "INSERT INTO " + table + " (" + ",".join("`" + c + "`" for c in columns) + ") VALUES (" + ",".join(["%s"] * len(columns)) + ")"
                    cur.executemany(sql, [[r[c] for c in columns] for r in rows])
                # 附件补迁时同步改指针；只修改匹配原始 URL 的行，避免覆盖 OA 后续替换。
                if args.copy_assets and table in ("portal_brand_docs", "portal_brand_doc_versions"):
                    for r in tables[table]:
                        if r["id"] in done and r["file_url"].startswith("brand-docs/") and not args.dry_run:
                            cur.execute("UPDATE " + table + " SET file_url=%s WHERE id=%s AND file_url=%s", (r["file_url"], r["id"], r.get("_original_file_url")))
                report[table] = {"source": len(tables[table]), "insert": len(rows), "skip": len(tables[table]) - len(rows)}
                if seed_override:
                    report[table]["seed_override"] = seed_override
            cur.execute("SELECT username,id FROM sys_user")
            users = dict(cur.fetchall())
            missing = [r["username"] for r in tables["preferences"] if r["username"] not in users]
            if missing:
                raise RuntimeError("庆祝偏好有未迁移身份，请先执行身份迁移（人数 %d）" % len(missing))
            if not args.dry_run:
                cur.executemany("INSERT IGNORE INTO portal_celebration_preferences(user_id,opt_out) VALUES(%s,1)", [(users[r["username"]],) for r in tables["preferences"]])
            report["不参与庆祝偏好"] = len(tables["preferences"])
            cur.execute("SELECT source_log_id FROM portal_celebration_push_log WHERE source_log_id IS NOT NULL")
            logged = {r[0] for r in cur.fetchall()}
            old_logs = [r for r in tables["portal_wecom_push_log"] if r["trigger"] in CELEBRATION_TRIGGERS and r["id"] not in logged]
            if old_logs and not args.dry_run:
                cur.executemany("INSERT INTO portal_celebration_push_log(source_log_id,user_id,kind,title,sent_ok,error_msg,created_at) VALUES(%s,%s,%s,%s,%s,%s,%s)", [
                    (r["id"], users.get(r["target_key"]), CELEBRATION_TRIGGERS[r["trigger"]], r["content"][:100], int(r["recipient_count"] > 0 and r["failed_count"] == 0), (r["error_msg"] or "")[:500] or None, pt.app_utc(r["pushed_at"]))
                    for r in old_logs])
            report["庆祝历史推送视图"] = {"insert": len(old_logs)}
        if args.dry_run:
            mysql.rollback()
        else:
            mysql.commit()
    except Exception:
        mysql.rollback()
        raise
    finally:
        mysql.close()
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--copy-assets", action="store_true")
    parser.add_argument("--require-all-assets", action="store_true")
    parser.add_argument("--document-root", default=os.getenv("PORTAL_DOCUMENT_ROOT", "data/portal-documents"))
    parser.add_argument("--portal-upload-root", default=r"D:\code\CYT-Portal\backend\uploads")
    parser.add_argument("--gcs-private-bucket")
    parser.add_argument("--gcs-public-bucket")
    args = parser.parse_args()
    report = migrate(asyncio.run(read_source()), args)
    print("[dry-run 未写库]" if args.dry_run else "迁移完成")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
