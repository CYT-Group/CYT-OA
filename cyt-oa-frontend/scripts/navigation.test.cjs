const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const root = path.join(__dirname, "..");
function moduleAt(file, dependencies = {}, globals = {}) {
  const code = ts.transpileModule(
    fs.readFileSync(path.join(root, file), "utf8"),
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
    Error,
    Set,
    ...globals,
    require(name) {
      if (!(name in dependencies))
        throw new Error("Unexpected dependency: " + name);
      return dependencies[name];
    },
  };
  vm.runInNewContext(code, context);
  return context.exports;
}
const portal = moduleAt("src/configs/portal.ts");
const pages = moduleAt("src/configs/pages.ts", { "./portal": portal });
const { navZones } = moduleAt("src/configs/nav.ts", { "./pages": pages });
const { findNavigationMatch, sidebarNavigation, personalizeNavigation } = moduleAt("src/lib/navigation.ts");
const policy = moduleAt("src/lib/portal-entry-policy.ts", {
  "../configs/portal": portal,
});
const leaves = (zones) =>
  zones.flatMap((zone) =>
    zone.items.flatMap((item) => item.children ?? [item]),
  );
function filtered({ admin = true, hrOnly = false, permissions, disabled = [] } = {}) {
  const access = moduleAt("src/lib/portal-access.ts", {
    "./session": {
      session: {
        isAdmin: admin,
        isHrOnly: hrOnly,
        has: (permission) =>
          permissions ? permissions.includes(permission) : true,
      },
    },
    "./portal-rollout": {
      portalEntryEnabled: (key) => !disabled.includes(key),
    },
    "./portal-entry-policy": policy,
  });
  return access.filterPortalNavigation(navZones);
}

test("重新分组后保留每个原有业务入口及其上线 key", () => {
  const present = new Set(leaves(navZones).map((item) => item.key));
  for (const item of pages.menuGroups.flatMap((group) => group.items))
    assert.ok(present.has(item.key), item.key);
});

test("侧栏按功能树收敛，人事只显示五个业务入口", () => {
  const compact = sidebarNavigation(filtered());
  const admin = compact.find(zone => zone.adminOnly);
  assert.equal(admin.items.length, 12);
  assert.ok(!admin.items.some(item => ["后台管理", "HR 管理", "系统管理", "办公台账", "console"].includes(item.key)));
  const hr = admin.items.find(item => item.key === "people-management");
  assert.deepEqual(Array.from(hr.children.map(item => item.title)), ["人事工作台", "招聘管理", "花名册", "入离职与合同", "员工生命周期"]);
  const tools = admin.items.find(item => item.key === "management-tools");
  assert.equal(tools.children, undefined);
  assert.equal(tools.to, "/portal?view=tools");
});

test("门户主目录包含全部员工模块，条件入口按真实团队和站点负责人显示", () => {
  const base = { teamCount: 0, isAdmin: false, canAccessConsole: false, canApproveMaterials: false };
  const employee = personalizeNavigation(filtered({ admin: false, permissions: [] }), base);
  const own = employee.find(zone => zone.key === 'me');
  assert.equal(own.items.length, 8);
  assert.ok(own.items.some(item => item.key === 'okr'));
  assert.equal(own.items.find(item => item.key === 'voice').children.length, 4);
  assert.ok(!leaves(employee).some(item => item.requiresApprover || item.requiresMaterialsApprover));
  assert.ok(leaves(employee).some(item => item.to === '/me/review-reports'));
  for (const tab of ['attendance', 'training', 'celebrations', 'office', 'sample'])
    assert.ok(leaves(employee).some(item => item.to === `/me/records?tab=${tab}`), tab);
  const leader = personalizeNavigation(filtered({ admin: false, permissions: [] }), { ...base, teamCount: 3, canApproveMaterials: true });
  assert.equal(leader.find(zone => zone.key === 'me').items.length, 9);
  assert.ok(leaves(leader).some(item => item.to === '/me/requests?view=pending'));
  assert.ok(leaves(leader).some(item => item.to === '/me/records?tab=approvals'));
  assert.ok(leaves(leader).some(item => item.to === '/me/team?tab=milestone'));
});

