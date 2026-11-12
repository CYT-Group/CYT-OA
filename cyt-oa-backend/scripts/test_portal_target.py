"""OA PostgreSQL target configuration and opt-in isolated-schema integration checks."""
from datetime import date, datetime, time, timezone
import asyncio
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import AsyncMock, patch
import uuid

import psycopg2
from psycopg2 import sql

from migrate_portal_esports import _time
import migrate_portal_esports as esports
import migrate_portal_identity as identity
import migrate_portal_materials_ai as materials
import portal_target
import portal_time


class TargetConfigurationTests(unittest.TestCase):
    def configuration(self, base, local="", environment=None, extra_files=None):
        with tempfile.TemporaryDirectory() as temporary, patch.dict(os.environ, environment or {}, clear=True):
            resources = Path(temporary)
            (resources / "application.yml").write_text(base, encoding="utf-8")
            if local:
                (resources / "application-local.yml").write_text(local, encoding="utf-8")
            for name, content in (extra_files or {}).items():
                (resources / name).write_text(content, encoding="utf-8")
            return portal_target.read_target_pg_conf(resources)

    def test_base_profile_and_environment_precedence_preserve_password(self):
        conf = self.configuration(
            "spring:\n  datasource:\n    url: jdbc:postgresql://${CYT_PG_HOST:localhost}:5432/oa\n"
            "    username: ${CYT_PG_USERNAME:oa}\n    password: base\n",
            "spring:\n  datasource:\n    password: local\n",
            {"CYT_PG_HOST": "pg", "CYT_PG_USERNAME": "migration", "SPRING_DATASOURCE_PASSWORD": " secret=值 "},
        )
        self.assertEqual(("pg", 5432, "oa", "migration", " secret=值 "),
                         tuple(conf[k] for k in ("host", "port", "dbname", "user", "password")))

    def test_connection_policy_and_encoded_database_are_preserved(self):
        conf = self.configuration("spring:\n  datasource:\n    url: "
            "jdbc:postgresql://pg:5544/oa%20test?sslmode=verify-full&sslrootcert=%2Fcerts%2Fca.pem"
            "&applicationName=portal-migration&connectTimeout=25&currentSchema=oa"
            "\n    username: oa\n    password:\n")
        self.assertEqual("oa test", conf["dbname"])
        self.assertEqual("verify-full", conf["sslmode"])
        self.assertEqual("/certs/ca.pem", conf["sslrootcert"])
        self.assertEqual("portal-migration", conf["application_name"])
        self.assertEqual(25, conf["connect_timeout"])
        self.assertEqual("", conf["password"])
        self.assertIn("search_path=oa", conf["options"])

    def test_unsupported_target_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "PostgreSQL|postgresql"):
            self.configuration("spring:\n  datasource:\n    url: jdbc:unsupported://localhost/oa\n")

    def test_nonlocal_profile_does_not_load_local_settings(self):
        conf = self.configuration(
            "spring:\n  datasource:\n    url: jdbc:postgresql://pg/oa\n    password: base\n",
            "spring:\n  datasource:\n    password: local\n",
            {"SPRING_PROFILES_ACTIVE": "test"},
        )
        self.assertEqual("base", conf["password"])

    def test_prod_properties_load_custom_placeholders_without_local_settings(self):
        conf = self.configuration(
            "spring:\n  datasource:\n    driver-class-name: org.postgresql.Driver\n",
            "spring:\n  datasource:\n    url: jdbc:postgresql://local/local\n    password: local\n",
            {"SPRING_PROFILES_ACTIVE": "prod", "CYT_PG_HOST": "prod-db",
             "CYT_PG_PORT": "5544", "CYT_PG_DATABASE": "oa-prod",
             "CYT_PG_USERNAME": "oa-prod-user", "CYT_PG_PASSWORD": " secret=:#值 "},
            {"application-prod.properties":
             "spring.datasource.url=jdbc:postgresql://${CYT_PG_HOST}:${CYT_PG_PORT:5432}/${CYT_PG_DATABASE}\n"
             "spring.datasource.username=${CYT_PG_USERNAME}\n"
             "spring.datasource.password=${CYT_PG_PASSWORD:}\n"},
        )
        self.assertEqual(("prod-db", 5544, "oa-prod", "oa-prod-user", " secret=:#值 "),
                         tuple(conf[k] for k in ("host", "port", "dbname", "user", "password")))

    def test_properties_override_yaml_then_later_profiles_override_earlier_profiles(self):
        conf = self.configuration(
            "spring:\n  datasource:\n    url: jdbc:postgresql://base/oa\n"
            "    username: base-yaml\n    password: base-yaml\n",
            environment={"SPRING_PROFILES_ACTIVE": "prod, override"},
            extra_files={
                "application.properties": "spring.datasource.username=base-properties\n",
                "application-prod.yml": "spring:\n  datasource:\n    password: prod-yaml\n",
                "application-prod.properties": "spring.datasource.password=prod-properties\n",
                "application-override.yml": "spring:\n  datasource:\n    url: jdbc:postgresql://override/oa\n",
            },
        )
        self.assertEqual(("override", "base-properties", "prod-properties"),
                         tuple(conf[k] for k in ("host", "user", "password")))

    def test_java_properties_escapes_continuations_and_environment_override(self):
        properties = (
            "spring.datasource.url: jdbc:postgresql://prod/oa\\\n"
            "    ?options=-c%20TimeZone=Asia/Shanghai\n"
            "spring.datasource.username prod\\ user\n"
            r"spring.datasource.password=\ leading\=value\:\#\!\\\u503c\ " "\n"
        )
        extra_files = {"application-prod.properties": properties}
        conf = self.configuration("", environment={"SPRING_PROFILES_ACTIVE": "prod"},
                                  extra_files=extra_files)
        self.assertEqual("prod user", conf["user"])
        self.assertEqual(" leading=value:#!\\值 ", conf["password"])
        self.assertEqual("-c TimeZone=Asia/Shanghai", conf["options"])

        conf = self.configuration(
            "", environment={"SPRING_PROFILES_ACTIVE": "prod",
                             "SPRING_DATASOURCE_URL": "jdbc:postgresql://environment/oa",
                             "SPRING_DATASOURCE_USERNAME": "env-user",
                             "SPRING_DATASOURCE_PASSWORD": " env\\=值 "},
            extra_files=extra_files,
        )
        self.assertEqual(("environment", "env-user", " env\\=值 "),
                         tuple(conf[k] for k in ("host", "user", "password")))


