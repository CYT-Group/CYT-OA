# 数据库变更规范

当前业务库为 PostgreSQL 17+。所有新结构、索引、权限种子和随版本交付的数据修正，
统一新增到 `src/main/resources/db/postgresql/migration/`，由部署入口的 Flyway 执行。
应用启动本身不修改结构。

## 新库与现有数据库

PostgreSQL 空库直接 `migrate`。首批版本分别建立全部结构和初始字典、权限、账号，
已包含门户、人事工作区与企微花名册结构，不跳过首批建表版本。
演示业务数据独立放在 `db/postgresql/local-demo-seed.sql`，不参与部署。

已有 PostgreSQL 环境执行待应用版本；发布前备份并确认目标库，校验关联、记录数、
时间、JSON 与 identity 序列。部署只使用 PostgreSQL 数据卷。

## 新增 SQL

1. 新建 `VYYYYMMDD_NNN__英文说明.sql`，日期使用北京时间，同一天序号从 `001` 递增。
2. 文件直接放 PostgreSQL 迁移目录，使用 UTF-8 无 BOM、LF 换行。
3. 操作当前连接的库与 schema，不写固定库名、口令或环境地址。
4. 在隔离 PostgreSQL 测试库验证，再将 SQL 与业务代码一同交付。

例如：

```sql
-- 目的：增加可空外部编号；旧应用忽略该字段。
ALTER TABLE hrm_employee ADD COLUMN external_id VARCHAR(100);
COMMENT ON COLUMN hrm_employee.external_id IS '外部系统编号';
```

状态与逻辑删除列保留 SMALLINT 数值语义，Java Boolean 经统一 TypeHandler/JdbcTemplate
绑定转换为 0/1。结构化 JSON 按既有 Java String 约定存 TEXT；需要 JSON 查询时显式转换。
时间字段使用 timestamp without time zone，连接会话时区固定为 Asia/Shanghai。

## 已执行版本冻结

任何共享环境成功执行的文件及文件名均不能修改或删除；修正继续新增版本。
Flyway 校验历史、校验值及文件完整性，禁止自动乱序、自动 baseline、clean、repair。
回退应用不自动回退数据库，旧代码须兼容新增结构。大表回填、收紧非空约束、删列等
需先在测试库评估，并采用兼容结构、回填、再收紧的分步升级方式。

## 部署与验证

Jenkins：检出 → 构建推送镜像 → Flyway 校验及执行迁移 → 更新应用 → 页面和登录检查。
SQL 失败会阻断应用更新；`VERIFY_ONLY=true` 与 `SKIP_DEPLOY=true` 不执行数据库迁移。
手动 `redeploy.sh` 也先迁移。数据库口令只从容器环境读取，不进入命令行或日志。

```bash
bash deploy/test-server/db-upgrade.sh "$ROOT" lint
bash deploy/test-server/db-upgrade.sh "$ROOT" info
bash deploy/test-server/db-upgrade.sh "$ROOT" validate
bash deploy/test-server/db-upgrade.sh "$ROOT" migrate
python3 -B -m unittest discover -s deploy/test-server/tests -v
# 显式建立一次性 PostgreSQL 容器，不连接业务库
OA_RUN_FLYWAY_INTEGRATION=1 python3 -B deploy/test-server/tests/test_flyway_integration.py -v
```

执行记录在 PostgreSQL `flyway_schema_history`。失败后保留日志并核对事务状态，
不能篡改校验值跳过检查。部署和空库初始化细节见 [部署说明](../../../../../../../deploy/test-server/README.md)。

本机也可用官方 Flyway CLI，指定 JDBC URL 和 PostgreSQL 迁移目录执行 `migrate`，
口令走 `FLYWAY_PASSWORD` 环境变量。不要把测试服口令复制到源码。

官方说明：[版本迁移](https://documentation.red-gate.com/flyway/reference/commands/migrate)、
[校验](https://documentation.red-gate.com/flyway/reference/commands/validate)、
[PostgreSQL INSERT](https://www.postgresql.org/docs/17/sql-insert.html)。
