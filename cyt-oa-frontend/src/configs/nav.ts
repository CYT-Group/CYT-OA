import { menuGroups } from "./pages";
import type { MenuLeaf } from "./types";

export interface NavSub {
  key?: string;
  title: string;
  to: string;
  icon?: string;
  glyph?: string;
  permission?: string;
  anyPermissions?: string[];
  allPermissions?: string[];
  systemOnly?: boolean;
  aliases?: string[];
}
export interface NavItem extends NavSub {
  key: string;
  icon: string;
  badge?: string;
  children?: NavSub[];
  requiresTeam?: boolean;
  automatic?: boolean;
}
export interface NavZone {
  key: string;
  title: string;
  glyph?: string;
  items: NavItem[];
  adminOnly?: boolean;
}

const consumed = new Set<string>();
const leafTarget = (item: MenuLeaf) => item.to ?? `/page/${item.key}`;
// 目录只改变呈现，权限、分批上线 key 和业务地址继续来自原菜单声明。
function existing(key: string, overrides: Partial<NavSub> = {}): NavSub {
  for (const group of menuGroups) {
    const item = group.items.find((item) => item.key === key);
    if (item) {
      consumed.add(key);
      return {
        ...item,
        to: leafTarget(item),
        systemOnly: !group.portal,
        ...overrides,
        allPermissions:
          item.permission &&
          overrides.permission &&
          item.permission !== overrides.permission
            ? [item.permission]
            : overrides.allPermissions,
      };
    }
  }
  throw new Error(`导航未找到现有功能：${key}`);
}
function group(
  key: string,
  title: string,
  glyph: string,
  children: NavSub[],
  aliases: string[] = [],
): NavItem {
  return {
    key,
    title,
    glyph,
    icon: "folder",
    to: children[0]!.to,
    children,
    aliases,
  };
}

const personal: NavItem[] = [
  {
    key: "overview",
    title: "首页",
    icon: "home",
    glyph: "🏠",
    to: "/me/overview",
  },
  group(
    "rhythm",
    "大促·新品",
    "🎪",
    [
      {
        title: "大促日历",
        glyph: "🗓",
        to: "/me/rhythm?tab=calendar&kind=sale",
      },
      { title: "新品上架", glyph: "🆕", to: "/me/rhythm?tab=all&kind=new" },
    ],
    ["/me/rhythm"],
  ),
  group(
    "apply",
    "申请与审批",
    "📝",
    [
      { title: "我要办事", glyph: "📤", to: "/me/apply?view=start" },
      { title: "我的申请", glyph: "📌", to: "/me/apply?view=mine" },
      { title: "历史记录", glyph: "✅", to: "/me/apply?view=history" },
    ],
    ["/me/apply"],
  ),
  group("claims", "申领记录", "📦", [
    {
      title: "办公用品申领记录",
      glyph: "🗂",
      to: "/me/apply?view=mine&type=supply",
    },
  ]),
  {
    ...group(
      "report",
      "报告中心",
      "📊",
      [
        { title: "周报", glyph: "📅", to: "/me/report?type=2" },
        { title: "月报", glyph: "🗓", to: "/me/report?type=3" },
        { title: "日报", icon: "file", to: "/me/report?type=1" },
      ],
      ["/me/report"],
    ),
    badge: "report",
  },
  {
    ...group(
      "team",
      "团队数据",
      "👥",
      [
        { title: "团队看板", glyph: "📊", to: "/me/team?tab=board" },
        { title: "团队成员", glyph: "👥", to: "/me/team?tab=members" },
        { title: "待我点评", icon: "message", to: "/me/team?tab=review" },
      ],
      ["/me/team"],
    ),
    requiresTeam: true,
    automatic: true,
  },
  group(
    "voice",
    "我要发声",
    "🗣",
    [
      {
        title: "建议箱",
        glyph: "📬",
        to: "/me/profile?tab=voice&channel=suggestion",
        aliases: ["/me/profile?tab=voice"],
      },
      {
        title: "CEO信箱",
        glyph: "✈️",
        to: "/me/profile?tab=voice&channel=ceo",
      },
    ],
    ["/me/profile?tab=voice"],
  ),
  group("profile", "我的资料", "👤", [
    { title: "基本信息", glyph: "📋", to: "/me/profile" },
    { title: "操作足迹", glyph: "👣", to: "/me/profile?tab=footprint" },
    { title: "提醒偏好", icon: "bell", to: "/me/profile?tab=preference" },
    { title: "收到的认可", icon: "heart", to: "/me/profile?tab=recognition" },
  ]),
  {
    key: "reminder",
    title: "我的提醒",
    icon: "bell",
    to: "/me/reminder",
    badge: "reminder",
  },
];