test("新业务深链归属正确，普通管理成员不能看到CEO管理或数据权限", () => {
  const all = sidebarNavigation(personalizeNavigation(filtered(), { teamCount: 1, isAdmin: true, canAccessConsole: true, canApproveMaterials: true }));
  for (const [url, parent] of [['/me/okr?tab=align', 'okr'], ['/me/voice?channel=survey', 'voice'], ['/me/review-reports', 'report'], ['/me/records?tab=sample', 'claims'], ['/portal/voice?tab=release', 'voice-management'], ['/portal/workspace-rules?section=flow&tab=leave', 'approval-templates'], ['/portal/workspace-rules?section=scope', 'permissions'], ['/portal/celebrations?tab=settings&template=anniversary', 'celebrations']])
    assert.equal(findNavigationMatch(all, url)?.item.key, parent, url);
  assert.equal(all.find(zone => zone.key === 'me').items.find(item => item.key === 'overview').to, '/admin');
  const manager = leaves(filtered({ admin: false, permissions: ['can_enter_admin', 'can_manage_voice', 'can_manage_workspace_rules'] }));
  assert.ok(manager.some(item => item.to === '/portal/voice?tab=suggestion'));
  assert.ok(!manager.some(item => item.to === '/portal/voice?tab=ceo'));
  assert.ok(!manager.some(item => item.to === '/portal/workspace-rules?section=scope'));
});

test("无key的兄弟页签不互相收集别名，收拢后个人记录仍精确选中", () => {
  const compact = sidebarNavigation(filtered());
  for (const [tab, title] of [['attendance', '考勤与请假'], ['training', '培训记录'], ['celebrations', '我的祝贺'], ['office', '办公用品申领记录'], ['sample', '样品申领记录']]) {
    const match = findNavigationMatch(compact, `/me/records?tab=${tab}`);
    assert.equal(match?.child?.title, title, tab);
  }
  const profile = compact.find(zone => zone.key === 'me').items.find(item => item.key === 'profile');
  assert.ok(!profile.children.find(item => item.title === '我的祝贺').aliases.includes('/me/records?tab=attendance'));
});

test("没有下属但被指定审批的员工可发现待我审批，不扩展团队与管理区", () => {
  const zones = personalizeNavigation(filtered({ admin: false, permissions: [] }), { teamCount: 0, isAdmin: false, canAccessConsole: false, canApproveMaterials: false, canApproveRequests: true });
  assert.ok(leaves(zones).some(item => item.to === '/me/requests?view=pending'));
  assert.ok(!zones.find(zone => zone.key === 'me').items.some(item => item.key === 'team'));
});

test("收拢页签后深链仍选中对应业务，辅助工具仍可搜索", () => {
  const compact = sidebarNavigation(filtered());
  for (const [url, key] of [["/hr/recruit?tab=interviews", "hr-candidates"], ["/hr/recruit?tab=posts", "hr-candidates"], ["/page/hrm-employee?roster=left", "hrm-employee"], ["/hr/templates", "hr-onboard"], ["/portal/contracts", "hr-onboard"]])
    assert.equal(findNavigationMatch(compact, url)?.child?.key, key, url);
  assert.equal(findNavigationMatch(compact, "/portal/ai")?.item.key, "management-tools");
  const catalog = leaves(filtered());
  assert.ok(catalog.some(item => item.key === "portal-ai"));
  assert.ok(catalog.some(item => item.key === "portal-resumes"));
});

test("人事页面保留统一门户目录，花名册默认在职分类且深链仍归属花名册", () => {
  const compact = sidebarNavigation(filtered());
  const hr = compact.find(zone => zone.adminOnly).items.find(item => item.key === 'people-management');
  const roster = hr.children.find(item => item.key === 'hrm-employee');
  assert.equal(roster.to, '/page/hrm-employee?roster=active');
  for (const path of ['/page/hrm-employee', '/page/hrm-employee?employeeId=20', '/page/hrm-employee?roster=left']) {
    const match = findNavigationMatch(compact, path);
    assert.equal(match?.item.key, 'people-management');
    assert.equal(match?.child?.key, 'hrm-employee');
  }
  assert.ok(compact.some(zone => zone.key === 'me'));
  assert.ok(!compact.some(zone => zone.key === 'workspace-switch' || zone.key === 'hr'));
});

