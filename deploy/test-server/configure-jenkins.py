#!/usr/bin/env python3
"""配置原生 SCM、动态分支及版本展示；凭据只在 Jenkins 内从服务器文件读取。"""
import argparse
import base64
import http.cookiejar
import json
from pathlib import Path
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET


REPOSITORY = 'https://github.com/CYT-Group/CYT-OA.git'
SITE_URL = 'http://192.168.1.182:8091/'
JOB_DISPLAY_NAME = '【测试环境】CYT-OA'
CREDENTIAL_ID = 'cyt-oa-github'
PLUGINS = ('git', 'git-parameter', 'build-name-setter')


class Jenkins:
    def __init__(self, base, auth_file):
        self.base = base.rstrip('/')
        auth = json.loads(Path(auth_file).read_text(encoding='utf-8'))
        self.authorization = 'Basic ' + base64.b64encode((auth['user'] + ':' + auth['password']).encode()).decode()
        self.opener = urllib.request.build_opener(
            urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.crumb = {}

    def request(self, path, data=None, content_type='application/json'):
        headers = {'Authorization': self.authorization, 'Content-Type': content_type}
        if data is not None:
            if not self.crumb:
                issuer = self.json('/crumbIssuer/api/json')
                self.crumb = {issuer['crumbRequestField']: issuer['crumb']}
            headers.update(self.crumb)
        with self.opener.open(urllib.request.Request(self.base + path, data=data, headers=headers), timeout=60) as response:
            return response.read()

    def json(self, path):
        return json.loads(self.request(path))

    def groovy(self, script):
        return self.request('/scriptText', urllib.parse.urlencode({'script': script}).encode(), 'application/x-www-form-urlencoded').decode()


def child(parent, name, text=None, **attributes):
    element = ET.SubElement(parent, name, attributes)
    if text is not None:
        element.text = text
    return element


def replace(parent, name, element):
    for old in parent.findall(name):
        parent.remove(old)
    parent.append(element)


def job_config(original, job):
    root = ET.fromstring(original)
    if root.tag != 'project':
        raise ValueError('仅支持现有 Freestyle 任务，不转换任务类型或历史')
    # 旧配置曾按 Latin-1 读取 UTF-8；仅修复可严格还原为中文的名称与说明。
    for element in [root.find('displayName'), *root.findall('.//description')]:
        if element is None or not element.text:
            continue
        try:
            repaired = element.text.encode('latin-1').decode('utf-8')
        except (UnicodeEncodeError, UnicodeDecodeError):
            continue
        if any('\u4e00' <= char <= '\u9fff' for char in repaired):
            element.text = repaired
    if job == 'cyt-oa-deploy':
        display_name = root.find('displayName')
        if display_name is None:
            display_name = child(root, 'displayName')
        display_name.text = JOB_DISPLAY_NAME
    root.find('description').text = (
        'OA 测试环境访问地址：' + SITE_URL + '\n'
        '访问方式：浏览器打开以上地址；需连接公司内网或可访问该内网的 VPN。\n'
        'Jenkins 是发版后台；日常使用 OA 请访问上述 8091 地址。\n\n'
        '用途：构建并部署 OA 办公系统。\n'
        '原生 Git 检出并记录 Changes；BRANCH 实时读取 GitHub 分支，支持搜索。\n'
        '构建历史显示分支、提交短号与目标；每次构建归档完整版本信息。\n'
        'VERIFY_ONLY=true 仅验证检出，不构建、不推送、不部署。\n'
        'SKIP_DEPLOY=true 只构建推送，不更新容器。')
    properties = root.find('properties')
    definitions = properties.find('hudson.model.ParametersDefinitionProperty/parameterDefinitions')
    if definitions is None:
        raise ValueError('缺少现有任务参数，未进行覆盖')
    for item in list(definitions):
        if item.findtext('name') in ('BRANCH', 'VERIFY_ONLY'):
            definitions.remove(item)
    branch = ET.Element('net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition')
    for name, value in {
        'name': 'BRANCH', 'description': '发布分支：实时读取远端分支，main 默认选中，可输入关键词筛选',
        'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, REPOSITORY + '#' + job + ':BRANCH')),
        'type': 'PT_BRANCH', 'branch': '', 'tagFilter': '*', 'branchFilter': 'origin/(.*)',
        'sortMode': 'ASCENDING_SMART', 'defaultValue': 'main', 'selectedValue': 'DEFAULT',
        'useRepository': r'https://github\.com/CYT-Group/CYT-OA\.git',
        'quickFilterEnabled': 'true', 'listSize': '10', 'requiredParameter': 'true',
    }.items():
        child(branch, name, value)
    definitions.insert(0, branch)
    verify = child(definitions, 'hudson.model.BooleanParameterDefinition')
    child(verify, 'name', 'VERIFY_ONLY')
    child(verify, 'description', '仅检出并验证分支、提交和 Changes；不构建镜像、不推送、不部署')
    child(verify, 'defaultValue', 'false')
    scm = ET.Element('scm', {'class': 'hudson.plugins.git.GitSCM'})
    child(scm, 'configVersion', '2')
    remote = child(child(scm, 'userRemoteConfigs'), 'hudson.plugins.git.UserRemoteConfig')
    child(remote, 'url', REPOSITORY)
    child(remote, 'credentialsId', CREDENTIAL_ID)
    child(remote, 'refspec', '+refs/heads/*:refs/remotes/origin/*')
    spec = child(child(scm, 'branches'), 'hudson.plugins.git.BranchSpec')
    child(spec, 'name', '*/${BRANCH}')
    child(scm, 'doGenerateSubmoduleConfigurations', 'false')
    child(scm, 'submoduleCfg', **{'class': 'empty-list'})
    extensions = child(scm, 'extensions')
    first = child(extensions, 'hudson.plugins.git.extensions.impl.FirstBuildChangelog')
    child(first, 'makeChangelog', 'true')
    local = child(extensions, 'hudson.plugins.git.extensions.impl.LocalBranch')
    child(local, 'localBranch', '${BRANCH}')
    clone = child(extensions, 'hudson.plugins.git.extensions.impl.CloneOption')
    for name, value in {'shallow': 'false', 'noTags': 'true', 'reference': '', 'timeout': '10', 'honorRefspec': 'true'}.items():
        child(clone, name, value)
    replace(root, 'scm', scm)
    root.find('concurrentBuild').text = 'false'
    builders = root.find('builders')
    if len(builders) != 1 or builders[0].tag != 'hudson.tasks.Shell':
        raise ValueError('任务构建步骤与已确认的配置不同，未覆盖其他步骤')
    builders.clear()
    shell = child(builders, 'hudson.tasks.Shell')
    child(shell, 'command', '#!/usr/bin/env bash\nset -Eeuo pipefail\nbash /opt/cyt-oa/deploy/jenkins-build.sh\n')
    publishers = root.find('publishers')
    for old in publishers.findall('hudson.tasks.ArtifactArchiver'):
        publishers.remove(old)
    archiver = child(publishers, 'hudson.tasks.ArtifactArchiver')
    for name, value in {'artifacts': '.jenkins-build/${BUILD_NUMBER}/*', 'allowEmptyArchive': 'true', 'onlyIfSuccessful': 'false', 'fingerprint': 'true', 'defaultExcludes': 'false'}.items():
        child(archiver, name, value)
    wrappers = root.find('buildWrappers')
    for old in wrappers.findall('org.jenkinsci.plugins.buildnamesetter.BuildNameSetter'):
        wrappers.remove(old)
    labels = child(wrappers, 'org.jenkinsci.plugins.buildnamesetter.BuildNameSetter')
    child(labels, 'template', '#${BUILD_NUMBER} · ${ENV,var="BRANCH"} · ${GIT_REVISION,length=8} · ${ENV,var="TARGET"}')
    child(labels, 'descriptionTemplate', '分支: ${ENV,var="BRANCH"} | 提交: ${GIT_REVISION,length=12} | 目标: ${ENV,var="TARGET"} | 仅验证: ${ENV,var="VERIFY_ONLY"} | 仅构建: ${ENV,var="SKIP_DEPLOY"}')
    child(labels, 'runAtStart', 'true')
    child(labels, 'runAtEnd', 'true')
    return ET.tostring(root, encoding='utf-8', xml_declaration=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8090')
    parser.add_argument('--job', default='cyt-oa-deploy')
    parser.add_argument('--auth-file', required=True, help='服务器临时 JSON：user/password；不放进仓库')
    parser.add_argument('--backup-dir', default='/opt/jenkins/job-label-backups')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    client = Jenkins(args.url, args.auth_file)
    active = {p['shortName'] for p in client.json('/pluginManager/api/json?tree=plugins[shortName,active]')['plugins'] if p['active']}
    missing = set(PLUGINS) - active
    if missing:
        raise SystemExit('先安装并加载 Jenkins 插件：' + ', '.join(sorted(missing)))
    job_path = '/job/' + urllib.parse.quote(args.job, safe='')
    original = client.request(job_path + '/config.xml')
    updated = job_config(original, args.job)
    if not args.apply:
        print(updated.decode())
        return
    job = client.json(job_path + '/api/json?tree=builds[number,building]')
    if any(b['building'] for b in job.get('builds', [])):
        raise SystemExit('任务正在构建，未更新配置')
    from datetime import datetime
    backup = Path(args.backup_dir) / (args.job + '-' + datetime.now().strftime('%Y%m%d-%H%M%S') + '.xml')
    backup.parent.mkdir(parents=True, exist_ok=True)
    backup.write_bytes(original)
    # 在 Jenkins 进程内读取已有 PAT 并按 Secret 格式加密保存；不把明文发回客户端或构建日志。
    credential_script = '''
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
def provider = SystemCredentialsProvider.getInstance()
def existing = provider.credentials.find { it.id == 'cyt-oa-github' }
if (existing == null) {
  def token = new File('/opt/jenkins/github_token').text.trim()
  if (!token) throw new IllegalStateException('GitHub token file is empty')
  provider.credentials.add(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
    'cyt-oa-github', 'CYT-OA repository read access', 'x-access-token', token))
  provider.save()
}
println('OA Git credential ready')
'''
    response = client.groovy(credential_script)
    if 'OA Git credential ready' not in response or 'Exception' in response:
        raise SystemExit('Jenkins Git 凭据配置失败；原任务尚未变更')
    if client.request(job_path + '/config.xml') != original:
        raise SystemExit('任务配置被其他人修改，请重新检查后再应用')
    client.request(job_path + '/config.xml', updated, 'application/xml; charset=UTF-8')
    print('已更新任务：' + args.job)
    print('原配置备份：' + str(backup))


if __name__ == '__main__':
    main()
