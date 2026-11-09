# CYT-OA 测试服部署（照 CYT-CS 的模式）

服务器 `192.168.1.182`：`http://192.168.1.182:8090/` 是 Jenkins 发版后台，OA 站点在 `http://192.168.1.182:8091/`。

## 架构

| 容器 | 作用 | 对外端口 |
| --- | --- | --- |
| `cyt-oa-web` | nginx 托管 Vue 静态资源，反代 `/api`、`/admin-api` | `8091` |
| `cyt-oa-api` | Spring Boot 单 JAR（`48081`） | 仅 `127.0.0.1:8092` |
| `cyt-oa-db` | OA 专用 MySQL 8 + 独立数据卷 | 不暴露 |
| `cyt-oa-redis` | OA 专用 Redis + 独立数据卷 | 不暴露 |

镜像推到局域网仓库 `192.168.1.182:5000/cyt-oa-api:latest`、`cyt-oa-web:latest`，编排文件放在
`/opt/1panel/docker/compose/oa/`（1Panel「容器 → 编排」里也能看到）。数据库、缓存都是独立容器，
不碰 CS / Common / Portal 的现有数据。

真实口令只放在服务器的 `/opt/1panel/docker/compose/oa/.env`（`chmod 600`），
仓库里只有 `.env.example`。测试服开账号密码登录（`admin / admin123`），关闭企微通讯录同步与门户 SSO。

**关于数据库初始化**：`cyt-oa-backend/src/main/resources/db/*.sql` 是增量脚本，假设通知、发声等一批旧表已存在，
`oa-schema.sql` 末尾的 ALTER 又是给老库补列用的（新库会报 `Duplicate column`），直接按 README 顺序跑建不出完整库。
所以部署目录里放了两份可复现的初始化文件：`db/structure.sql`（开发库 `mysqldump --no-data` 的 114 张表最终结构，
不含任何业务数据）+ `db/seed-data.sql`（从项目自带脚本抽出的 INSERT/UPDATE：字典、权限、账号、演示数据）。
`db-init.sh` 只在空库时导入这两份，且不含开发库的 195 条员工档案。

## 首次部署（一次性）

```bash
# 1) 服务器准备：编排目录 + .env（按 .env.example 填口令）+ 取源码脚本
sudo mkdir -p /opt/1panel/docker/compose/oa /opt/cyt-oa/deploy /opt/cyt-oa/db
sudo cp deploy/test-server/docker-compose.yml /opt/1panel/docker/compose/oa/
sudo cp deploy/test-server/.env.example /opt/1panel/docker/compose/oa/.env   # 改掉 CHANGE_ME
sudo cp deploy/test-server/git-hub.sh /opt/cyt-oa/deploy/git-hub.sh          # Jenkins 拉源码用
sudo cp deploy/test-server/db/*.sql /opt/cyt-oa/db/

# 2) 初始化数据库（库里已有表就自动跳过，绝不覆盖）
bash deploy/test-server/db-init.sh /opt/cyt-oa/db

# 3) 构建推镜像 + 重建容器 + 健康检查
bash deploy/test-server/ci-build.sh
```

## 日常发版

任务地址 `http://192.168.1.182:8090/job/cyt-oa-deploy/`，和 CS 的 `cyt-cs-deploy` 并列。
Jenkins → `cyt-oa-deploy` → 选参数 → 立即构建。任务会：从 GitHub 取源码 → 服务器构建镜像推仓库 →
`docker compose pull && up -d` → 等页面 200 且 `/api/auth/login` 用 admin 登录成功。

| 参数 | 取值 | 说明 |
| --- | --- | --- |
| `BRANCH` | `main` | `git-hub.sh` 走 GitHub REST + codeload 取源码，服务器不留 `.git` |
| `TARGET` | `all` / `api` / `web` | 构建哪些镜像 |
| `SKIP_DEPLOY` | 勾选 / 不勾选 | 勾选只推镜像、不重建容器（Jenkins 布尔参数传 `true`） |

任务通过 Jenkins REST 热创建，不需要重启 Jenkins（重启会打断别的发版）。Jenkins 以 root 运行，
读 `/opt/jenkins/github_token`（GitHub PAT）与 `/opt/1panel/docker/compose/oa/.env`；
每次构建临时源码放在 `/tmp/cyt-oa-build-$$`，结束后自动删除。

手动跑也一样（在源码根目录）：`bash deploy/test-server/ci-build.sh`；只发一端用
`TARGET=api bash deploy/test-server/ci-build.sh`。

## 排障

```bash
curl -I http://127.0.0.1:8091/                                # 前端
curl -X POST http://127.0.0.1:8092/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","device":"admin"}'   # 后端 + 数据库 + Redis
docker logs --tail 100 cyt-oa-api
docker ps --format '{{.Names}}\t{{.Status}}' | grep cyt-oa
```
