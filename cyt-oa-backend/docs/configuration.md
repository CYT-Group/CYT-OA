# 配置说明

[返回项目首页](../README.md)

所有可变项都有环境变量入口，`application.yml` 只放默认值，真实口令一律写在被 Git 忽略的 `application-local.yml` 里。

## 数据库

| 配置 | 环境变量 | 默认值 |
| --- | --- | --- |
| 连接 | `spring.datasource.url` | `CYT_MYSQL_HOST` / `CYT_MYSQL_PORT` / `CYT_MYSQL_DATABASE`，默认 `127.0.0.1:3306/cyt_oa_center` |
| 账号 | `spring.datasource.username` | `CYT_MYSQL_USERNAME`，默认 `root` |
| 口令 | `spring.datasource.password` | `CYT_MYSQL_PASSWORD`，默认空，必须由本机配置或环境变量提供 |

建表与种子数据在 `src/main/resources/db/`，执行顺序见[项目首页](../README.md#3-建表与初始数据)。没有引入 Flyway/Liquibase，schema 变更直接维护脚本文件。

## Redis 与缓存

`spring.data.redis.*` 走 `CYT_REDIS_HOST` / `CYT_REDIS_PORT` / `CYT_REDIS_DATABASE` / `CYT_REDIS_PASSWORD`，默认本机 6379 的 1 号库。连接工厂由 Redisson 提供，Lettuce 已在依赖里排除；Spring Data Redis 的 Repository 扫描关闭。

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

本机默认管理员账号在 `db/system-seed.sql` 中，仅用于开发环境。

## 企业微信与网关

| 配置 | 环境变量 | 说明 |
| --- | --- | --- |
| 网关地址 | `CYT_WECOM_GATEWAY_URL` | 通讯录与身份换取都通过 common-api 网关发起，不在本服务保存企微密钥。 |
| 网关密钥 | `CYT_WECOM_API_KEY` | 生产必填；本机可用 `CYT_WECOM_ALLOW_DEV_IDENTITY=true` 配合 `CYT_WECOM_SYSTEM_ID` 直连。 |
| 企业 ID | `CYT_WECOM_CORP_ID` | 用于校验身份返回是否属于本企业。 |
| 通讯录同步 | `CYT_DIRECTORY_SYNC_ENABLED` / `CYT_DIRECTORY_SYNC_CRON` | 默认开启，每天 06:30 全量同步员工、部门与账号状态。 |
| 花名册同步 | `CYT_WECOM_HR_SYNC_ENABLED` / `CYT_WECOM_HR_SYNC_CRON` | 默认开启，每天北京时间 06:40 同步生日、入职日期；本地人工修改的冲突日期不自动覆盖。 |

花名册同步使用同一服务端网关凭据，需授权 `wecom.hr.get_fields`、`wecom.hr.get_staff_info`，
并确保企微应用有花名册读取权限。已有库先执行 `db/wecom-hr-sync-schema.sql`；缺表时同步明确报错，不自动建表或假报同步成功。

## Portal 单点登录

`cyt.sso.*` 控制与门户之间的互认，默认关闭：

- `CYT_SSO_ENABLED`：是否接受门户签发的中转码。
- `CYT_SSO_PORTAL_BASE_URL` / `CYT_SSO_PORTAL_PUBLIC_BASE_URL`：门户的服务端地址与浏览器可达地址。
- `CYT_SSO_ENDPOINT_PREFIX` / `CYT_SSO_AUDIENCE` / `CYT_SSO_STATE_PREFIX` / `CYT_SSO_CODE_PREFIX`：与门户约定的接口前缀与命名空间，两端必须一致。
- `CYT_SSO_RETURN_BASE_URL`：登录完成后回跳的本服务地址。

## 跨域与导出

- `CYT_ALLOWED_ORIGINS`：逗号分隔的允许来源，前后端分端口开发时必填。
- `CYT_OA_WORK_REPORT_PDF_FONT`：工作汇报 PDF 的中文字体路径，留空则按系统常见路径回退。

## 其他

- `CYT_CENTER_PORT`：服务端口，默认 48081。
- `SPRING_PROFILES_ACTIVE`：默认 `local`，加载 `application-local.yml`。
- `warm-flow`：流程定义由 `db/flow-def/*.json` 持有，启动时由 `BpmFlowDefinitionLoader` 导入；办理人权限与全局监听器由 `module/bpm` 下的两个类提供。
