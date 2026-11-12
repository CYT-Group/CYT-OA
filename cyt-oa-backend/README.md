# CTY OA

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
- MyBatis Plus + PostgreSQL 17+，建表与种子数据在 `src/main/resources/db/postgresql/migration/`。
- Sa-Token 身份认证与权限，Redis/Redisson 做会话与缓存。
- Warm-Flow 1.8.9 审批流引擎，PDFBox 导出工作汇报。
- springdoc-openapi 提供接口文档。

## 快速开始

### 1. 准备环境

安装 JDK 21、Maven 3.9+，并准备一个可写的 PostgreSQL 17+ 与 Redis。数据库需先创建，例如 `cyt_oa_center`。

### 2. 配置本机参数

准备并编辑 `src/main/resources/application-local.yml`，填写本地 PostgreSQL / Redis 连接、
企微网关和管理员 userid。该文件已被 Git 忽略。生产参数单独填写到
`src/main/resources/application-prod.properties`，通过 `prod` profile 加载。
公共框架设置保留在 `application.yml`。配置项含义见[配置说明](docs/configuration.md)。

### 3. 建表与初始数据

数据库统一使用 `src/main/resources/db/postgresql/migration/`。
新建空数据库后，用 Flyway 执行该目录的全部版本：

```sh
flyway -url=jdbc:postgresql://127.0.0.1:5432/cyt_oa_center -user=cyt_oa \
  -locations=filesystem:src/main/resources/db/postgresql/migration migrate
```

口令通过 `FLYWAY_PASSWORD` 环境变量提供。也可在本机空测试库按版本顺序用
`psql -v ON_ERROR_STOP=1 --single-transaction -f <文件>` 执行；长期开发和部署统一使用 Flyway。
完整结构包含门户、人事工作区和企微花名册，种子包含字典、权限和开发管理员。
本机演示数据单独放在 `db/postgresql/local-demo-seed.sql`，仅在开发库执行。

版本迁移负责结构和内置种子，业务数据通过相应导入流程写入。
后续结构变更遵循[数据库变更规范](src/main/resources/db/postgresql/migration/README.md)；
部署的空库初始化和后续升级见[部署说明](../deploy/test-server/README.md)。

### 4. 构建并启动

```sh
mvn clean -DskipTests package
java -jar target/cyt-oa-backend-1.0.0.jar
```

服务默认监听 `48081`，接口文档在 `/swagger-ui.html`。本地登录账号与联调方式见[开发与验证](docs/development.md)。

## 项目结构

```text
src/main/java/com/cyt/center/core/      通用组件：配置、异常、结果封装、企微网关客户端
src/main/java/com/cyt/center/module/    业务模块：auth、system、hrm、oa、bpm、portal、me
src/main/resources/db/postgresql/       PostgreSQL 版本迁移与本机演示数据
src/main/resources/db/flow-def/         审批流程定义
src/main/resources/application.yml      公共配置及环境变量映射
src/main/resources/application-local.yml 本地连接与开发开关（Git 忽略）
src/main/resources/application-prod.properties 生产连接与运行开关（Git 忽略）
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
