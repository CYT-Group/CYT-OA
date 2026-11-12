# 开发说明

## 本地跑起来

后端默认端口 48081，前端 dev server 默认 4810。

```bash
# 后端
cd cyt-oa-backend
# 准备并填写 src/main/resources/application-local.yml 中的本机库/Redis 配置（Git 忽略）
mvn clean -DskipTests package
java -jar target/cyt-oa-backend-1.0.0.jar

# 前端
cd cyt-oa-frontend
npm install
npm run dev
```

前端代理指向 48081；联调新构建的类时改指别的端口：

```bash
VITE_PROXY_TARGET=http://127.0.0.1:48082 npm run dev
```

## 改代码时的编译约定

`spring-boot-starter-parent` 会给 javac 带上 `-parameters`，走 Maven 构建不用管。
但绕开 Maven 手工增量编译时必须显式加，否则所有未具名的 `@RequestParam`
运行期都抛 `IllegalArgumentException: Name for argument of type ... not specified`，
表现为接口 500：

```bash
javac -parameters -encoding UTF-8 -proc:full -nowarn \
  -d target/classes -cp "target/classes;$(cat target/cp.txt)" @target/all.txt
```

直接起 classes 而不是 jar 时同理，`-parameters` 是编译期参数，
写成 `java -parameters ...` 会被 JVM 当未知选项拒绝。

## 数据库

新增数据库变更统一遵循 [数据库变更规范](../src/main/resources/db/postgresql/migration/README.md)，
放在 `src/main/resources/db/postgresql/migration/`，通过部署入口的 Flyway 执行并记录版本；
不在应用启动时自动迁移。新空库直接执行 `migrate`，后续部署应用待执行的版本。
初始化顺序见 [README.md](../README.md)。`db/postgresql/local-demo-seed.sql` 只给开发机造演示数据。

`scripts/` 下的 Portal 搬迁脚本读取 OA 的 PostgreSQL 配置与 Portal 的 `.env`，
源库仍只读。先 `python -m pip install -r scripts/requirements.txt`，再使用各脚本的
`--dry-run`（排版任务脚本默认 dry-run，`--apply` 才写入）核对。正式搬迁应在目标库停写期间进行，
提交前会向前推进 identity 序列，避免保留历史 ID 后新数据主键冲突。脚本里不写口令。

## 测试

```bash
mvn test          # 后端，覆盖 SSO、密码、组织同步、档案边界和账号状态等
cd cyt-oa-frontend && npx vue-tsc --noEmit   # 前端类型检查
```

PostgreSQL 兼容测试需显式指定一次性测试库，不能指向业务库：设置
`CYT_TEST_PG_URL=jdbc:postgresql://127.0.0.1:5432/oa_test?stringtype=unspecified`、
`CYT_TEST_PG_USERNAME`、`CYT_TEST_PG_PASSWORD` 后运行 `mvn test`。
Portal 搬迁脚本的测试使用 `OA_TEST_POSTGRES_DSN`，需具备创建和删除临时库的权限，
运行 `python -B -m unittest discover -s scripts -v`；不设置连接时跳过真实数据库检查。

前端没有单测框架，改动后至少手工走一遍受影响的页面；接口返回值可用
`/swagger-ui.html` 直接调。

## 提交前

- `application-local.yml`、`.env` 一类真实配置不进仓库，只更新对应 `.example`。
- 新增配置项时同步更新 [configuration.md](configuration.md)。
- 不要提交 `target/`、`node_modules/`、`dist/` 与临时抓取产物。
