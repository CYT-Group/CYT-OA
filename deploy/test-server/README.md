# CYT-OA 测试服部署（照 CYT-CS 的模式）

服务器 `192.168.1.182`：`http://192.168.1.182:8090/` 是 Jenkins 发版后台，OA 站点在 `http://192.168.1.182:8091/`。

## 架构

| 容器 | 作用 | 对外端口 |
| --- | --- | --- |
| `cyt-oa-web` | nginx 托管 Vue 静态资源，反代 `/api`、`/admin-api` | `8091` |
| `cyt-oa-api` | Spring Boot 单 JAR（`48081`） | 仅 `127.0.0.1:8092` |
| `cyt-oa-db` | OA 专用 PostgreSQL 17 + 独立数据卷 | 不暴露 |
| `cyt-oa-redis` | OA 专用 Redis + 独立数据卷 | 不暴露 |

镜像推到局域网仓库 `192.168.1.182:5000/cyt-oa-api:latest`、`cyt-oa-web:latest`，编排文件放在
`/opt/1panel/docker/compose/oa/`（1Panel「容器 → 编排」里也能看到）。数据库、缓存都是独立容器，
不碰 CS / Common / Portal 的现有数据。

真实口令只放在服务器的 `/opt/1panel/docker/compose/oa/.env`（`chmod 600`），
仓库里只有 `.env.example`。测试服开账号密码登录（`admin / admin123`），关闭企微通讯录同步与门户 SSO。

**关于数据库初始化**：PostgreSQL 的结构和内置种子统一放在
`cyt-oa-backend/src/main/resources/db/postgresql/migration/`，由 Flyway 从空库初始化并记录版本；
重复执行不会重复导入。`db-init.sh` 启动数据库后调用与日常发版相同的迁移入口。

## 首次部署（一次性）

```bash
# 1) 服务器准备：编排目录 + .env（按 .env.example 填口令）+ 取源码脚本
sudo mkdir -p /opt/1panel/docker/compose/oa /opt/cyt-oa/deploy
sudo cp deploy/test-server/docker-compose.yml /opt/1panel/docker/compose/oa/
sudo cp deploy/test-server/.env.example /opt/1panel/docker/compose/oa/.env   # 改掉 CHANGE_ME
sudo cp deploy/test-server/git-hub.sh /opt/cyt-oa/deploy/git-hub.sh          # 手动下载备用

# 2) 初始化 PostgreSQL（传入当前源码根目录）
bash deploy/test-server/db-init.sh "$PWD"

# 3) 构建推镜像 + 重建容器 + 健康检查
bash deploy/test-server/ci-build.sh
```

## 数据库备份

PostgreSQL 的日常备份与隔离库恢复验证：

```bash
docker exec cyt-oa-db sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" exec pg_dump -h127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > oa-postgresql.dump
# 将备份复制到隔离 PostgreSQL，使用 pg_restore --exit-on-error 验证；不要覆盖运行中的业务库。
```

## 日常发版

任务地址 `http://192.168.1.182:8090/job/cyt-oa-deploy/`，和 CS 的 `cyt-cs-deploy` 并列。
Jenkins → `cyt-oa-deploy` → 选参数 → 立即构建。任务会：原生 Git SCM 检出 → 服务器构建镜像推仓库 →
`docker compose pull && up -d` → 等页面 200 且 `/api/auth/login` 用 admin 登录成功。

| 参数 | 取值 | 说明 |
| --- | --- | --- |
| `BRANCH` | 默认 `main`，远端全部分支 | Git Parameter 实时读取 GitHub 分支，支持筛选与包含 `/` 的分支名 |
| `TARGET` | `all` / `api` / `web` | 构建哪些镜像 |
| `SKIP_DEPLOY` | 勾选 / 不勾选 | 勾选只推镜像、不重建容器（Jenkins 布尔参数传 `true`） |
| `VERIFY_ONLY` | 勾选 / 不勾选 | 勾选只检出和验证版本，不构建、不推送、不部署 |

