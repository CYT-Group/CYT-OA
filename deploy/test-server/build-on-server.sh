#!/usr/bin/env bash
set -Eeuo pipefail

# 在【测试服】构建 CYT-OA 镜像并推到局域网仓库（照 CYT-CS 的做法：服务器上有 docker，开发机不用装）。
# 用法：bash build-on-server.sh <源码根目录> [all|api|web]
# 可选：REGISTRY / BASE_REGISTRY / NPM_REGISTRY / NO_PUSH=1

ROOT="${1:-$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)}"
TARGET="${2:-all}"
REGISTRY="${REGISTRY:-192.168.1.182:5000}"
BASE_REGISTRY="${BASE_REGISTRY:-docker.m.daocloud.io/}"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}"
API_IMAGE="${OA_API_IMAGE:-$REGISTRY/cyt-oa-api:latest}"
WEB_IMAGE="${OA_WEB_IMAGE:-$REGISTRY/cyt-oa-web:latest}"

case "$TARGET" in all|api|web) ;; *) echo "ERROR: 目标只能是 all / api / web" >&2; exit 2 ;; esac

command -v docker >/dev/null || { echo "ERROR: 服务器上没有 docker" >&2; exit 2; }
docker info >/dev/null || { echo "ERROR: docker 守护进程不可用" >&2; exit 2; }

echo "源码目录: $ROOT | 目标: $TARGET | 仓库: $REGISTRY"

if [[ "$TARGET" == "all" || "$TARGET" == "api" ]]; then
  echo "==> 构建后端 $API_IMAGE"
  docker build --build-arg "BASE_REGISTRY=$BASE_REGISTRY" \
    -f "$ROOT/deploy/test-server/Dockerfile.api" -t "$API_IMAGE" "$ROOT"
fi
if [[ "$TARGET" == "all" || "$TARGET" == "web" ]]; then
  echo "==> 构建前端 $WEB_IMAGE"
  docker build --build-arg "NPM_REGISTRY=$NPM_REGISTRY" \
    -f "$ROOT/deploy/test-server/Dockerfile.web" -t "$WEB_IMAGE" "$ROOT"
fi

if [[ "${NO_PUSH:-0}" == "1" ]]; then
  echo "NO_PUSH=1，跳过推送。"; exit 0
fi

[[ "$TARGET" == "all" || "$TARGET" == "api" ]] && docker push "$API_IMAGE"
[[ "$TARGET" == "all" || "$TARGET" == "web" ]] && docker push "$WEB_IMAGE"
echo "==> 镜像已推送：$API_IMAGE / $WEB_IMAGE"
