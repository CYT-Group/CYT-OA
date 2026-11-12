# CTY OA

CYT 企业数字化平台 - OA 协同子模块。员工在个人中心处理汇报、审批、办事与团队事务，
管理员在控制台维护组织、流程与业务台账；员工身份与组织以企业微信为准，经 common-api 网关同步。

本仓库是单仓：`cyt-oa-backend` 与 `cyt-oa-frontend` 各为独立子工程，可分别构建。

## 文档

文档按子工程维护，根 README 只做索引与快速上手。

### 新人首读

1. [后端 README](./cyt-oa-backend/README.md) — 业务模块、技术栈、建表顺序与本地启动。
2. [前端 README](./cyt-oa-frontend/README.md) — 两级导航、页面结构、代理配置与构建。
3. [开发与验证（后端）](./cyt-oa-backend/docs/development.md) — 编译约定、冒烟检查与提交前核对。

### 专题

- [配置说明](./cyt-oa-backend/docs/configuration.md) — 数据库、Redis、企微网关、SSO 与登录开关。
- [组织与员工档案](./cyt-oa-backend/docs/organization-and-employees.md) — 企微字段归属、人事维护、账号状态及已有库升级。
- [开发与验证（前端）](./cyt-oa-frontend/docs/development.md) — 类型检查与改动约定。
- [数据库变更规范](./cyt-oa-backend/src/main/resources/db/postgresql/migration/README.md) — SQL 统一目录、版本命名、执行历史及部署前迁移。

## 主要功能

- **个人中心**：公司节奏日历、统一待办、周报月报与点评、请假加班报销等自助办事、我的团队看板与认可。
- **人事管理**：员工档案、入转调离、组织与岗位、考勤与请假、社保与薪酬核算。
- **审批流程**：基于 Warm-Flow 的流程定义与待办/已办/我发起三类视图，请假、加班、报销、出差、用品领用已接入。
- **管理控制台**：配置化的台账维护、字典、菜单与按钮权限。
- **企业微信接入**：登录与通讯录同步通过 common-api 网关完成；本地开发保留账号密码登录。

## 快速启动

### 后端（Java 21 · Maven · Spring Boot）

```bash
cd cyt-oa-backend
# 准备并填写 src/main/resources/application-local.yml 中的本地连接信息
mvn clean -DskipTests package
java -jar target/cyt-oa-backend-1.0.0.jar   # http://localhost:48081
```

首次启动前需先建库并执行 `src/main/resources/db/postgresql/migration/` 下的版本脚本，
顺序见后端 README 的「建表与初始数据」一节。local / prod 环境文件已被 Git 忽略，需在构建环境中准备；
服务起来后接口文档在 `/swagger-ui.html`。

### 前端

```bash
cd cyt-oa-frontend
npm install
npm run dev    # http://localhost:4810
```

开发服务把 `/admin-api`、`/api` 代理到 `http://127.0.0.1:48081`；
联调其他后端实例时覆盖 `VITE_PROXY_TARGET` 即可。

## 仓库结构

```text
CYT-OA/
├── cyt-oa-backend/       Spring Boot 后端：src/、scripts/、docs/
├── cyt-oa-frontend/      Vue 前端：src/、public/、docs/
└── .gitignore            统一排除 IDE、构建产物、本机配置与临时文件
```

## 部署

- 后端产出 `cyt-oa-backend/target/cyt-oa-backend-1.0.0.jar`，默认监听 `48081`；
  数据库口令、Redis 与企微网关参数走环境变量或本机配置，不写入仓库。
- 前端 `npm run build` 产出 `cyt-oa-frontend/dist/` 静态资源，由后端或反向代理托管。
- 提交前：后端跑通编译，前端跑 `npx vue-tsc --noEmit`；
  不要把本机配置、密钥、日志与构建产物纳入提交。

## 技术栈

Java 21 + Spring Boot 3.3.2 + MyBatis Plus + PostgreSQL 17+ + Redis + Warm-Flow + Vue 3 + TypeScript + Vite 6

## License

Internal CYT Group
