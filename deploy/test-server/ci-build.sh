#!/usr/bin/env bash
set -Eeuo pipefail

# Jenkins「cyt-oa-deploy」构建步骤总入口；Jenkins 原生 Git 检出到工作区。
#   SKIP_DEPLOY=1 / true 只构建推镜像、不重启容器（Jenkins 布尔参数传的是 true/false）。

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

echo "==> 工作区: $ROOT"
if [[ -f "$ROOT/.deploy-branch" ]]; then
  echo "==> 发布分支: $(cat "$ROOT/.deploy-branch")"
fi
if [[ -f "$ROOT/.deploy-sha" ]]; then
  echo "==> 源码提交: $(cut -c1-12 "$ROOT/.deploy-sha")"
else
  echo "==> 分支: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '(非 git)')  提交: $(git rev-parse --short HEAD 2>/dev/null || echo '-')"
fi

TARGET="${TARGET:-all}"
echo "==> 1/2 构建并推送镜像（$TARGET）"
bash "$ROOT/deploy/test-server/build-on-server.sh" "$ROOT" "$TARGET"

case "${SKIP_DEPLOY:-0}" in
  1|true|TRUE|yes|YES)
    echo "==> SKIP_DEPLOY=${SKIP_DEPLOY}，跳过重启，容器仍在跑旧镜像"
    exit 0
    ;;
esac

echo "==> 2/2 重建容器 + 健康检查"
bash "$ROOT/deploy/test-server/redeploy.sh" "$ROOT"

echo "✅ 部署完成：http://192.168.1.182:8091/"