任务配置通过 Jenkins REST 热更新；首次安装插件若提示需要重启，应先确认执行器和队列均为空。Jenkins 以 root 运行，
Git 使用 Jenkins 内加密保存的 `cyt-oa-github` 凭据，初次配置从 `/opt/jenkins/github_token` 读取已有 PAT；
应用部署配置仍来自 `/opt/1panel/docker/compose/oa/.env`。
源码位于 Jenkins 的 OA 独立工作区，不再另下源码包，确保实际构建版本与 Changes 一致。

## 分支、版本与 Changes

构建历史名称格式为 `#构建号 · 分支 · 提交短号 · 目标`，构建描述同时显示仅验证/仅构建参数。
`Changes` 使用 Git 插件采集的原生提交记录；分支首次构建显示最近一条提交，再次构建同一提交时无新增变更。
变更记录从启用原生 SCM 后开始采集，过去使用压缩包构建的历史没有 Git 变更基线。

每次构建在 `.jenkins-build/<构建号>/` 生成 `build-info.json` 和 `version.txt` 并归档到构建产物，包含完整
分支、提交 SHA、目标与 GitHub 链接。产物按构建号隔离，检出失败时不会误归档上一次构建的版本信息。

管理员配置入口为 `configure-jenkins.py`：保持原任务类型、地址及历史，备份原 XML 后更新 SCM、分支参数、
版本展示和归档器。依赖已安装的 `git`、`git-parameter`、`build-name-setter` 插件，不修改 Jenkins 全局安全设置。

```bash
sudo install -m 0755 deploy/test-server/jenkins-build.sh /opt/cyt-oa/deploy/jenkins-build.sh
sudo install -m 0755 deploy/test-server/db-upgrade.sh /opt/cyt-oa/deploy/db-upgrade.sh
sudo install -m 0755 deploy/test-server/migrate-database.py /opt/cyt-oa/deploy/migrate-database.py
# AUTH_FILE 是服务器临时凭据 JSON（user/password），限制为管理员可读，完成后删除。
sudo python3 deploy/test-server/configure-jenkins.py --auth-file "$AUTH_FILE" --apply
python3 -B -m unittest discover -s deploy/test-server/tests -v
```

`git-hub.sh` 仍保留给无 Git 环境的手动下载，不再作为这个 Jenkins 任务的检出步骤。

测试站点访问地址保留在任务描述首行，任务名称为 `【测试环境】CYT-OA`。

`db-init.sh` 与日常发版共用迁移入口；已登记的版本不会重复执行。新增 SQL 一律放入
`cyt-oa-backend/src/main/resources/db/postgresql/migration/`，遵循 [数据库变更规范](../../cyt-oa-backend/src/main/resources/db/postgresql/migration/README.md)。
`db-upgrade.sh` 使用固定版本 Flyway 容器，记录 `flyway_schema_history` 并校验历史文件。
默认镜像 `flyway/flyway:11.20.2-alpine`，首次需缓存到服务器；国内网络可从已有镜像源拉取后打回同名标签。
Jenkins 先构建推镜像，再执行迁移，成功后更新应用；手动 `redeploy.sh` 也会先迁移。
仅验证或仅构建镜像不升级数据库。全新 PostgreSQL 直接 `migrate`；已有部署持续校验并执行尚未应用的版本。

测试服的 GitHub 直连不稳定，原有代理对 GitHub 也走直连。已为 Jenkins 单独部署
`jenkins-git-proxy` 容器，仅在 `127.0.0.1:17890` 提供代理，使用现有订阅节点自动测速，
仅允许 GitHub 相关域名。配置及订阅缓存位于 `/opt/jenkins/git-proxy/`，包含私密订阅信息，不入库。
容器设置 `unless-stopped` 自动启动，没有修改其他服务的代理规则。root 的 Git 配置仅对 OA 仓库启用它：

```bash
sudo git config --global http.https://github.com/CYT-Group/CYT-OA.git.proxy http://127.0.0.1:17890
sudo docker logs --tail 30 jenkins-git-proxy
```