class PortalTimeTests(unittest.TestCase):
    def test_date_and_time_keep_their_distinct_iso_values(self):
        self.assertEqual("2026-11-12", _time(date(2026, 11, 12)))
        self.assertEqual("12:03:04.123456", _time(time(12, 3, 4, 123456)))

    def test_timezone_aware_values_are_converted_once_to_local_wall_time(self):
        aware = datetime(2026, 7, 23, 0, 3, 4, 123456, tzinfo=timezone.utc)
        expected = datetime(2026, 7, 23, 8, 3, 4, 123456)
        self.assertEqual(expected, portal_time.app_utc(aware))
        self.assertEqual(expected, portal_time.server_clock(aware))
        self.assertTrue(portal_time.is_cutover_day(aware))


@unittest.skipUnless(os.environ.get("OA_TEST_POSTGRES_DSN"), "set OA_TEST_POSTGRES_DSN for isolated-schema PG checks")
class TargetPostgresIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.dsn = os.environ["OA_TEST_POSTGRES_DSN"]
        self.schema = "portal_migration_it_" + uuid.uuid4().hex
        self.admin = psycopg2.connect(self.dsn)
        self.admin.autocommit = True
        self.addCleanup(self.admin.close)
        with self.admin.cursor() as cursor:
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(self.schema)))
        self.addCleanup(self.drop_schema)
        self.configuration = {"dsn": self.dsn, "options": "-c TimeZone=Asia/Shanghai -c search_path=" + self.schema}
        self.patcher = patch("portal_target.read_target_pg_conf", return_value=self.configuration)
        self.patcher.start()
        self.addCleanup(self.patcher.stop)

    def drop_schema(self):
        with self.admin.cursor() as cursor:
            cursor.execute(sql.SQL("DROP SCHEMA {} CASCADE").format(sql.Identifier(self.schema)))

    def connect(self, readonly=False):
        connection = portal_target.connect_target(readonly=readonly)
        self.addCleanup(connection.close)
        return connection

    def test_numeric_flags_text_json_and_fractional_timestamps(self):
        connection = self.connect()
        value = datetime(2026, 11, 12, 13, 14, 15, 123456)
        with connection.cursor() as cursor:
            cursor.execute("CREATE TABLE records(id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                           "enabled SMALLINT, document TEXT CHECK (document::jsonb IS NOT NULL), "
                           "happened_at TIMESTAMP(6))")
            cursor.execute("INSERT INTO records(id,enabled,document,happened_at) VALUES(%s,%s,%s,%s)",
                           (42, True, json.dumps({"中文": [True, 1]}, ensure_ascii=False), value))
            cursor.execute("SELECT enabled,document,happened_at FROM records")
            enabled, document, timestamp = cursor.fetchone()
            self.assertEqual(1, enabled)
            self.assertEqual({"中文": [True, 1]}, json.loads(document))
            self.assertEqual(value, timestamp)
        portal_target.advance_target_sequences(connection)
        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO records(enabled,document) VALUES(%s,%s) RETURNING id", (False, "[]"))
            self.assertEqual(43, cursor.fetchone()[0])
        connection.commit()

    def test_identity_and_serial_sequences_advance_without_rewinding(self):
        connection = self.connect()
        with connection.cursor() as cursor:
            cursor.execute('CREATE TABLE "Rows.With.Dot" (id BIGINT GENERATED BY DEFAULT AS IDENTITY)')
            cursor.execute('INSERT INTO "Rows.With.Dot"(id) VALUES (70)')
            cursor.execute("CREATE TABLE serial_rows(id BIGSERIAL)")
            cursor.execute("INSERT INTO serial_rows(id) VALUES (20)")
            cursor.execute("CREATE TABLE already_ahead(id BIGINT GENERATED BY DEFAULT AS IDENTITY)")
            cursor.execute("INSERT INTO already_ahead(id) VALUES (30)")
            cursor.execute("SELECT setval(pg_get_serial_sequence('already_ahead','id'),500,true)")
        portal_target.advance_target_sequences(connection)
        with connection.cursor() as cursor:
            for table, expected in [('"Rows.With.Dot"', 71), ("serial_rows", 21), ("already_ahead", 501)]:
                cursor.execute("INSERT INTO " + table + " DEFAULT VALUES RETURNING id")
                self.assertEqual(expected, cursor.fetchone()[0])
        connection.commit()

    def test_dry_run_uses_database_enforced_readonly_transaction(self):
        writer = self.connect()
        with writer.cursor() as cursor:
            cursor.execute("CREATE TABLE readonly_probe(id INT)")
        writer.commit()
        connection = self.connect(readonly=True)
        with connection.cursor() as cursor:
            cursor.execute("SHOW transaction_read_only")
            self.assertEqual("on", cursor.fetchone()[0])
            with self.assertRaises(psycopg2.errors.ReadOnlySqlTransaction):
                cursor.execute("INSERT INTO readonly_probe VALUES(1)")
        connection.rollback()
        with writer.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM readonly_probe")
            self.assertEqual(0, cursor.fetchone()[0])

    def test_identity_esports_and_material_archives_against_current_schema(self):
        resources = Path(__file__).resolve().parents[1] / "src/main/resources/db/postgresql/migration"
        connection = self.connect()
        with connection.cursor() as cursor:
            for path in sorted(resources.glob("*.sql")):
                cursor.execute(path.read_text(encoding="utf-8"))
        connection.commit()
        departments = [{"id": 90001, "parent_id": None, "name": "PG migration", "sort_order": 0,
                        "leader_userids": ["pg_migration_user"], "is_active": True}]
        users = [{"id": 90001, "username": "pg_migration_user", "display_name": "搬迁验收",
                  "role": "pg_migration_role", "department_ids": [90001], "phone": None, "email": None,
                  "avatar_url": None, "is_active": True, "birthday": date(2000, 1, 2),
                  "hire_date": date(2020, 2, 3), "celebration_opt_out": False}]
        roles = [{"key": "pg_migration_role", "name": "Migration role", "description": ""}]
        grants = [{"role_key": "pg_migration_role", "permission_key": "pg_migration_permission"}]
        identity.migrate(departments, users, roles, grants, [], dry_run=True)
        with connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM sys_user WHERE username='pg_migration_user'")
            self.assertEqual(0, cursor.fetchone()[0])
        connection.rollback()
        identity.migrate(departments, users, roles, grants, [], dry_run=False)
        with connection.cursor() as cursor:
            cursor.execute("SELECT id,enabled FROM sys_user WHERE username='pg_migration_user'")
            owner, enabled = cursor.fetchone()
            self.assertEqual(1, enabled)
            cursor.execute("SELECT birthday,entry_time FROM hrm_employee WHERE user_id=%s", (owner,))
            self.assertEqual((datetime(2000, 1, 2), datetime(2020, 2, 3)), cursor.fetchone())
        connection.rollback()

        stamp = datetime(2026, 11, 12, 2, 3, 4, 123456)
        booking = {"id": 90001, "booking_date": date(2026, 11, 12),
                   "start_time": time(12, 0), "end_time": time(13, 0), "title": "PG booking",
                   "purpose": "integration", "participant_count": 1, "participants": ["pg_migration_user"],
                   "applicant_user_id": owner, "applicant_name": "搬迁验收", "approver_user_id": owner,
                   "approver_name": "搬迁验收", "approval_mode": "any", "current_stage_index": 0,
                   "status": "pending", "reviewed_by_user_id": None, "reviewed_by_name": None,
                   "review_remark": None, "reviewed_at": None, "cancelled_by_user_id": None,
                   "cancelled_at": None, "created_at": stamp, "updated_at": stamp}
        source = {src: [] for src, _ in esports.TABLES}
        source["portal_esports_bookings"] = [booking]
        esports.migrate(source, dry_run=False)
        with connection.cursor() as cursor:
            cursor.execute("SELECT booking_date,participants,created_at FROM portal_esports_bookings WHERE id=90001")
            booked, participants, created = cursor.fetchone()
            self.assertEqual(date(2026, 11, 12), booked)
            self.assertEqual(["pg_migration_user"], json.loads(participants))
            self.assertEqual(datetime(2026, 11, 12, 10, 3, 4, 123456), created)
        connection.rollback()

        with connection.cursor() as cursor:
            cursor.execute("INSERT INTO portal_categories(id,scope,name,created_by) "
                           "VALUES(1,'materials','Migration category','pg_migration_user')")
        connection.commit()

        product = {"id": 90001, "kind": "sample", "category_id": 1, "name": "Archived PG product",
                   "created_by": "pg_migration_user", "image_urls": [], "variant_dims": [],
                   "archived_at": stamp, "created_at": stamp, "updated_at": stamp}
        material = {"id": 90001, "kind": "sample", "product_id": 90001, "category": "integration",
                    "name": "Archived PG SKU", "owner_username": "pg_migration_user", "owner_name": "搬迁验收",
                    "created_by": "pg_migration_user", "variant_values": {}, "image_urls": [],
                    "current_borrowers": [], "archived_at": stamp, "created_at": stamp, "updated_at": stamp}
        records = {table: [] for table in materials.TABLES}
        records["portal_material_site_owners"] = [{"site": "SZ", "kind": "sample",
            "owner_username": "pg_migration_user", "owner_name": "搬迁验收", "updated_at": stamp}]
        records["portal_material_products"] = [product]
        records["portal_materials"] = [material]

        class Source:
            async def fetch(self, statement):
                table = statement.split(" FROM ", 1)[1].split()[0]
                return records[table]

            async def close(self):
                pass

        with patch.object(materials, "read_pg_dsn", return_value="postgresql://fixture.invalid/readonly"), \
             patch.object(materials.asyncpg, "connect", new=AsyncMock(return_value=Source())):
            report = asyncio.run(materials.migrate(dry_run=False))
        self.assertEqual(2, report["archive_groups"])
        with connection.cursor() as cursor:
            cursor.execute("SELECT archived_at,archive_group,updated_at FROM portal_material_products WHERE id=90001")
            archived, group, updated = cursor.fetchone()
            self.assertEqual(datetime(2026, 11, 12, 10, 3, 4, 123456), archived)
            self.assertIsNotNone(group)
            self.assertEqual(stamp, updated)
            cursor.execute("SELECT archive_group,updated_at FROM portal_materials WHERE id=90001")
            self.assertEqual((group, stamp), cursor.fetchone())


if __name__ == "__main__":
    unittest.main()
