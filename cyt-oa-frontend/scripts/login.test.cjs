const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
function moduleAt(file, dependencies = {}) {
  const output = ts.transpileModule(
    fs.readFileSync(path.join(__dirname, "..", file), "utf8"),
    {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        target: ts.ScriptTarget.ES2022,
      },
    },
  ).outputText;
  const context = {
    exports: {},
    URL,
    require: (name) => {
      if (!(name in dependencies))
        throw new Error("Unexpected dependency: " + name);
      return dependencies[name];
    },
  };
  vm.runInNewContext(output, context);
  return context.exports;
}
const {
  safeLoginTarget,
  parseLoginTarget,
  selectLoginTarget,
  hasLoginHandoff,
  validWecomPanel,
} = moduleAt("src/lib/login-target.ts");

test("登录后保留站内文档深链、筛选和片段", () => {
  for (const target of [
    "/",
    "/me/overview",
    "/portal/hr-documents/id/17/edit",
    "/portal/materials?kind=office&tab=applies",
    "/portal/hr-documents?q=%E5%91%98%E5%B7%A5%20%E6%89%8B%E5%86%8C#list",
  ])
    assert.equal(safeLoginTarget(target), target);
});
test("拒绝外站、反斜杠、控制字符、编码逃逸与认证循环", () => {
  for (const target of [
    undefined,
    null,
    [],
    ["//evil.invalid"],
    "https://evil.invalid",
    "//evil.invalid",
    "/\\evil.invalid",
    "/%2fevil.invalid",
    "/%252fevil.invalid",
    "/%25252fevil.invalid",
    "/%255cevil.invalid",
    "/portal\n/abc",
    "/portal/%00abc",
    "/portal/%250dabc",
    "/login",
    "/login?from=/login",
    "/LoGiN/",
    "/api/wecom/login",
    "/admin-api/portal/rows",
    "/portal/../login",
    "/portal/%2e%2e/api/auth/login",
    " /me/overview",
    "/me/overview ",
    "/%not-valid",
  ])
    assert.equal(safeLoginTarget(target), "/", String(target));
});

test("Portal 不可用回填优先保留 from，空事务不能吞掉原业务目标或显式首页", () => {
  const target = "/portal/hr-documents?q=%E5%91%98%E5%B7%A5#list";
  assert.equal(selectLoginTarget(target, "/"), target);
  assert.equal(selectLoginTarget(target, "/me/overview"), target);
  assert.equal(selectLoginTarget("/", "/portal/hr-documents"), "/");
  assert.equal(parseLoginTarget("/"), "/");
});