如果分支下拉列表报错或检出超时，先检查此容器及订阅节点可用性；不要把错误回退的默认 `main` 当成分支查询成功。

手动跑也一样（在源码根目录）：`bash deploy/test-server/ci-build.sh`；只发一端用
`TARGET=api bash deploy/test-server/ci-build.sh`。

## 企微审批模板与网关（含穿透访问）

穿透到测试服 `8091` 后，请求由 nginx 转发到 `cyt-oa-api`，使用 `test` profile；
不会读取开发电脑的 `application-local.yml`。本地可以加载模板而测试服提示
「未配置企微网关地址 cyt.wecom.gateway-url」，说明测试服后端没有收到网关地址，尚未发起网关请求。
旧编排将 `CYT_WECOM_GATEWAY_URL` 固定为空，且未传入 API Key，只改 `.env` 不会生效。

在服务器 `/opt/1panel/docker/compose/oa/.env` 中补充以下配置，真实密钥不要提交到仓库：

```dotenv
# 当前测试服 Common Java 发布在 8088；其他部署替换为容器可达的网关地址。
CYT_WECOM_GATEWAY_URL=http://192.168.1.182:8088
CYT_WECOM_API_KEY=替换为网关后台为OA签发的APIKey
CYT_WECOM_SYSTEM_ID=oa
```

容器里的 `127.0.0.1` 指向 OA 容器自身，不能直接照搬本机的 `http://127.0.0.1:8189`。
网关地址与浏览器使用的 OA 穿透地址是两个地址；应以 OA 容器能否访问 Common 为准。
网关需给这把 Key 授予审批模板相关操作权限；账号密码登录、定时同步和门户 SSO 开关与模板读取独立。

将本次编排文件同步到服务器后应用配置（在服务器源码根目录执行）：

```bash
# redeploy.sh 只在首次部署时复制编排；已有部署需要显式更新此文件，保留现有 .env。
sudo cp deploy/test-server/docker-compose.yml /opt/1panel/docker/compose/oa/docker-compose.yml
cd /opt/1panel/docker/compose/oa
sudo docker compose config --quiet
sudo docker compose up -d --no-deps --force-recreate api web
```

仅修改环境变量无需重建镜像；`docker compose restart` 不会把新的 `.env` 注入已有容器。
可检查配置是否注入，以下命令只显示地址与密钥是否非空：

```bash
sudo docker compose exec -T api bash -c '[[ -n "$CYT_WECOM_GATEWAY_URL" ]] && echo "gateway-url: configured" || echo "gateway-url: empty"; [[ -n "$CYT_WECOM_API_KEY" ]] && echo "api-key: configured" || echo "api-key: empty"'
```

最后在审批模板页点击「重试」验证列表读取。若改为连接失败或超时，再检查网关的监听地址、端口与容器网络；
若返回 HTTP 401/403，再核对网关 API Key 及其操作授权。

2026-11-11 已在测试服应用：Common Java 注册独立调用方 `oa`，签发 OA 专用 API Key，
按本地已有权限配置模板列表、详情、绑定、新建、更新及审批单详情操作；OA 应用凭据加密保存在网关，
OA 后端仅持有网关调用密钥。同步本地已有的 `test`、`tplidem` 两个模板绑定，未调用企微模板新建或更新接口。
配置修改前的备份位于服务器 `/opt/1panel/docker/compose/oa/backup-wecom-20261111T083401Z/`。
重建 API 容器并重载 nginx 后，已通过 `https://cyt-oatest.lrbelec.top` 验证模板列表及两个模板详情均为 HTTP 200、业务码 0，
网关使用真实模式（`mock=false`）。

## 排障

```bash
curl -I http://127.0.0.1:8091/                                # 前端
curl -X POST http://127.0.0.1:8092/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","device":"admin"}'   # 后端 + 数据库 + Redis
docker logs --tail 100 cyt-oa-api
docker ps --format '{{.Names}}\t{{.Status}}' | grep cyt-oa
```