const management: NavItem[] = [
  group("permissions", "权限管理", "🔐", [
    existing("portal-roles", { title: "账号与权限", glyph: "🎭" }),
    existing("sys-users", { title: "账号分配", glyph: "🧩" }),
  ]),
  group("approval-templates", "审批流程配置", "✅", [
    existing("wecom-templates", { title: "流程模板", glyph: "🔀" }),
  ]),
  group("attendance-rules", "考勤与报销规则", "📊", [
    existing("hrm-group", { title: "考勤规则", glyph: "⏰" }),
  ]),
  group(
    "office-materials",
    "办公用品管理",
    "📦",
    [
      existing("portal-supplies", {
        title: "用品目录",
        glyph: "📦",
        to: "/portal/materials?kind=office&tab=products",
      }),
      existing("portal-supplies", {
        title: "库存管理",
        glyph: "🏷",
        to: "/portal/materials?kind=office&tab=inventory",
        aliases: ["/portal/materials?kind=office"],
      }),
      existing("portal-supplies", {
        title: "领用记录",
        glyph: "📋",
        to: "/portal/materials?kind=office&tab=applies",
      }),
      existing("portal-supplies", {
        title: "站点负责人",
        icon: "users",
        to: "/portal/materials?kind=office&tab=owners",
        permission: "can_configure_site_owners",
      }),
    ],
    ["/portal/materials?kind=office"],
  ),
  group(
    "sample-materials",
    "样品管理",
    "🧪",
    [
      existing("portal-samples", {
        title: "样品目录",
        glyph: "🧪",
        to: "/portal/materials?kind=sample&tab=products",
      }),
      existing("portal-samples", {
        title: "借用归还",
        glyph: "🔄",
        to: "/portal/materials?kind=sample&tab=applies",
      }),
      existing("portal-samples", {
        title: "台账盘点",
        glyph: "📒",
        to: "/portal/materials?kind=sample&tab=inventory",
        aliases: ["/portal/materials?kind=sample", "/portal/materials"],
      }),
    ],
    ["/portal/materials?kind=sample", "/portal/materials"],
  ),
  group("company-news", "公司动态发布", "📰", [
    existing("portal-news", {
      title: "发布动态",
      glyph: "✍️",
      to: "/portal/news?action=create",
      permission: "can_create_content",
    }),
    existing("portal-news", { title: "动态管理", glyph: "📰" }),
  ]),
  group("documents", "制度发布与签收", "📑", [
    existing("portal-hr-documents", { title: "发布制度", glyph: "📑" }),
    existing("portal-signoff", { title: "签收统计", glyph: "📈" }),
  ]),
  group("business-rhythm", "大促与新品管理", "🎪", [
    existing("portal-events", { title: "活动排期上传", glyph: "🗓" }),
    existing("portal-products", { title: "新品链接维护", glyph: "🆕" }),
  ]),
  group("people-management", "人事管理", "👥", [
    existing("org"),
    existing("hrm-employee"),
    existing("hrm-clock"),
    existing("hrm-holiday"),
  ]),
];

// 设计稿没有的能力保持原有分组、名称与 Lucide 图标。
const remaining: NavItem[] = menuGroups.flatMap((group) => {
  const items = group.items.filter((item) => !consumed.has(item.key));
  return items.length
    ? [
        {
          key: group.group,
          title: group.group,
          icon: items[0]!.icon,
          to: leafTarget(items[0]!),
          systemOnly: !group.portal,
          children: items.map((item) => ({ ...item, to: leafTarget(item) })),
        },
      ]
    : [];
});
export const navZones: NavZone[] = [
  { key: "me", title: "个人办公", items: personal },
  {
    key: "admin",
    title: "管理中心",
    glyph: "⚙️",
    adminOnly: true,
    items: [
      ...management,
      {
        key: "console",
        title: "企业工作台",
        icon: "trending-up",
        to: "/admin",
        systemOnly: true,
      },
      ...remaining,
    ],
  },
];
