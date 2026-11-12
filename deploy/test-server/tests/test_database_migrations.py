from pathlib import Path
import json
import runpy
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace


MODULE = runpy.run_path(str(Path(__file__).parents[1] / 'migrate-database.py'))


class MigrationPolicyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)

    def write(self, name, content=b'SELECT 1;\n'):
        (self.directory / name).write_bytes(content)

    def test_valid_versions_are_sorted(self):
        self.write('V20261112_001__add_index.sql')
        self.write('V20261111_002__add_column.sql')
        files = MODULE['check_migrations'](self.directory)
        self.assertEqual('V20261111_002__add_column.sql', files[0].name)

    def test_duplicate_version_is_rejected(self):
        self.write('V20261111_001__first.sql')
        self.write('V20261111_001__second.sql')
        with self.assertRaisesRegex(ValueError, '重复'):
            MODULE['check_migrations'](self.directory)

    def test_invalid_names_dates_and_repeatables_are_rejected(self):
        for name in ['update.sql', 'R__seed.sql', 'V20261301_001__bad.sql', 'V20261111_000__bad.sql']:
            with self.subTest(name=name):
                self.write(name)
                with self.assertRaises(ValueError):
                    MODULE['check_migrations'](self.directory)
                (self.directory / name).unlink()

    def test_utf8_bom_and_non_utf8_are_rejected(self):
        for content in [b'\xef\xbb\xbfSELECT 1;', b'\xffSELECT 1;']:
            with self.subTest(content=content):
                self.write('V20261111_001__bad_encoding.sql', content)
                with self.assertRaises(ValueError):
                    MODULE['check_migrations'](self.directory)

    def test_missing_or_empty_directory_is_not_silently_skipped(self):
        for directory in [self.directory, self.directory / 'missing']:
            with self.assertRaises(ValueError):
                MODULE['check_migrations'](directory)

    def test_flyway_is_strict_and_does_not_embed_passwords(self):
        cmd = MODULE['flyway_command']('oa-db', self.directory, 'flyway:test', 'migrate')
        self.assertIn('FLYWAY_PASSWORD', cmd)
        self.assertFalse(any(item.startswith('FLYWAY_PASSWORD=') for item in cmd))
        for option in ['-baselineOnMigrate=false', '-cleanDisabled=true', '-validateOnMigrate=true',
                       '-outOfOrder=false', '-ignoreMigrationPatterns=']:
            self.assertIn(option, cmd)

    def test_database_environment_uses_postgresql_credentials_without_logging_password(self):
        info = [{'State': {'Running': True}, 'Config': {'Env': [
            'POSTGRES_DB=cyt_oa', 'POSTGRES_USER=oa', 'POSTGRES_PASSWORD=secret=value']}}]
        with patch('subprocess.run', return_value=SimpleNamespace(stdout=json.dumps(info))) as run:
            environment = MODULE['database_environment']('test-db')
        self.assertEqual('jdbc:postgresql://127.0.0.1:5432/cyt_oa', environment['FLYWAY_URL'])
        self.assertEqual('oa', environment['FLYWAY_USER'])
        self.assertEqual('secret=value', environment['FLYWAY_PASSWORD'])
        self.assertEqual(['docker', 'inspect', 'test-db'], run.call_args.args[0])

    def test_unsupported_database_container_is_rejected_before_running_migrations(self):
        info = [{'State': {'Running': True}, 'Config': {'Env': [
            'DATABASE_URL=unsupported://database/oa']}}]
        with patch('subprocess.run', return_value=SimpleNamespace(stdout=json.dumps(info))):
            with self.assertRaisesRegex(ValueError, 'POSTGRES_DB'):
                MODULE['database_environment']('unsupported-db')


if __name__ == '__main__':
    unittest.main()
