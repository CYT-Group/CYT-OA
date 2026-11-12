"""物资台账和 AI 管理从 Portal PG 搬到 OA PostgreSQL；保留原主键，已存在行跳过。

先执行 portal-materials-schema.sql / portal-ai-schema.sql，随后 --dry-run 核对，
去掉 --dry-run 才写入。本脚本不修改源 PG、不解密或输出 API key。
"""
import argparse
import asyncio
import datetime as dt
import json
import uuid
from decimal import Decimal

import asyncpg
from portal_target import connect_target, advance_target_sequences

from migrate_portal_identity import read_pg_dsn
import portal_time as pt

TABLES = {
    "portal_material_site_owners": ("site", "kind"),
    "portal_material_products": ("id",),
    "portal_materials": ("id",),
    "portal_material_applies": ("id",),
    "portal_material_apply_items": ("id",),
    "portal_material_apply_logs": ("id",),
    "portal_ai_models": ("id",),
    "portal_ai_provider_settings": ("provider",),
    "portal_ai_task_settings": ("task_name",),
    "portal_ai_task_setting_versions": ("id",),
    "portal_api_call_log": ("id",),
}


def converted(table, row, columns):
    out = {}
    for column, data_type in columns:
        if column not in row:
            continue  # OA 新增的可空或有默认值列交给目标库。
        value = row[column]
        if isinstance(value, dt.datetime):
            # 物资正常建改由 PG func.now() 写，归档由 datetime.utcnow() 写。
            utc = column == "archived_at"
            if table.startswith("portal_ai_"):
                utc = True
                if table == "portal_ai_models":
                    actor = row.get("created_by" if column == "created_at" else "updated_by")
                    utc = actor not in (None, "system")  # 目录初始 seed 是数据库时钟。
                elif table == "portal_ai_provider_settings" and column == "updated_at":
                    utc = row.get("updated_by") not in (None, "system")
            if table == "portal_api_call_log":
                utc = True
            value = pt.app_utc(value) if utc else pt.server_clock(value)
        if isinstance(value, (dict, list)):
            value = json.dumps(value, ensure_ascii=False)
        if isinstance(value, bool):
            value = int(value)
        out[column] = value
    return out


async def migrate(dry_run):
    pg = await asyncpg.connect(read_pg_dsn())
    target = connect_target(readonly=dry_run)
    report = {}
    try:
        with target.cursor() as cur:
            for table, keys in TABLES.items():
                cur.execute("SELECT COLUMN_NAME,DATA_TYPE FROM information_schema.columns "
                            "WHERE table_schema=current_schema() AND table_name=%s "
                            "AND is_generated='NEVER' ORDER BY ORDINAL_POSITION", (table,))
                columns = cur.fetchall()
                if not columns:
                    raise RuntimeError(f"目标表 {table} 不存在，请先执行建表脚本")
                cur.execute("SELECT " + ",".join(keys) + " FROM " + table)
                existing = set(cur.fetchall())
                records = await pg.fetch("SELECT * FROM " + table + " ORDER BY " + ",".join(keys))
                pending = [converted(table, dict(row), columns) for row in records
                           if tuple(row[key] for key in keys) not in existing]
                if pending and not dry_run:
                    names = list(pending[0])
                    sql = "INSERT INTO " + table + " (" + ",".join('"' + x + '"' for x in names) + ") VALUES (" + ",".join(["%s"] * len(names)) + ")"
                    for start in range(0, len(pending), 500):
                        cur.executemany(sql, [tuple(row[name] for name in names) for row in pending[start:start + 500]])
                report[table] = {"source": len(records), "existing": len(records) - len(pending), "pending" if dry_run else "inserted": len(pending)}
            product_archives = {r["id"]: r["archived_at"] for r in await pg.fetch("SELECT id,archived_at FROM portal_material_products")}
            archive_groups = 0
            for table in ("portal_material_products", "portal_materials"):
                fields = "id,archived_at" + (",product_id" if table == "portal_materials" else "")
                for row in await pg.fetch("SELECT " + fields + " FROM " + table + " WHERE archived_at IS NOT NULL"):
                    product_id = row["product_id"] if table == "portal_materials" else row["id"]
                    grouped = table == "portal_material_products" or product_archives.get(product_id) == row["archived_at"]
                    identity = (f"product:{product_id}" if grouped else f"sku:{row['id']}") + ":" + row["archived_at"].isoformat()
                    batch = str(uuid.uuid5(uuid.NAMESPACE_URL, "cyt-portal-archive:" + identity))
                    archived_at = pt.app_utc(row["archived_at"])
                    cur.execute("SELECT updated_at FROM " + table + " WHERE id=%s AND archived_at=%s AND archive_group IS NULL", (row["id"], archived_at))
                    previous = cur.fetchone()
                    archive_groups += previous is not None
                    if previous is not None and not dry_run:
                        cur.execute("UPDATE " + table + " SET archive_group=%s WHERE id=%s AND archive_group IS NULL", (batch, row["id"]))
                        # Adding migration metadata must not rewrite the imported business timestamp.
                        # The first UPDATE invokes the normal touch trigger; explicitly restoring
                        # the historical value is preserved by that trigger.
                        cur.execute("UPDATE " + table + " SET updated_at=%s WHERE id=%s AND archive_group=%s",
                                    (previous[0], row["id"], batch))
            report["archive_groups"] = archive_groups
            if not dry_run:
                advance_target_sequences(target)
                target.commit()
            else:
                target.rollback()
        return report
    except BaseException:
        target.rollback()
        raise
    finally:
        target.close()
        await pg.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    print(json.dumps(asyncio.run(migrate(args.dry_run)), ensure_ascii=False, indent=2))