test("HR 专用账号只保留人事目录，独立授予的模板查询入口不被合并丢失", () => {
  const hr = sidebarNavigation(filtered({ admin: false, hrOnly: true, permissions: ["can_enter_admin", "hrm:home:hr-query", "hrm:employee:query", "hrm:employee:update", "hrm:contract-template:query", "hrm:recruit:candidate:query", "hrm:recruit:interview:query", "hrm:recruit:post:query"] }));
  assert.equal(hr.length, 1);
  assert.equal(hr[0].items.length, 1);
  assert.equal(hr[0].items[0].key, "people-management");
  assert.equal(hr[0].items[0].children.length, 5);
  const limited = leaves(sidebarNavigation(filtered({ admin: false, permissions: ["can_enter_admin", "hrm:contract-template:query"] })));
  assert.ok(limited.some(item => item.key === "hr-templates"));
  assert.ok(!limited.some(item => item.key === "hr-onboard"));
});
test("相同页面的周报、月报及发声入口只选中最具体的一项", () => {
  for (const [url, parent, title] of [
    ["/me/report?type=3&tab=timeline", "report", "月报"],
    ["/me/report?type=2", "report", "周报"],
    ["/me/profile?tab=voice&channel=ceo", "voice", "CEO信箱"],
    ["/me/profile?tab=footprint", "profile", "操作足迹"],
    ["/me/apply?view=mine&type=supply", "claims", "办公用品申领记录"],
    ["/me/apply?view=mine&type=travel", "apply", "我的申请"],
  ]) {
    const match = findNavigationMatch(navZones, url);
    assert.equal(match?.item.key, parent, url);
    assert.equal(match?.child?.title, title, url);
  }
});
test("物资目录、库存和借还的查询参数分别归属正确菜单", () => {
  for (const [kind, tab, expected] of [
    ["office", "products", "用品目录"],
    ["office", "inventory", "库存管理"],
    ["office", "applies", "领用记录"],
    ["sample", "products", "样品目录"],
    ["sample", "inventory", "台账盘点"],
    ["sample", "applies", "借用归还"],
  ])
    assert.equal(
      findNavigationMatch(navZones, `/portal/materials?kind=${kind}&tab=${tab}`)
        ?.child?.title,
      expected,
    );
  assert.equal(
    findNavigationMatch(navZones, "/portal/materials?kind=office")?.item.key,
    "office-materials",
  );
  assert.equal(
    findNavigationMatch(navZones, "/portal/materials")?.item.key,
    "sample-materials",
  );
});
test("文档编辑深链归属制度目录，旧入口与页面筛选仍能定位父菜单", () => {
  assert.equal(
    findNavigationMatch(navZones, "/me/profile?tab=voice")?.child?.title,
    "建议箱",
  );
  assert.equal(
    findNavigationMatch(navZones, "/portal/materials?kind=office")?.child
      ?.title,
    "库存管理",
  );
  assert.equal(
    findNavigationMatch(navZones, "/portal/materials")?.child?.title,
    "台账盘点",
  );
  assert.equal(
    findNavigationMatch(navZones, "/portal/hr-documents/id/17/edit")?.child
      ?.title,
    "发布制度",
  );
  assert.equal(
    findNavigationMatch(navZones, "/me/report?tab=review")?.item.key,
    "report",
  );
  assert.equal(
    findNavigationMatch(navZones, "/me/rhythm?tab=now")?.item.key,
    "rhythm",
  );
});
test("每个分批上线开关关闭时，移入不同分组的所有对应子项一并隐藏", () => {
  for (const key of policy.configurablePortalKeys) {
    const items = leaves(filtered({ disabled: [key] }));
    assert.equal(
      items.some((item) => policy.portalEntryKey(item) === key),
      false,
      key,
    );
  }
});
test("门户管理员看不到系统账号配置，交叉分组不会扩大原有权限", () => {
  const items = leaves(
    filtered({
      admin: false,
      permissions: ["can_enter_admin", "can_manage_roles"],
    }),
  );
  assert.ok(items.some((item) => item.key === "portal-roles"));
  assert.ok(
    !items.some(
      (item) =>
        item.key === "sys-users" ||
        item.key === "wecom-templates" ||
        item.key === "hrm-group",
    ),
  );
  const createOnly = leaves(
    filtered({ admin: false, permissions: ["can_create_content"] }),
  );
  assert.ok(!createOnly.some((item) => item.key === "portal-news"));
  const ownerOnly = leaves(
    filtered({ admin: false, permissions: ["can_configure_site_owners"] }),
  );
  assert.ok(!ownerOnly.some((item) => item.key === "portal-supplies"));
});
test("分组第一个入口关闭后仍能访问同组其他已上线功能", () => {
  const zones = filtered({ disabled: ["portal-hr-documents"] });
  const group = zones
    .flatMap((zone) => zone.items)
    .find((item) => item.key === "documents");
  assert.equal(group.to, "/portal/signoff");
  assert.equal(group.children.length, 1);
});

