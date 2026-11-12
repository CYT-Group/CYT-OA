"""Portal 搬迁脚本共用的 OA PostgreSQL 连接；不修改源 Portal 库。"""
import os
from pathlib import Path
import re
from urllib.parse import parse_qs, unquote, urlsplit

import javaproperties
import psycopg2
from psycopg2 import sql
from psycopg2.extensions import cursor as PgCursor
import yaml


def _resolve(value):
    def replace(match):
        return os.environ.get(match[1], match[2] or "")
    return re.sub(r"\$\{([^:}]+)(?::([^}]*))?\}", replace, "" if value is None else str(value))


def read_target_pg_conf(resources=None):
    resources = Path(resources or Path(__file__).resolve().parents[1] / "src/main/resources")
    datasource = {}
    profiles = os.environ.get("SPRING_PROFILES_ACTIVE", "local").split(",")
    names = ["application"] + [f"application-{p.strip()}" for p in profiles if p.strip()]
    for name in names:
        path = resources / f"{name}.yml"
        if path.is_file():
            settings = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
            datasource.update(settings.get("spring", {}).get("datasource", {}))
        # Spring Boot prefers .properties over YAML for the same configuration name.
        # Parse Java escapes and continuation lines rather than splitting on '='.
        path = resources / f"{name}.properties"
        if path.is_file():
            settings = javaproperties.loads(path.read_bytes())
            prefix = "spring.datasource."
            datasource.update({key.removeprefix(prefix): value
                               for key, value in settings.items() if key.startswith(prefix)})
    values = {}
    for key in ("url", "username", "password"):
        values[key] = _resolve(os.environ.get("SPRING_DATASOURCE_" + key.upper(), datasource.get(key, "")))
    jdbc = values["url"].strip()
    if not jdbc.startswith("jdbc:postgresql://"):
        raise ValueError("OA 目标库必须配置 jdbc:postgresql:// URL")
    parsed = urlsplit(jdbc.removeprefix("jdbc:"))
    if not parsed.hostname or not parsed.path.strip("/"):
        raise ValueError("OA PostgreSQL URL 缺少主机或数据库名")
    query = parse_qs(parsed.query)
    configuration = {
        "host": parsed.hostname, "port": parsed.port or 5432,
        "dbname": unquote(parsed.path.lstrip("/")),
        "user": values["username"], "password": values["password"],
        "options": query.get("options", ["-c TimeZone=Asia/Shanghai"])[0],
        "connect_timeout": 10,
    }
    # JDBC and libpq share the SSL parameter names; preserve an explicitly configured
    # verification policy instead of silently reverting to libpq's defaults.
    for key in ("sslmode", "sslrootcert", "sslcert", "sslkey", "sslpassword", "gssencmode"):
        if key in query:
            configuration[key] = query[key][-1]
    if "applicationName" in query:
        configuration["application_name"] = query["applicationName"][-1]
    if "connectTimeout" in query:
        configuration["connect_timeout"] = int(query["connectTimeout"][-1])
    if "currentSchema" in query:
        # libpq options split on unescaped whitespace, while PostgreSQL itself
        # interprets the resulting search_path (including comma-separated schemas).
        schema = query["currentSchema"][-1].replace("\\", "\\\\").replace(" ", "\\ ")
        configuration["options"] += " -c search_path=" + schema
    return configuration


class NumericBooleanCursor(PgCursor):
    """OA 的状态列保留 SMALLINT 0/1；只在目标连接转换 Python bool。"""
    @staticmethod
    def _parameters(parameters):
        if parameters is None:
            return None
        if isinstance(parameters, dict):
            return {k: int(v) if isinstance(v, bool) else v for k, v in parameters.items()}
        return tuple(int(v) if isinstance(v, bool) else v for v in parameters)

    def execute(self, query, vars=None):
        return super().execute(query, self._parameters(vars))

    def executemany(self, query, vars_list):
        return super().executemany(query, (self._parameters(values) for values in vars_list))


def connect_target(*, readonly=False):
    connection = psycopg2.connect(**read_target_pg_conf(), cursor_factory=NumericBooleanCursor)
    connection.set_session(readonly=readonly)
    return connection


def advance_target_sequences(connection):
    """保留源 ID 后向前推进 identity 序列；只用于停写期间的离线搬迁。"""
    with connection.cursor() as cur:
        cur.execute("SELECT table_schema,table_name,column_name,pg_get_serial_sequence("
                    "format('%I.%I',table_schema,table_name),column_name) "
                    "FROM information_schema.columns WHERE table_schema=current_schema() "
                    "AND (is_identity='YES' OR column_default LIKE 'nextval(%')")
        for schema, table, column, sequence in cur.fetchall():
            if sequence is None:
                continue
            cur.execute(sql.SQL("SELECT MAX({}) FROM {}").format(
                sql.Identifier(column), sql.Identifier(schema, table)))
            maximum = cur.fetchone()[0]
            if maximum is None:
                continue
            cur.execute("SELECT n.nspname,c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                        "WHERE c.oid=%s::regclass", (sequence,))
            sequence_schema, sequence_name = cur.fetchone()
            cur.execute(sql.SQL("SELECT last_value,is_called FROM {}").format(
                sql.Identifier(sequence_schema, sequence_name)))
            last, called = cur.fetchone()
            if maximum > last or maximum == last and not called:
                cur.execute("SELECT setval(%s::regclass,%s,true)", (sequence, maximum))
