#!/usr/bin/env bash
set -Eeuo pipefail

# 【在测试服上执行】拉最新镜像 → 重建容器 → 等页面和登录接口都通。
# 用法：bash redeploy.sh <源码根目录>     首次会自动把编排文件放到 1Panel 的 compose 目录。
# 可选：SKIP_PULL=1 / WAIT_SECONDS=180

ROOT="${1:-$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)}"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/1panel/docker/compose/oa}"
WEB_PORT="${OA_WEB_PORT:-8091}"
API_PORT="${OA_API_PORT:-8092}"
WAIT_SECONDS="${WAIT_SECONDS:-180}"

if [[ ! -f "$COMPOSE_DIR/docker-compose.yml" ]]; then
  echo "==> 首次部署：安装编排文件到 $COMPOSE_DIR"
  mkdir -p "$COMPOSE_DIR"
  cp "$ROOT/deploy/test-server/docker-compose.yml" "$COMPOSE_DIR/docker-compose.yml"
fi
[[ -f "$COMPOSE_DIR/.env" ]] || { echo "ERROR: 缺 $COMPOSE_DIR/.env（按 .env.example 填真实口令）" >&2; exit 2; }

cd "$COMPOSE_DIR"
set -a
# shellcheck disable=SC1091
. "$COMPOSE_DIR/.env"
set +a
WEB_PORT="${OA_WEB_PORT:-8091}"
API_PORT="${OA_API_PORT:-8092}"

if [[ "${OA_MIGRATIONS_APPLIED:-0}" != 1 ]]; then
  bash "$ROOT/deploy/test-server/db-upgrade.sh" "$ROOT"
fi

if [[ "${SKIP_PULL:-0}" != "1" ]]; then
  echo "==> 拉取最新镜像"
  docker compose pull api web 2>&1 | tail -6
fi

echo "==> 重建容器"
docker compose up -d 2>&1 | tail -10

echo "==> 等健康检查（最多 ${WAIT_SECONDS}s）"
deadline=$(( $(date +%s) + WAIT_SECONDS ))
login='{"username":"admin","password":"admin123","device":"admin"}'
while :; do
  code="$(curl -s -o /dev/null -w '%{http_code}' -m 5 "http://127.0.0.1:$WEB_PORT/" || true)"
  auth="$(curl -s -m 8 -X POST "http://127.0.0.1:$API_PORT/api/auth/login" \
          -H 'Content-Type: application/json' -d "$login" || true)"
  if [[ "$code" == "200" && "$auth" == *'"code":0'* ]]; then
    echo "✅ 页面 200 / 登录接口成功：${auth:0:200}"
    docker ps --format '{{.Names}}\t{{.Status}}' | grep cyt-oa || true
    echo "页面: http://192.168.1.182:$WEB_PORT/   账号: admin / admin123"
    exit 0
  fi
  if (( $(date +%s) > deadline )); then
    echo "❌ 超时：页面=$code 登录=${auth:0:200}" >&2
    echo "--- api 最近日志 ---" >&2
    docker logs --tail 40 cyt-oa-api >&2 || true
    exit 1
  fi
  sleep 5
done
