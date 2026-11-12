#!/usr/bin/env bash
set -Eeuo pipefail

# Git 检出由 Jenkins SCM 完成；不再下载另一份源码，确保 Changes 对应实际构建版本。
ROOT="${WORKSPACE:?必须由 Jenkins 提供 WORKSPACE}"
cd "$ROOT"
SHA="$(git rev-parse --verify HEAD)"
BRANCH="${BRANCH:?请选择发布分支}"
TARGET="${TARGET:-all}"
case "$TARGET" in all|api|web) ;; *) echo 'ERROR: 无效的构建目标' >&2; exit 2 ;; esac
git check-ref-format "refs/heads/$BRANCH" >/dev/null
if [[ -n "${GIT_COMMIT:-}" && "$SHA" != "$GIT_COMMIT" ]]; then
  echo 'ERROR: 工作区提交与 Jenkins 检出记录不一致，停止构建' >&2
  exit 3
fi

export SHA BRANCH TARGET
printf '%s\n' "$SHA" > .deploy-sha
printf '%s\n' "$BRANCH" > .deploy-branch
mkdir -p .jenkins-build
python3 - <<'PY'
import json, os
from pathlib import Path
from urllib.parse import quote
branch, sha = os.environ['BRANCH'], os.environ['SHA']
metadata = {
    'build': os.environ.get('BUILD_NUMBER', ''),
    'branch': branch,
    'commit': sha,
    'target': os.environ['TARGET'],
    'verifyOnly': os.environ.get('VERIFY_ONLY', '').lower() in ('1', 'true'),
    'skipDeploy': os.environ.get('SKIP_DEPLOY', '').lower() in ('1', 'true'),
    'branchUrl': 'https://github.com/CYT-Group/CYT-OA/tree/' + quote(branch, safe=''),
    'commitUrl': 'https://github.com/CYT-Group/CYT-OA/commit/' + sha,
}
build_number = os.environ.get('BUILD_NUMBER', '')
if not build_number.isdecimal():
    raise SystemExit('BUILD_NUMBER 必须是数字')
output = Path('.jenkins-build') / build_number
output.mkdir(parents=True, exist_ok=True)
output.joinpath('build-info.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
output.joinpath('version.txt').write_text(
    f"分支: {branch}\n提交: {sha}\n目标: {metadata['target']}\n仅验证: {metadata['verifyOnly']}\n仅构建: {metadata['skipDeploy']}\n",
    encoding='utf-8')
PY

echo "==> 构建版本：$BRANCH @ ${SHA:0:12}；目标：$TARGET"
git log -1 --format='提交：%h%n作者：%an%n时间：%cI%n说明：%s'
case "${VERIFY_ONLY:-false}" in
  true|TRUE|1)
    echo '==> 仅验证检出、分支、版本与 Changes；未构建镜像、未推送、未部署。'
    exit 0
    ;;
esac

# 服务器入口控制阶段顺序，旧分支也遵守：构建 → 迁移 → 更新应用。
TARGET="$TARGET" SKIP_DEPLOY=true bash deploy/test-server/ci-build.sh
case "${SKIP_DEPLOY:-false}" in
  true|TRUE|1|yes|YES) exit 0 ;;
esac
TOOLS_DIR="${OA_DEPLOY_TOOLS_DIR:-/opt/cyt-oa/deploy}"
bash "$TOOLS_DIR/db-upgrade.sh" "$ROOT"
OA_MIGRATIONS_APPLIED=1 bash deploy/test-server/redeploy.sh "$ROOT"
