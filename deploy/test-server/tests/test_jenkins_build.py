import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).parents[1] / 'jenkins-build.sh'


@unittest.skipUnless(shutil.which('git') and shutil.which('bash'), '需要 Git 和 Bash')
class JenkinsBuildTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='cyt-oa-jenkins-test-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.branch = 'feature/team/分支验收'
        self.git('init', '-b', self.branch)
        self.git('-c', 'user.name=CI fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '--allow-empty', '-qm', 'metadata fixture')
        self.sha = self.git('rev-parse', 'HEAD').strip()
        deploy = self.root / 'deploy/test-server'
        deploy.mkdir(parents=True)
        (deploy / 'ci-build.sh').write_text('printf "%s" "$SKIP_DEPLOY" > deployment-ran\n', encoding='utf-8')
        self.tools = self.root / 'server-tools'
        self.tools.mkdir()

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True, stderr=subprocess.DEVNULL)

    def run_build(self, **extra):
        env = os.environ.copy()
        env.update(WORKSPACE=str(self.root), BRANCH=self.branch, GIT_COMMIT=self.sha,
                   TARGET='web', BUILD_NUMBER='42', SKIP_DEPLOY='true', VERIFY_ONLY='true',
                   OA_DEPLOY_TOOLS_DIR=str(self.tools))
        env.update(extra)
        return subprocess.run(['bash', str(SCRIPT)], cwd=self.root, env=env,
                              text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def test_verify_only_preserves_unicode_branch_and_never_deploys(self):
        result = self.run_build()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertFalse((self.root / 'deployment-ran').exists())
        metadata = json.loads((self.root / '.jenkins-build/42/build-info.json').read_text(encoding='utf-8'))
        self.assertEqual(self.branch, metadata['branch'])
        self.assertEqual(self.sha, metadata['commit'])
        self.assertTrue(metadata['verifyOnly'])
        self.assertIn('%2F', metadata['branchUrl'])

    def test_wrong_commit_stops_before_build(self):
        result = self.run_build(GIT_COMMIT='0' * 40, VERIFY_ONLY='false')
        self.assertEqual(3, result.returncode)
        self.assertFalse((self.root / 'deployment-ran').exists())

    def test_normal_build_passes_skip_deploy_to_existing_entrypoint(self):
        result = self.run_build(VERIFY_ONLY='false')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('true', (self.root / 'deployment-ran').read_text())

    def prepare_deployment_stages(self, migration_exit):
        deploy = self.root / 'deploy/test-server'
        (deploy / 'ci-build.sh').write_text('echo build >> stages\n', encoding='utf-8')
        (self.tools / 'db-upgrade.sh').write_text(
            f'echo migrate >> stages\nexit {migration_exit}\n', encoding='utf-8')
        (deploy / 'redeploy.sh').write_text('echo deploy >> stages\n', encoding='utf-8')

    def test_full_deployment_builds_then_migrates_then_updates_containers(self):
        self.prepare_deployment_stages(0)
        result = self.run_build(VERIFY_ONLY='false', SKIP_DEPLOY='false')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(['build', 'migrate', 'deploy'], (self.root / 'stages').read_text().splitlines())

    def test_failed_migration_prevents_container_update(self):
        self.prepare_deployment_stages(42)
        result = self.run_build(VERIFY_ONLY='false', SKIP_DEPLOY='false')
        self.assertEqual(42, result.returncode)
        self.assertEqual(['build', 'migrate'], (self.root / 'stages').read_text().splitlines())


if __name__ == '__main__':
    unittest.main()
