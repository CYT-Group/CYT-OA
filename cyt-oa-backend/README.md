# 畅韵OA · CYT OA

面向企业内部协同与人事管理的一体化系统：员工在个人中心处理汇报、审批、办事与团队事务，管理员在控制台维护组织、流程与业务台账。

本目录为 **cyt-oa-backend 后端服务**，提供账号与权限、组织与人事、审批流程、OA 台账、门户内容与个人中心的接口。配套前端见 `cyt-oa-frontend`。

## 主要功能

- **个人中心**：公司节奏日历、统一待办、周报月报与点评、请假加班报销等自助办事、我的团队看板与认可。
- **人事管理**：员工档案、入转调离、组织与岗位、考勤与请假、社保与薪酬核算。
- **审批流程**：基于 Warm-Flow 的流程定义与待办/已办/我发起的三类视图，请假、加班、报销、出差、用品领用等已接入。
- **管理控制台**：配置化的台账维护、字典、菜单与按钮权限。
- **企业微信接入**：登录与通讯录同步通过 common-api 网关完成，员工身份与部门以企微为准；本地开发保留账号密码登录。

## 技术栈

- Java 21、Spring Boot 3.3.2、Maven。
- MyBatis Plus + MySQL 8，建表与种子数据在 `src/main/resources/db/`。
- Sa-Token 身份认证与权限，Redis/Redisson 做会话与缓存。
- Warm-Flow 1.8.9 审批流引擎，PDFBox 导出工作汇报。
- springdoc-openapi 提供接口文档。

## 快速开始

### 1. 准备环境

安装 JDK 21、Maven 3.9+，并准备一个可写的 MySQL 8 与 Redis。数据库需先创建，例如 `cyt_oa_center`。

### 2. 配置本机参数

```sh
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

编辑该文件填写数据库口令、管理员 userid 等本机参数。`application-local.yml` 已被 Git 忽略，只提交 `.example`。配置项含义见[配置说明](docs/configuration.md)。

### 3. 建表与初始数据

按顺序执行 `src/main/resources/db/` 下的脚本，`local-demo-seed.sql` 只在开发库执行：

```text
system-schema.sql → dict-seed.sql → permission-seed.sql → system-seed.sql
→ oa-schema.sql → hrm-schema.sql → flow-schema.sql（配合 db/flow-def/）
→ me-schema.sql → portal-identity-schema.sql → portal-content-schema.sql
→ local-demo-seed.sql（仅本机演示）
```

已有数据库启用企微花名册同步前，需额外执行 `src/main/resources/db/wecom-hr-sync-schema.sql`。
新库的 `hrm-schema.sql` 已包含该表；此升级脚本仅创建缺失的同步基线表，不修改员工日期。

Portal 管理迁移的其他增量按[迁移说明](docs/portal-admin-migration.md)执行。启用「入口上线管理」前还需执行 `src/main/resources/db/portal-entry-rollout-schema.sql`；该脚本可重复执行且保留已有开关，默认全部开启。

### 4. 构建并启动

```sh
mvn -DskipTests package
java -jar target/cyt-oa-backend-1.0.0.jar
```

服务默认监听 `48081`，接口文档在 `/swagger-ui.html`。本地登录账号与联调方式见[开发与验证](docs/development.md)。

## 项目结构

```text
src/main/java/com/cyt/center/core/      通用组件：配置、异常、结果封装、企微网关客户端
src/main/java/com/cyt/center/module/    业务模块：auth、system、hrm、oa、bpm、portal、me
src/main/resources/db/                  建表脚本、种子数据与流程定义
src/main/resources/application.yml      默认配置，敏感值一律走环境变量或本机配置
src/test/java/                          契约与集成检查
scripts/                                Portal 数据搬迁与时间口径换算脚本
docs/                                   配置与开发文档
```

## 文档

- [配置说明](docs/configuration.md)：数据库、Redis、企微网关、SSO 与登录开关。
- [开发与验证](docs/development.md)：本地启动、编译约定、冒烟检查与提交前核对。
- [组织与员工档案](docs/organization-and-employees.md)：企微字段归属、人事维护、账号状态及已有数据库升级。
- [Portal 两组管理迁移](docs/portal-admin-migration.md)：入口、数据、附件缺口、建表顺序与切流配置。

## 参与开发

前后端在同一仓库中分别提交，提交说明描述具体改动。修改前先读[开发与验证](docs/development.md)，提交时核对差异，确保本机配置、密钥、日志与构建产物未被纳入。
