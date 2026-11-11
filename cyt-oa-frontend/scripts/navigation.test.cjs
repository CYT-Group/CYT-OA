const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const root = path.join(__dirname, "..");
function moduleAt(file, dependencies = {}) {
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
const { findNavigationMatch } = moduleAt("src/lib/navigation.ts");
const policy = moduleAt("src/lib/portal-entry-policy.ts", {
  "../configs/portal": portal,
});
const leaves = (zones) =>
  zones.flatMap((zone) =>
    zone.items.flatMap((item) => item.children ?? [item]),
  );
function filtered({ admin = true, permissions, disabled = [] } = {}) {
  const access = moduleAt("src/lib/portal-access.ts", {
    "./session": {
      session: {
        isAdmin: admin,
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
