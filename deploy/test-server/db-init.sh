#!/usr/bin/env bash
set -Eeuo pipefail

# 首次部署：启动 PostgreSQL，在空库执行版本化结构和种子迁移。
# 用法：bash db-init.sh [源码根目录]；默认使用当前脚本所属源码。
# 成功迁移由 Flyway 记账，重复执行不会重复导入种子。

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="${1:-$(cd -- "$SCRIPT_DIR/../.." && pwd)}"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/1panel/docker/compose/oa}"

[[ -f "$COMPOSE_DIR/.env" ]] || { echo "ERROR: 找不到 $COMPOSE_DIR/.env" >&2; exit 2; }
[[ -d "$ROOT/cyt-oa-backend/src/main/resources/db/postgresql/migration" ]] || {
  echo "ERROR: 缺少 PostgreSQL 迁移目录，请传入当前源码根目录" >&2; exit 2; }

cd "$COMPOSE_DIR"
echo "==> 启动 OA 专用 PostgreSQL 容器"
docker compose up -d db

echo "==> 等数据库就绪"
state=""
for _ in $(seq 1 60); do
  state="$(docker inspect -f '{{.State.Health.Status}}' cyt-oa-db 2>/dev/null || echo starting)"
  [[ "$state" == "healthy" ]] && break
  sleep 3
done
[[ "$state" == "healthy" ]] || { echo "ERROR: cyt-oa-db 未就绪" >&2; docker logs --tail 30 cyt-oa-db >&2; exit 1; }

echo "==> 校验并执行 PostgreSQL 结构和初始种子迁移"
bash "$SCRIPT_DIR/db-upgrade.sh" "$ROOT" migrate
echo "==> 初始化完成；后续发版继续使用相同 Flyway 迁移入口"
