const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const root = path.join(__dirname, "..");
function moduleAt(file, dependencies = {}) {
  const source = fs.readFileSync(path.join(root, file), "utf8");
  const code = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  }).outputText;
  const context = {
    exports: {},
    URL,
    Error,
    require: (name) => {
      if (!(name in dependencies))
        throw new Error("Unexpected dependency: " + name);
      return dependencies[name];
    },
  };
  vm.runInNewContext(code, context);
  return context.exports;
}
const { portalMenus } = moduleAt("src/configs/portal.ts");
const policy = moduleAt("src/lib/portal-entry-policy.ts", {
  "../configs/portal": { portalMenus },
});
const enabled = () =>
  Object.fromEntries(
    [...policy.configurablePortalKeys].map((key) => [key, true]),
  );
function runtime(get) {
  const session = { token: "first-token", canAccessConsole: true };
  const store = moduleAt("src/lib/portal-rollout.ts", {
    vue: { reactive: (value) => value },
    "./api": { api: { get } },
    "./session": { session },
    "./portal-entry-policy": policy,
  });
  return { session, ...store };
}
test("前后端上线目录一致，恢复入口不能被当作业务开关", () => {
  const source = fs.readFileSync(
    path.join(
      root,
      "../cyt-oa-backend/src/main/java/com/cyt/center/module/portal/service/admin/PortalEntryRolloutService.java",
    ),
    "utf8",
  );
  const list = source.match(/ENTRY_KEYS\s*=\s*List\.of\(([\s\S]*?)\);/)[1];
  const backend = [...list.matchAll(/"(portal-[a-z-]+)"/g)]
    .map((match) => match[1])
    .sort();
  assert.deepEqual([...policy.configurablePortalKeys].sort(), backend);
  assert.equal(backend.length, 20);
  assert.equal(backend.includes("portal-hub"), false);
  assert.equal(backend.includes("portal-rollout"), false);
});
test("同路径的样品和办公用品独立控制，HR编辑预览继承同一个开关", () => {
  assert.equal(
    policy.portalEntryForRoute("/portal/materials", { kind: "office" }).key,
    "portal-supplies",
  );
  assert.equal(
    policy.portalEntryForRoute("/portal/materials", { kind: "sample" }).key,
    "portal-samples",
  );
  assert.equal(
    policy.portalEntryForRoute("/portal/materials", {}).key,
    "portal-samples",
  );
  assert.equal(
    policy.portalEntryForRoute("/portal/hr-documents/id/17/preview").key,
    "portal-hr-documents",
  );
  assert.equal(
    policy.portalEntryForRoute("/portal/hr-documents/new").key,
    "portal-hr-documents",
  );
  assert.equal(
    policy.portalEntryForRoute("/page/portal-events").key,
    "portal-events",
  );
  assert.equal(policy.portalEntryForRoute("/portal-news"), undefined);
});
test("恢复按钮只允许回到已知管理入口，不跳转外站或注入地址", () => {
  assert.equal(
    policy.safePortalEntryTarget("/portal/materials?kind=office"),
    "/portal/materials?kind=office",
  );
  for (const unsafe of [
    "https://evil.example/portal/ai",
    "//evil.example",
    "javascript:alert(1)",
    "/page/sys-users",
    undefined,
  ])
    assert.equal(policy.safePortalEntryTarget(unsafe), "/portal");
});
test("入口匹配兼容路由的大小写与末尾斜线，不能绕过关闭状态", () => {
  assert.equal(
    policy.portalEntryForRoute("/PORTAL/HR-DOCUMENTS/id/17/EDIT/").key,
    "portal-hr-documents",
  );
  assert.equal(
    policy.portalEntryForRoute("/PORTAL/MATERIALS/", { kind: "office" }).key,
    "portal-supplies",
  );
  assert.equal(
    policy.portalEntryForRoute("/PAGE/PORTAL-EVENTS/").key,
    "portal-events",
  );
});
test("并发读取合并为一个请求，保存后导航即时使用新状态", async () => {
  let resolve,
    calls = 0;
  const state = runtime(() => {
    calls++;
    return new Promise((done) => {
      resolve = done;
    });
  });
  assert.equal(state.portalEntryEnabled("portal-ai"), false);
  const one = state.loadPortalRollout(),
    two = state.loadPortalRollout();
  await new Promise(setImmediate);
  assert.equal(calls, 1);
  resolve({ entries: enabled(), revision: "0" });
  await Promise.all([one, two]);
  assert.equal(state.portalEntryEnabled("portal-ai"), true);
  state.applyPortalRollout({
    entries: { ...enabled(), "portal-ai": false },
    revision: "1",
  });
  assert.equal(state.portalEntryEnabled("portal-ai"), false);
  assert.equal(state.portalEntryEnabled("portal-hub"), true);
  assert.equal(state.portalEntryEnabled("portal-rollout"), true);
});
test("配置读取失败或格式残缺时不泄露未上线菜单，恢复入口保持可用", async () => {
  for (const get of [
    async () => {
      throw new Error("读取失败");
    },
    async () => ({ entries: {}, revision: 0 }),
  ]) {
    const state = runtime(get);
    await assert.rejects(state.loadPortalRollout());
    assert.equal(state.portalEntryEnabled("portal-ai"), false);
    assert.equal(state.portalEntryEnabled("portal-rollout"), true);
    assert.equal(state.portalRollout.loaded, false);
  }
});
test('关闭分组的首个入口不会隐藏其他仍开启的同组功能', () => {
  const state = runtime(async () => ({ entries: enabled(), revision: 0 }));
  state.session.has = () => true; state.session.isAdmin = true;
  state.applyPortalRollout({ entries: { ...enabled(), 'portal-hr-documents': false }, revision: 1 });
  const access = moduleAt('src/lib/portal-access.ts', { './session': { session: state.session }, './portal-rollout': state, './portal-entry-policy': policy });
  const groups = access.filterPortalNavigation([{ key: 'admin', title: '管理', adminOnly: true, items: [{ key: 'HR 管理', title: 'HR 管理', to: '/portal/hr-documents', children: [
    { key: 'portal-hr-documents', to: '/portal/hr-documents', title: 'HR 文档管理' },
    { key: 'portal-signoff', to: '/portal/signoff', title: '签收看板' },
  ] }] }]);
  assert.equal(groups[0].items[0].children.length, 1);
  assert.equal(groups[0].items[0].children[0].key, 'portal-signoff');
});
test("账号切换后丢弃旧账号的晚响应，不覆盖新会话状态", async () => {
  const pending = [];
  const state = runtime(() => new Promise((resolve) => pending.push(resolve)));
  const old = state.loadPortalRollout();
  await new Promise(setImmediate);
  state.session.token = "second-token";
  const current = state.loadPortalRollout();
  await new Promise(setImmediate);
  pending[1]({ entries: { ...enabled(), "portal-ai": false }, revision: 2 });
  await current;
  pending[0]({ entries: enabled(), revision: 0 });
  await old;
  assert.equal(state.portalRollout.owner, "second-token");
  assert.equal(state.portalEntryEnabled("portal-ai"), false);
});
test("保存后晚到的旧读取结果不能重新打开已关闭入口", async () => {
  let resolve;
  const state = runtime(
    () =>
      new Promise((done) => {
        resolve = done;
      }),
  );
  const read = state.loadPortalRollout();
  await new Promise(setImmediate);
  state.applyPortalRollout({
    entries: { ...enabled(), "portal-ai": false },
    revision: "2",
  });
  resolve({ entries: enabled(), revision: "1" });
  await read;
  assert.equal(state.portalEntryEnabled("portal-ai"), false);
  assert.equal(state.portalRollout.revision, "2");
});
