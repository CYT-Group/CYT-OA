# 开发说明

## 本地跑起来

后端默认端口 48081，前端 dev server 默认 4810。

```bash
# 后端
cd cyt-oa-backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 按本机库/Redis 改这个文件，它已被 .gitignore 忽略
mvn -DskipTests package
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

没有接 Flyway，建表与种子数据按 `src/main/resources/db/*.sql` 手工执行，
顺序见 [README.md](../README.md)。`local-demo-seed.sql` 只给开发机造演示数据，
不要在共享库执行。

`scripts/` 下的迁移脚本从本地配置读库连接信息（`application-local.yml`、
Portal 的 `.env`），脚本里不写口令。

## 测试

```bash
mvn test          # 后端，覆盖 SSO、密码、组织同步、档案边界和账号状态等
cd cyt-oa-frontend && npx vue-tsc --noEmit   # 前端类型检查
```

前端没有单测框架，改动后至少手工走一遍受影响的页面；接口返回值可用
`/swagger-ui.html` 直接调。

## 提交前

- `application-local.yml`、`.env` 一类真实配置不进仓库，只更新对应 `.example`。
- 新增配置项时同步更新 [configuration.md](configuration.md)。
- 不要提交 `target/`、`node_modules/`、`dist/` 与临时抓取产物。
