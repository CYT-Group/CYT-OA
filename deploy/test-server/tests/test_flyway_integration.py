"""显式开启的 PostgreSQL + Flyway 集成验收；仅操作自动创建的临时数据库容器。"""
import os
from pathlib import Path
import runpy
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import unittest


TOOLS = Path(__file__).resolve().parents[1]
MODULE = runpy.run_path(str(TOOLS / 'migrate-database.py'))


@unittest.skipUnless(os.environ.get('OA_RUN_FLYWAY_INTEGRATION') == '1', '需显式开启 Docker 集成验收')
class FlywayIntegrationTests(unittest.TestCase):
    def test_empty_database_once_only_checksums_missing_files_and_sql_failure(self):
        name = 'cyt-oa-migration-it-' + secrets.token_hex(4)
        password = secrets.token_urlsafe(24)
        environment = dict(os.environ, POSTGRES_DB='oa_migration_test',
                           POSTGRES_USER='migration_test', POSTGRES_PASSWORD=password)
        subprocess.run(['docker', 'run', '-d', '--name', name, '--tmpfs', '/var/lib/postgresql/data',
                        '-e', 'POSTGRES_DB', '-e', 'POSTGRES_USER', '-e', 'POSTGRES_PASSWORD',
                        'postgres:17-alpine'], env=environment, check=True, capture_output=True)
        self.addCleanup(lambda: subprocess.run(['docker', 'rm', '-f', name], capture_output=True))

        def query(sql):
            result = subprocess.run(['docker', 'exec', '-i', name, 'sh', '-c',
                'PGPASSWORD="$POSTGRES_PASSWORD" exec psql -h127.0.0.1 -U "$POSTGRES_USER" '
                '-d "$POSTGRES_DB" -X -q -A -t -v ON_ERROR_STOP=1'],
                input=sql, text=True, capture_output=True)
            if result.returncode:
                raise RuntimeError(result.stderr)
            return result.stdout.strip()

        for _ in range(60):
            try:
                query('SELECT 1;')
                break
            except RuntimeError:
                time.sleep(1)
        else:
            self.fail('隔离 PostgreSQL 未就绪')

        with tempfile.TemporaryDirectory(prefix='oa-flyway-it-') as temporary:
            directory = Path(temporary)
            directory.chmod(0o755)
            source = Path(os.environ.get('OA_MIGRATION_SQL_DIR',
                          str(TOOLS.parents[1] / MODULE['MIGRATION_PATH'])))
            for path in source.glob('*.sql'):
                shutil.copyfile(path, directory / path.name)
            self.assertTrue(list(directory.glob('*.sql')), '必须测试真实首批迁移')
            create = directory / 'V20990101_001__create_integration_probe.sql'
            create.write_text('CREATE TABLE migration_probe (id INT PRIMARY KEY);\n')
            insert = directory / 'V20990101_002__insert_integration_probe.sql'
            insert.write_text('INSERT INTO migration_probe(id) VALUES (1);\n')

            def run(command, *extra):
                return subprocess.run([sys.executable, '-B', str(TOOLS / 'migrate-database.py'),
                    '--root', temporary, '--migrations-dir', temporary, '--database-container', name,
                    command, *extra], capture_output=True, text=True)

            def success(command, *extra):
                result = run(command, *extra)
                self.assertEqual(0, result.returncode, result.stdout + result.stderr)

            query('CREATE TABLE unmanaged_probe(id INT PRIMARY KEY);')
            self.assertNotEqual(0, run('migrate').returncode, '非空库不能被自动纳管')
            # 只删除本验收创建的探针，将隔离库恢复为空库后验证真实初始化迁移。
            query('DROP TABLE unmanaged_probe;')
            success('migrate')
            admin_count = query("SELECT COUNT(*) FROM sys_user WHERE username='admin';")
            self.assertEqual('1', admin_count, '初始化必须创建管理员账号')
            success('migrate')
            self.assertEqual(admin_count, query("SELECT COUNT(*) FROM sys_user WHERE username='admin';"),
                             '重复迁移不能重复导入初始种子')
            self.assertEqual('1', query('SELECT COUNT(*) FROM migration_probe;'), '成功版本不能重复执行')
            self.assertEqual('1', query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='portal_hr_layout_jobs';"))
            # Explicit seed IDs must leave identity sequences beyond existing rows.
            last_role_id = int(query('SELECT MAX(id) FROM sys_role;'))
            role_id = int(query("INSERT INTO sys_role(code,name,built_in) VALUES "
                                "('migration_it','Migration integration',0) RETURNING id;"))
            self.assertGreater(role_id, last_role_id)

            # Touch timestamps only for changed data and preserve explicit historical times.
            original_time = query("SELECT updated_at FROM sys_user WHERE username='admin';")
            query("UPDATE sys_user SET name=name WHERE username='admin';")
            self.assertEqual(original_time, query("SELECT updated_at FROM sys_user WHERE username='admin';"))
            query("UPDATE sys_user SET name=name || '-history', updated_at='2001-01-01 00:00:00' "
                  "WHERE username='admin';")
            self.assertEqual('2001-01-01 00:00:00', query("SELECT updated_at FROM sys_user WHERE username='admin';"))
            query("UPDATE sys_user SET name=name || '-changed' WHERE username='admin';")
            self.assertEqual('t', query("SELECT updated_at > TIMESTAMP '2001-01-01' "
                                       "FROM sys_user WHERE username='admin';"))
            contract = TOOLS.parents[1] / 'cyt-oa-backend/src/test/resources/db/postgresql-schema-contract.sql'
            query(contract.read_text(encoding='utf-8'))

            original = create.read_text()
            create.write_text(original + 'ALTER TABLE migration_probe ADD COLUMN tampered INT;\n')
            self.assertNotEqual(0, run('validate').returncode, '修改已执行 SQL 必须阻断部署')
            create.write_text(original)
            saved = insert.read_text()
            insert.unlink()
            self.assertNotEqual(0, run('validate').returncode, '旧分支缺失已执行 SQL 必须阻断部署')
            insert.write_text(saved)
            success('validate')

            (directory / 'V20990101_003__invalid_sql.sql').write_text('THIS IS INVALID SQL;\n')
            (directory / 'V20990101_004__must_not_run.sql').write_text('INSERT INTO migration_probe(id) VALUES (2);\n')
            self.assertNotEqual(0, run('migrate').returncode, 'SQL 失败必须返回非零状态')
            self.assertEqual('1', query('SELECT COUNT(*) FROM migration_probe;'), '失败后不能继续执行后续版本')


if __name__ == '__main__':
    unittest.main()