test("HR 能进入人事模块，角色配置与全员导出仍独立授权", () => {
  const permissions = ["can_enter_admin", "hrm:home:hr-query", "hrm:employee:query", "hrm:employee:update", "hrm:contract-template:query", "hrm:recruit:candidate:query", "hrm:recruit:interview:query"];
  const items = leaves(filtered({ admin: false, permissions }));
  for (const key of ["hr-workbench", "hr-roster-active", "hr-roster-left", "hr-onboard", "hr-offboard", "hr-templates", "hr-lifecycle", "hrm-employee"])
    assert.ok(items.some(item => item.key === key), key);
  assert.ok(!items.some(item => item.key === "portal-roles" || item.key === "sys-users" || item.key === "org"));
  assert.equal(findNavigationMatch(navZones, "/hr/recruit?tab=interviews")?.child?.key, "hr-interviews");
  assert.equal(findNavigationMatch(navZones, "/page/hrm-employee?roster=left&employeeId=20")?.child?.key, "hr-roster-left");
  const readOnly = leaves(filtered({ admin: false, permissions: ["hrm:employee:query"] }));
  assert.ok(!readOnly.some(item => item.key === "hr-onboard" || item.key === "hr-offboard"));
});

function sessionFor(permissions, roles = ["hr", "employee"]) {
  const stored = new Map();
  const { session } = moduleAt("src/lib/session.ts", {
    vue: { reactive: value => value },
  }, {
    localStorage: {
      getItem: key => stored.get(key) ?? null,
      setItem: (key, value) => stored.set(key, value),
      removeItem: key => stored.delete(key),
    },
  });
  session.save({ token: "navigation-test", userId: 7, name: "人事", permissions, roles });
  return session;
}

function routerFor(session, { disabled = [] } = {}) {
  let guard, routes;
  const router = { beforeEach: callback => { guard = callback; } };
  const login = moduleAt("src/lib/login-target.ts");
  moduleAt("src/router.ts", {
    "vue-router": {
      createWebHistory: () => ({}),
      createRouter: options => { routes = options.routes; return router; },
    },
    "./lib/session": { session },
    "./configs/pages": pages,
    "./configs/portal": portal,
    "./lib/portal-access": {
      hasPortalPermission: item =>
        (!item.permission || session.has(item.permission)) &&
        (!item.anyPermissions?.length || item.anyPermissions.some(p => session.has(p))) &&
        (!item.allPermissions?.length || item.allPermissions.every(p => session.has(p))),
    },
    "./lib/portal-rollout": {
      loadPortalRollout: async () => {},
      portalEntryEnabled: key => !disabled.includes(key),
    },
    "./lib/portal-entry-policy": policy,
    "./lib/login-target": login,
  });
  return {
    home: () => routes.find(route => route.path === "/").children.find(route => route.path === "").redirect(),
    navigate: (target, overrides = {}) => {
      const url = new URL(target, "http://oa.local");
      return guard({ path: url.pathname, fullPath: target, query: Object.fromEntries(url.searchParams), meta: {}, params: {}, ...overrides });
    },
  };
}

