"""原 HR 智能排版历史迁移。默认 dry-run；--apply 才写入，不重放任何 AI 调用。

保留任务 ID / 正文 / 成功 blocks，按旧提交人 username 映射 OA sys_user。
源 created_at 和 finished_at 均为 PG timestamptz，按真实时区转为北京时间裸值；
不使用其他旧表的 server_clock 分段规则。pending/running 一律转 failed。
目标同 ID 若不是同提交人同正文则报冲突并回滚，绝不覆盖新建 OA 任务。
"""
import argparse
import asyncio
import json
from datetime import datetime, timezone, timedelta
import asyncpg
import pymysql
from migrate_portal_identity import read_mysql_conf, read_pg_dsn

SHANGHAI = timezone(timedelta(hours=8))


def local_time(value):
    if value is None:
        return None
    if value.tzinfo is None:
        raise ValueError("HR layout timestamp must be timezone-aware")
    return value.astimezone(SHANGHAI).replace(tzinfo=None)


def transform(row, owner_id):
    interrupted = row["status"] in ("pending", "running")
    blocks = row.get("blocks")
    if blocks is not None and not isinstance(blocks, str):
        blocks = json.dumps(blocks, ensure_ascii=False)
    return {
        "id": row["id"], "user_id": owner_id, "actor_username": row["username"],
        "status": "failed" if interrupted else row["status"],
        "source_type": row["source_type"], "source_name": row["source_name"],
        "input_text": row["input_text"], "input_chars": row["input_chars"],
        "progress": row["progress"], "stage": "迁移时中断" if interrupted else row["stage"],
        "summary": row["summary"], "blocks": blocks,
        "error": "门户迁移时任务中断，未自动重放；可手动重试" if interrupted else row["error"],
        "created_at": local_time(row["created_at"]),
        "finished_at": datetime.now(SHANGHAI).replace(tzinfo=None) if interrupted else local_time(row["finished_at"]),
    }


async def fetch_portal():
    pg = await asyncpg.connect(read_pg_dsn())
    try:
        return [dict(row) for row in await pg.fetch(
            "SELECT j.*,u.username FROM portal_hr_layout_jobs j "
            "JOIN portal_users u ON u.id=j.user_id ORDER BY j.id")]
    finally:
        await pg.close()


def migrate(rows, apply=False):
    connection = pymysql.connect(**read_mysql_conf())
    report = {"source": len(rows), "insert": 0, "skip": 0, "interrupted": 0, "unmapped": []}
    try:
        with connection.cursor() as cursor:
            cursor.execute("SELECT username,id FROM sys_user")
            owners = dict(cursor.fetchall())
            cursor.execute("SELECT id,actor_username,input_text FROM portal_hr_layout_jobs")
            existing = {row[0]: row[1:] for row in cursor.fetchall()}
            for source in rows:
                if source["username"] not in owners:
                    report["unmapped"].append({"id": source["id"], "username": source["username"]})
                    continue
                row = transform(source, owners[source["username"]])
                if row["id"] in existing:
                    if existing[row["id"]] != (row["actor_username"], row["input_text"]):
                        raise ValueError(f"任务 ID {row['id']} 与新 OA 任务冲突，未覆盖；请协调编号后重试")
                    report["skip"] += 1
                    continue
                report["insert"] += 1
                report["interrupted"] += source["status"] in ("pending", "running")
                if apply:
                    columns = list(row)
                    cursor.execute("INSERT INTO portal_hr_layout_jobs (" + ",".join(columns) + ") VALUES (" + ",".join(["%s"] * len(columns)) + ")", tuple(row[c] for c in columns))
            if report["unmapped"] and apply:
                raise ValueError("存在未映射提交人，未写入任何任务：" + json.dumps(report["unmapped"], ensure_ascii=False))
        if apply:
            connection.commit()
        else:
            connection.rollback()
        return report
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--dry-run", action="store_true", help="只读统计，默认行为")
    group.add_argument("--apply", action="store_true", help="幂等写入历史，不启动 AI 任务")
    args = parser.parse_args()
    print(json.dumps(migrate(asyncio.run(fetch_portal()), args.apply), ensure_ascii=False, indent=2))
