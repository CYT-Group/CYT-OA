# 配置说明

[返回项目首页](../README.md)

配置文件均位于 `src/main/resources/`，按用途维护：

- `application.yml`：公共框架设置、业务默认值和环境变量映射，不填写实际服务器地址或凭据。
- `application-local.yml`：本地 PostgreSQL / Redis 连接、开发登录开关、本机网关、跨域和 debug 日志。
- `application-prod.properties`：线上 PostgreSQL / Redis 连接、网关凭据、域名、SSO、生产开关和 info 日志。

启动时先读取公共配置，再叠加当前 profile 的配置。YAML 与 properties 只是格式不同，
不会决定环境；环境由 `SPRING_PROFILES_ACTIVE` 或 `--spring.profiles.active` 选择，默认 `local`。
驱动、JSON 时区、缓存策略、MyBatis、Warm-Flow、令牌机制和同步时间等公共项不在环境文件中重复维护。
两个环境文件中的真实地址和凭据均被 Git 忽略，新环境需自行准备所需文件。

## 生产环境 properties

在 `application-prod.properties` 中替换 `CHANGE_ME` 占位值，并按需填写 Redis、网关、
允许跨域来源和门户 SSO 配置；也可通过每项对应的环境变量注入。修改后重新打包：

```bash
cd cyt-oa-backend
# 先准备并填写 src/main/resources/application-prod.properties
mvn -DskipTests package
java -jar target/cyt-oa-backend-1.0.0.jar --spring.profiles.active=prod
```

配置随 Maven / Docker 构建打进 JAR，启动时指定 `--spring.profiles.active=prod` 或环境变量
`SPRING_PROFILES_ACTIVE=prod` 即可加载，覆盖公共配置中的数据库连接；`prod` 不加载本机的
`application-local.yml`。真实文件已被 Git 忽略；若通过 Git 检出后在 CI 中构建，
需在构建前将真实配置写入上述资源路径。修改文件中的生产配置后需要重新打包。

生产默认关闭密码登录与开发身份，开启 HTTPS Cookie；本地保留开发登录与 HTTP Cookie。
SSO 默认关闭，填写完整地址后显式启用。数据库驱动和公共协议配置继承 `application.yml`。
现有测试服使用 `test` profile，不加载 local / prod 文件，通过公共配置中的环境变量映射接收部署参数。

## 数据库

数据库使用 PostgreSQL 17+，JDBC 驱动为 `org.postgresql.Driver`。

- `CYT_PG_HOST` / `CYT_PG_PORT` / `CYT_PG_DATABASE`：本地默认 `127.0.0.1:5432/cyt_oa_center`，生产填写线上连接。
- `CYT_PG_USERNAME`：本地默认 `cyt_oa`，生产填写线上账号。
- `CYT_PG_PASSWORD`：在对应环境文件或环境变量中提供。
- Spring 标准 `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` 可覆盖完整连接配置。

连接显式设置会话时区为 `Asia/Shanghai`，业务 `LocalDateTime` 保持北京时间。
JDBC 使用 `stringtype=unspecified`，由目标列推断表单日期等字符串参数的类型；覆盖完整 URL 时保留此项。
状态和逻辑删除列保留 `SMALLINT` 0/1；MyBatis 与 JDBC 参数绑定统一转换布尔值。
结构化 JSON 以 `TEXT` 存储，保留现有 Java 字符串序列化约定。

