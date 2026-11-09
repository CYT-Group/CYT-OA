#!/usr/bin/env bash
set -Eeuo pipefail

# 【只在首次部署时执行】给 cyt-oa-db 灌表结构与演示种子。
# 安全边界：只操作 OA 自己的新库；库里已有任何表就直接跳过，绝不覆盖已有数据。
#
# 用法：bash db-init.sh [快照目录]     默认用脚本同目录的 db/
#
# 为什么用快照：cyt-oa-backend/src/main/resources/db/*.sql 是「增量脚本」，
# 假设 notifications / voices 等一批旧表已经存在，且 oa-schema.sql 末尾的 ALTER
# 是给老库补列用的（新库执行会报 Duplicate column）。全新库直接按 README 顺序跑会缺表。
# 所以这里用两份可复现的初始化文件：
#   structure.sql  = 开发库 mysqldump --no-data 的 114 张表最终结构（不含任何业务数据）
#   seed-data.sql  = 从项目自带 db/*.sql 里抽出的 INSERT/UPDATE（字典/权限/账号/演示数据）

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
SNAPSHOT_DIR="${1:-$SCRIPT_DIR/db}"
COMPOSE_DIR="${COMPOSE_DIR:-/opt/1panel/docker/compose/oa}"

[[ -f "$SNAPSHOT_DIR/structure.sql" && -f "$SNAPSHOT_DIR/seed-data.sql" ]] || {
  echo "ERROR: 缺 $SNAPSHOT_DIR/structure.sql 或 seed-data.sql" >&2; exit 2; }
[[ -f "$COMPOSE_DIR/.env" ]] || { echo "ERROR: 找不到 $COMPOSE_DIR/.env" >&2; exit 2; }

set -a
# shellcheck disable=SC1091
. "$COMPOSE_DIR/.env"
set +a
DB="${MYSQL_DATABASE:-cyt_oa_center}"
ROOT_PWD="${MYSQL_ROOT_PASSWORD:?请在 .env 里配置 MYSQL_ROOT_PASSWORD}"

cd "$COMPOSE_DIR"
echo "==> 启动 OA 专用数据库容器"
docker compose up -d db

echo "==> 等数据库就绪"
state=""
for _ in $(seq 1 60); do
  state="$(docker inspect -f '{{.State.Health.Status}}' cyt-oa-db 2>/dev/null || echo starting)"
  [[ "$state" == "healthy" ]] && break
  sleep 3
done
[[ "$state" == "healthy" ]] || { echo "ERROR: cyt-oa-db 未就绪" >&2; docker logs --tail 30 cyt-oa-db >&2; exit 1; }

count="$(docker exec -e "MYSQL_PWD=$ROOT_PWD" cyt-oa-db mysql -uroot -N -B \
  -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB'")"
if (( count > 0 )); then
  echo "==> 库 $DB 已有 $count 张表，跳过初始化（不覆盖已有数据）"
  exit 0
fi

echo "==> 空库 $DB：导入表结构"
docker exec -e "MYSQL_PWD=$ROOT_PWD" -i cyt-oa-db mysql -uroot --default-character-set=utf8mb4 "$DB" \
  < "$SNAPSHOT_DIR/structure.sql"
echo "==> 导入演示种子（不含开发库业务数据）"
docker exec -e "MYSQL_PWD=$ROOT_PWD" -i cyt-oa-db mysql -uroot --default-character-set=utf8mb4 "$DB" \
  < "$SNAPSHOT_DIR/seed-data.sql"

echo "==> 核对结果"
docker exec -e "MYSQL_PWD=$ROOT_PWD" cyt-oa-db mysql -uroot -t "$DB" -e "
  SELECT COUNT(*) AS tables_count FROM information_schema.tables WHERE table_schema='$DB';
  SELECT (SELECT COUNT(*) FROM sys_user) AS users,
         (SELECT COUNT(*) FROM sys_role) AS roles,
         (SELECT COUNT(*) FROM sys_menu) AS menus,
         (SELECT COUNT(*) FROM sys_dict_data) AS dict_rows,
         (SELECT COUNT(*) FROM hrm_employee) AS employees,
         (SELECT COUNT(*) FROM rhythm_nodes) AS rhythm_nodes;
  SELECT id, username FROM sys_user WHERE username='admin';"