test("HR 专用身份回到已授权首页，缺少工作台权限也不会落到个人区或空管理页", () => {
  for (const [permission, target] of [
    ["hrm:home:hr-query", "/hr/workbench"],
    ["hrm:recruit:candidate:query", "/hr/recruit?tab=candidates"],
    ["hrm:recruit:interview:query", "/hr/recruit?tab=interviews"],
    ["hrm:recruit:post:query", "/hr/recruit?tab=posts"],
    ["hrm:employee:query", "/page/hrm-employee?roster=active"],
    ["hrm:contract-template:query", "/hr/templates"],
    ["can_manage_resumes", "/portal/resumes"],
    ["can_generate_contracts", "/portal/contracts"],
  ]) {
    const session = sessionFor(["can_enter_admin", permission]);
    assert.equal(session.consoleHome, target, permission);
    assert.equal(routerFor(session).home(), target, permission);
  }
  assert.equal(sessionFor(["can_enter_admin"]).consoleHome, "/forbidden");
  assert.equal(sessionFor(["hrm:home:hr-query"]).consoleHome, "/forbidden");
});

test("HR 专用账号的个人区和非人事旧书签被重定向，保留登录交接和错误恢复页", async () => {
  const session = sessionFor(["can_enter_admin", "hrm:contract-template:query"]);
  const router = routerFor(session);
  for (const target of ["/me/overview", "/me/profile?tab=voice", "/me/team", "/Me/Profile/", "/portal", "/portal?view=tools", "/portal/news", "/portal/roles", "/admin", "/org", "/page/hrm-group"]) {
    assert.equal(await router.navigate(target), "/hr/templates", target);
  }
  for (const target of ["/forbidden", "/portal/unavailable?from=/portal/resumes", "/login?wecom_code=test"]) {
    assert.equal(await router.navigate(target), undefined, target);
  }
  const empty = routerFor(sessionFor(["can_enter_admin"]));
  assert.equal(await empty.navigate("/me/profile"), "/forbidden");
  assert.equal(await empty.navigate("/forbidden"), undefined);
});

test("HR 保留招聘和合同融合深链，仍校验业务权限与入口上线状态", async () => {
  const permissions = ["can_enter_admin", "hrm:home:hr-query", "hrm:employee:query", "hrm:employee:update", "hrm:contract-template:query", "hrm:recruit:candidate:query", "hrm:recruit:interview:query", "can_manage_resumes", "can_generate_contracts"];
  const session = sessionFor(permissions), router = routerFor(session);
  for (const target of ["/hr/workbench", "/hr/recruit?tab=candidates", "/hr/recruit?tab=interviews", "/page/hrm-employee?roster=left&employeeId=20", "/hr/employment?mode=offboard", "/hr/templates", "/hr/lifecycle?employeeId=20", "/portal/resumes", "/portal/contracts"]) {
    assert.equal(await router.navigate(target), undefined, target);
  }
  assert.equal(await router.navigate("/hr/recruit?tab=posts"), "/forbidden");
  assert.equal(await router.navigate("/HR/RECRUIT/?tab=posts"), "/forbidden");
  const limited = routerFor(sessionFor(["can_enter_admin", "hrm:home:hr-query"]));
  assert.equal(await limited.navigate("/portal/resumes"), "/forbidden");
  assert.equal(await limited.navigate("/portal/contracts"), "/forbidden");
  assert.equal(await limited.navigate("/hr/templates", { meta: { permission: "hrm:contract-template:query" } }), "/forbidden");
  const closed = await routerFor(session, { disabled: ["portal-resumes"] }).navigate("/portal/resumes");
  assert.equal(closed.path, "/portal/unavailable");
  assert.equal(closed.query.from, "/portal/resumes");
});

test("HR 专用限制不改变员工、系统管理员和复合岗位的既有导航", async () => {
  for (const roles of [["employee"], ["admin", "hr"], ["hr", "operations"]]) {
    const session = sessionFor(["can_enter_admin"], roles);
    assert.equal(session.isHrOnly, false, roles.join(","));
    assert.equal(session.hrOnlyRedirect("/me/profile"), undefined);
    assert.equal(await routerFor(session).navigate("/me/profile"), undefined);
  }
  const wildcard = sessionFor(["*:*:*"]);
  assert.equal(wildcard.isHrOnly, false);
  assert.equal(wildcard.consoleHome, "/admin");
  const employee = sessionFor([], ["employee"]);
  assert.equal(routerFor(employee).home(), "/me/overview");
});