建表与升级统一使用 `src/main/resources/db/postgresql/migration/`，由部署入口的 Flyway
执行；应用启动本身不执行迁移。初始化方式见[项目首页](../README.md#3-建表与初始数据)。
PostgreSQL 的普通 `LIKE` 搜索默认区分大小写；账号和企微 userid 另有忽略大小写的唯一索引，防止重复身份。
Portal 搬迁脚本同样按当前 profile 读取 YAML / properties；执行生产搬迁时指定 `SPRING_PROFILES_ACTIVE=prod`。

## Redis 与缓存

`spring.data.redis.*` 走 `CYT_REDIS_HOST` / `CYT_REDIS_PORT` / `CYT_REDIS_DATABASE` / `CYT_REDIS_PASSWORD`。
本地默认 `127.0.0.1:6379` 的 1 号库，生产在 properties 中填写线上连接。连接工厂由 Redisson 提供，
Lettuce 已在依赖里排除；Spring Data Redis 的 Repository 扫描关闭。

进程内缓存用 Caffeine，`expireAfterWrite=7d`，只缓存第三方来源的社保标准数据。

## 登录与令牌

| 配置 | 说明 |
| --- | --- |
| `cyt.auth.password-login-enabled` | 账号密码登录开关。线上关闭，只留企微；本机在 `application-local.yml` 打开。 |
| `cyt.auth.dev-identity-enabled` | 允许前端用 `X-User-Id` 头指定身份，仅本机调试使用，接上登录态后必须关闭。 |
| `cyt.auth.admin-userids` | 逗号分隔的企微 userid 名单，通讯录同步后自动授予 admin 角色。 |
| `cyt.auth.token-timeout` | 令牌有效期，默认 604800 秒（7 天）。 |
| `sa-token.token-name` | 令牌头为 `cyt-token`，同时支持 Cookie；同一账号可在企微前台与管理控制台并发在线。 |
| `CYT_COOKIE_SECURE` | 生产走 HTTPS 时置为 `true`。 |

本机默认管理员账号在 PostgreSQL `V20261112_002__initial_seed.sql` 中，仅用于开发环境。

## 企业微信与网关

| 配置 | 环境变量 | 说明 |
| --- | --- | --- |
| 网关地址 | `CYT_WECOM_GATEWAY_URL` | 通讯录与身份换取都通过 common-api 网关发起，不在本服务保存企微密钥。 |
| 网关密钥 | `CYT_WECOM_API_KEY` | 生产必填；本机可用 `CYT_WECOM_ALLOW_DEV_IDENTITY=true` 配合 `CYT_WECOM_SYSTEM_ID` 直连。 |
| 企业 ID | `CYT_WECOM_CORP_ID` | 用于校验身份返回是否属于本企业。 |
| 通讯录同步 | `CYT_DIRECTORY_SYNC_ENABLED` / `CYT_DIRECTORY_SYNC_CRON` | 默认开启，每天 06:30 全量同步员工、部门与账号状态。 |
| 花名册同步 | `CYT_WECOM_HR_SYNC_ENABLED` / `CYT_WECOM_HR_SYNC_CRON` | 默认开启，每天北京时间 06:40 同步生日、入职日期；本地人工修改的冲突日期不自动覆盖。 |

花名册同步使用同一服务端网关凭据，需授权 `wecom.hr.get_fields`、`wecom.hr.get_staff_info`，
并确保企微应用有花名册读取权限。PostgreSQL 库先执行版本迁移（已包含同步基线表）；缺表时同步明确报错，不自动建表或假报同步成功。

## Portal 单点登录

`cyt.sso.*` 控制与门户之间的互认，默认关闭：

- `CYT_SSO_ENABLED`：是否接受门户签发的中转码。
- `CYT_SSO_PORTAL_BASE_URL` / `CYT_SSO_PORTAL_PUBLIC_BASE_URL`：门户的服务端地址与浏览器可达地址。
- `CYT_SSO_ENDPOINT_PREFIX` / `CYT_SSO_AUDIENCE` / `CYT_SSO_STATE_PREFIX` / `CYT_SSO_CODE_PREFIX`：与门户约定的接口前缀与命名空间，两端必须一致。
- `CYT_SSO_RETURN_BASE_URL`：登录完成后回跳的本服务地址。

门户只负责展示与企业身份认证，管理操作由 OA 承接。门户管理入口先进入 OA
`/api/wecom/portal/authorize?from=/portal/...`，OA 建立浏览器绑定的 nonce/PKCE 事务，
再返回门户 `/sso/oa` 使用已有登录态授权。门户签发的一次性码经 OA 后端兑换后，
只按本地已同步账号的状态、角色和权限创建 OA 会话；两个站点不共享管理员 token。

单独打开 OA 未登录时，登录页调用 `/api/wecom/panel?scope=auto` 加载企微二维码；
扫码与门户免登都由 `/api/wecom/callback` 回到 `/login` 完成会话回填。
`auto` 只由后端按实际角色选择管理或员工会话，不会让普通员工获得管理权限。
旧门户登录 token 没有可验证的企业身份声明时，需首次扫码确认；之后新门户会话可直接免登。

两端配置必须一致：Portal 的 `OA_FRONTEND_URL` 等于 OA 的 `CYT_SSO_RETURN_BASE_URL`，
浏览器使用 `CYT_SSO_PORTAL_PUBLIC_BASE_URL`，OA 服务端用 `CYT_SSO_PORTAL_BASE_URL`。
企微回调仍是门户 `/api/wework/callback`，应在企微应用中登记该域名。
生产需启用 HTTPS Cookie，并保证回调与发起请求能读到同一 HttpSession（单实例或配置会话共享/粘性路由）。

正式域名确定后填写以下配置（`oa.example.com` 是占位地址，不能直接用于上线）：

```dotenv
# Portal 后端
OA_FRONTEND_URL=https://oa.example.com
PORTAL_BASE_URL=https://portal.cyt.group

# Portal 前端构建
VITE_OA_CONSOLE_BASE_URL=https://oa.example.com

# OA 后端
CYT_SSO_ENABLED=true
CYT_SSO_PORTAL_BASE_URL=https://portal.cyt.group
CYT_SSO_PORTAL_PUBLIC_BASE_URL=https://portal.cyt.group
CYT_SSO_RETURN_BASE_URL=https://oa.example.com
CYT_WECOM_CORP_ID=填写企业CorpID
CYT_COOKIE_SECURE=true
CYT_PASSWORD_LOGIN_ENABLED=false
CYT_DEV_IDENTITY_ENABLED=false
```

上述密码与开发身份开关由环境配置读取同名环境变量；正式环境保持关闭。
未配置正式地址时保留 SSO 默认关闭，页面显示可重试的配置提示，不使用伪造二维码。

## Portal 管理迁移

迁移所需建表顺序、附件缺口和旧入口切流见 [Portal 两组管理迁移](portal-admin-migration.md)。

- `GEMINI_API_KEY` / `cyt.ai.gemini-key`：AI 厂商凭据，默认空；通过环境变量或本机配置提供。`AI_PROVIDER` / `cyt.ai.provider` 默认 `gemini`。
- `AI_KEY_ENCRYPTION_KEY`：Base64 编码的 32 字节 AES 主密钥；读取旧加密凭据时必须沿用原主密钥。
- `NAS_MINIO_ENDPOINT`、`NAS_MINIO_ACCESS_KEY`、`NAS_MINIO_SECRET_KEY`：培训资料 NAS 的连接与凭据；未配置时媒体和导入接口明确报错。`NAS_MINIO_BUCKET` 默认 `cyt-training`，`NAS_MINIO_REGION` 默认 `us-east-1`。该模块直接读取系统环境变量，写入 YAML / properties 不会生效。
- `PORTAL_DOCUMENT_ROOT`：私有附件、图片和贺卡持久卷，默认 `data/portal-documents`。真实文件与本机配置均不提交源码仓库。
- `PORTAL_PUBLIC_BASE_URL`：OA 对外基址，用于生成贺卡分享链接。`PORTAL_DISPLAY_BASE_URL`：员工展示门户基址，用于内容推送的阅读链接。
- `PORTAL_CARD_FONT`：贺卡中文字体路径；部署环境需提供可用中文字体。
- `cyt.portal.celebrations.scheduler-enabled`：默认不启用庆祝定时任务；切流时先停旧任务，再显式设置为 `true`。

菜单分批上线由系统管理员在 `/portal/rollout` 保存到 `portal_entry_rollout`，默认全部开启，无独立环境变量。PostgreSQL 初始迁移已包含该表及默认设置。开关只控制管理页面入口，角色授权、已发布内容和自动任务保持独立。

## 跨域与导出

- `CYT_ALLOWED_ORIGINS`：逗号分隔的允许来源，前后端分端口开发时必填。
- `CYT_OA_WORK_REPORT_PDF_FONT`：工作汇报 PDF 的中文字体路径，留空则按系统常见路径回退。

## 其他

- `CYT_CENTER_PORT`：服务端口，默认 48081。
- `SPRING_PROFILES_ACTIVE`：默认 `local`，加载 `application-local.yml`。
- `warm-flow`：流程定义由 `db/flow-def/*.json` 持有，启动时由 `BpmFlowDefinitionLoader` 导入；办理人权限与全局监听器由 `module/bpm` 下的两个类提供。