test("缺少或非法 from 才恢复事务目标，非法事务最终回到首页", () => {
  for (const from of [
    undefined,
    null,
    [],
    ["/portal"],
    "//evil.invalid",
    "/api/wecom/login",
    "/login",
    "/%252fevil.invalid",
  ]) {
    assert.equal(parseLoginTarget(from), null);
    assert.equal(
      selectLoginTarget(from, "/portal/materials?kind=office"),
      "/portal/materials?kind=office",
    );
    assert.equal(selectLoginTarget(from, "//evil.invalid"), "/");
  }
});
test("已有 OA 身份遇到交接码或失败回填仍进入身份校准流程", () => {
  assert.equal(hasLoginHandoff({ from: "/portal" }), false);
  for (const key of ["wecom_code", "wecom_error", "sso_error"]) {
    assert.equal(hasLoginHandoff({ [key]: "" }), true);
    assert.equal(hasLoginHandoff({ [key]: ["a", "b"] }), true);
  }
});
const config = {
  available: true,
  publicBaseUrl: "https://oa.cyt.group",
  portalBaseUrl: "https://portal.cyt.group",
  oauthCallbackUri: "https://portal.cyt.group/api/wework/callback",
  statePrefix: "oa.",
};
const params = {
  login_type: "CorpApp",
  appid: "corp",
  agentid: "123",
  redirect_uri: config.oauthCallbackUri,
  state: "oa." + "A".repeat(43),
  redirect_type: "top",
  panel_size: "small",
  lang: "zh",
};
test("二维码参数只接受当前 OA origin 与登记的 Portal 固定回调", () => {
  assert.equal(validWecomPanel(config, params, config.publicBaseUrl), true);
  for (const change of [
    { redirect_uri: "https://evil.invalid/api/wework/callback" },
    { state: "sales." + "A".repeat(43) },
    { state: "oa.short" },
    { redirect_type: "callback" },
    { appid: "" },
    { panel_size: "middle" },
  ])
    assert.equal(
      validWecomPanel(config, { ...params, ...change }, config.publicBaseUrl),
      false,
    );
  assert.equal(
    validWecomPanel(config, params, "https://another-oa.invalid"),
    false,
  );
  assert.equal(
    validWecomPanel(
      { ...config, available: false },
      params,
      config.publicBaseUrl,
    ),
    false,
  );
  assert.equal(
    validWecomPanel(
      { ...config, portalBaseUrl: "http://portal.cyt.group" },
      params,
      config.publicBaseUrl,
    ),
    false,
  );
});
function deferred() {
  let resolve, reject;
  const promise = new Promise((yes, no) => {
    resolve = yes;
    reject = no;
  });
  return { promise, resolve, reject };
}
function summaryStore() {
  const session = { token: "employee-a" };
  const requests = [];
  const store = moduleAt("src/lib/me.ts", {
    vue: { reactive: (value) => value, ref: (value) => ({ value }) },
    "./api": {
      api: {
        get() {
          const pending = deferred();
          requests.push(pending);
          return pending.promise;
        },
      },
    },
    "./session": { session },
  });
  return { store, session, requests };
}
const summary = (deptName) => ({
  deptName,
  jobTitle: "",
  teamCount: 2,
  counts: { reminder: 1, report: 1, team: 1 },
});
test("切换身份后上一位员工的晚响应不能覆盖新员工组织与提醒", async () => {
  const { store, session, requests } = summaryStore();
  const old = store.loadMeSummary();
  store.resetMeSummary();
  assert.equal(store.meSummaryLoaded.value, false);
  assert.equal(store.meSummary.teamCount, 0);
  session.token = "employee-b";
  const current = store.loadMeSummary();
  requests[1].resolve(summary("B"));
  await current;
  requests[0].resolve(summary("A"));
  await old;
  assert.equal(store.meSummary.deptName, "B");
});
test("旧员工的失败请求不能清空新会话已加载的个人摘要", async () => {
  const { store, session, requests } = summaryStore();
  const old = store.loadMeSummary();
  store.resetMeSummary();
  session.token = "employee-b";
  const current = store.loadMeSummary();
  requests[1].resolve(summary("B"));
  await current;
  requests[0].reject(new Error("old unauthorized"));
  await old;
  assert.equal(store.meSummary.deptName, "B");
  assert.equal(store.meSummaryLoaded.value, true);
});

function apiInterceptors() {
  const handlers = {};
  const navigations = [];
  const session = {
    token: "employee-b",
    clear() {
      this.token = "";
    },
  };
  moduleAt("src/lib/api.ts", {
    axios: {
      default: {
        create: () => ({
          interceptors: {
            request: {
              use(fn) {
                handlers.request = fn;
              },
            },
            response: {
              use(success, failure) {
                handlers.success = success;
                handlers.failure = failure;
              },
            },
          },
        }),
      },
    },
    "../router": {
      default: {
        currentRoute: {
          value: { path: "/me/overview", fullPath: "/me/overview", query: {} },
        },
        push(target) {
          navigations.push(target);
        },
        replace(target) {
          navigations.push(target);
        },
      },
    },
    "./session": { session },
  });
  return { handlers, session, navigations };
}
test("旧身份晚到的业务/HTTP 401 不会注销新 OA 身份", () => {
  const { handlers, session, navigations } = apiInterceptors();
  const config = { headers: { "cyt-token": "employee-a" } };
  assert.throws(() =>
    handlers.success({
      status: 200,
      data: { code: 401, message: "expired" },
      config,
    }),
  );
  assert.throws(() =>
    handlers.success({ status: 401, data: { code: 401 }, config }),
  );
  assert.throws(() => handlers.failure({ response: { status: 401 }, config }));
  assert.equal(session.token, "employee-b");
  assert.equal(navigations.length, 0);
});
test("当前会话的 401 仍清理身份并保留原业务页供重新登录", () => {
  const { handlers, session, navigations } = apiInterceptors();
  assert.throws(() =>
    handlers.failure({
      response: { status: 401 },
      config: { headers: { "cyt-token": "employee-b" } },
    }),
  );
  assert.equal(session.token, "");
  assert.equal(navigations[0].path, "/login");
  assert.equal(navigations[0].query.from, "/me/overview");
});
