import re
import runpy
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

MODULE = runpy.run_path(str(Path(__file__).parents[1] / 'configure-jenkins.py'))
CONFIGURE = MODULE['job_config']
SOURCE = b'''<project><description>old</description><displayName>Existing OA job</displayName>
<properties><hudson.model.ParametersDefinitionProperty><parameterDefinitions>
<hudson.model.ChoiceParameterDefinition><name>BRANCH</name><choices><string>main</string></choices></hudson.model.ChoiceParameterDefinition>
<hudson.model.ChoiceParameterDefinition><name>TARGET</name><choices><string>all</string><string>web</string><string>api</string></choices></hudson.model.ChoiceParameterDefinition>
<hudson.model.BooleanParameterDefinition><name>SKIP_DEPLOY</name><defaultValue>false</defaultValue></hudson.model.BooleanParameterDefinition>
</parameterDefinitions></hudson.model.ParametersDefinitionProperty></properties>
<scm class="hudson.scm.NullSCM"/><concurrentBuild>false</concurrentBuild>
<builders><hudson.tasks.Shell><command>old download script</command></hudson.tasks.Shell></builders>
<publishers><hudson.tasks.JUnitResultArchiver/></publishers><buildWrappers/>
</project>'''


class JenkinsConfigurationTests(unittest.TestCase):
    def configured(self):
        return ET.fromstring(CONFIGURE(SOURCE, 'cyt-oa-deploy'))

    def test_dynamic_branch_matches_actual_repository(self):
        root = self.configured()
        definition = root.find('.//net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition')
        self.assertEqual('PT_BRANCH', definition.findtext('type'))
        self.assertEqual('main', definition.findtext('defaultValue'))
        self.assertTrue(re.fullmatch(definition.findtext('useRepository'), MODULE['REPOSITORY']))
        self.assertEqual('feature/team/change', re.fullmatch(definition.findtext('branchFilter'), 'origin/feature/team/change').group(1))

    def test_checkout_tracks_branch_and_generates_first_changelog(self):
        root = self.configured()
        self.assertEqual('hudson.plugins.git.GitSCM', root.find('scm').get('class'))
        self.assertEqual('*/${BRANCH}', root.findtext('scm/branches/hudson.plugins.git.BranchSpec/name'))
        self.assertEqual('true', root.findtext('scm/extensions/hudson.plugins.git.extensions.impl.FirstBuildChangelog/makeChangelog'))
        self.assertEqual('false', root.findtext('.//hudson.plugins.git.extensions.impl.CloneOption/shallow'))

    def test_preserves_deploy_parameters_history_label_and_other_publishers(self):
        root = self.configured()
        self.assertEqual(MODULE['JOB_DISPLAY_NAME'], root.findtext('displayName'))
        definitions = root.find('.//parameterDefinitions')
        names = [n.findtext('name') for n in definitions]
        self.assertEqual(['BRANCH', 'TARGET', 'SKIP_DEPLOY', 'VERIFY_ONLY'], names)
        verify = next(n for n in definitions if n.findtext('name') == 'VERIFY_ONLY')
        self.assertEqual('false', verify.findtext('defaultValue'))
        self.assertIsNotNone(root.find('publishers/hudson.tasks.JUnitResultArchiver'))
        self.assertIn('GIT_REVISION', root.findtext('.//org.jenkinsci.plugins.buildnamesetter.BuildNameSetter/template'))
        self.assertEqual('false', root.findtext('concurrentBuild'))

    def test_configuration_is_repeatable(self):
        once = CONFIGURE(SOURCE, 'cyt-oa-deploy')
        twice = CONFIGURE(once, 'cyt-oa-deploy')
        self.assertEqual(once, twice)

    def test_repairs_legacy_utf8_text_without_changing_normal_labels(self):
        root = ET.fromstring(SOURCE)
        title = '【测试部署】CYT-OA｜OA 办公系统｜8091'
        root.find('displayName').text = title.encode('utf-8').decode('latin-1')
        parameter = root.find('.//hudson.model.BooleanParameterDefinition')
        description = ET.SubElement(parameter, 'description')
        description.text = '只构建并推送镜像，不重启容器'.encode('utf-8').decode('latin-1')
        configured = ET.fromstring(CONFIGURE(ET.tostring(root), 'cyt-oa-deploy'))
        self.assertEqual(MODULE['JOB_DISPLAY_NAME'], configured.findtext('displayName'))
        self.assertEqual('只构建并推送镜像，不重启容器', configured.findtext('.//hudson.model.BooleanParameterDefinition/description'))

    def test_unknown_build_steps_are_not_silently_overwritten(self):
        root = ET.fromstring(SOURCE)
        ET.SubElement(root.find('builders'), 'custom.build.Step')
        with self.assertRaises(ValueError):
            CONFIGURE(ET.tostring(root), 'cyt-oa-deploy')


if __name__ == '__main__':
    unittest.main()
