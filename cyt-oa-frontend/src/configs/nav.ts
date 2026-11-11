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
  requiresTeam?: boolean;
  requiresApprover?: boolean;
  requiresMaterialsApprover?: boolean;
  aliases?: string[];
  /** 已有页签的快捷入口保留在搜索中，侧栏归并到同一业务入口。 */
  sidebarParent?: string;
  section?: string;
}
export interface NavItem extends NavSub {
  key: string;
  icon: string;
  badge?: string;
  children?: NavSub[];
  automatic?: boolean;
  /** 工具入口直接打开总览，完整目录仍供总览、搜索和面包屑使用。 */
  sidebarLink?: boolean;
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
    aliases: ["/admin"],
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
      { title: "待我审批", glyph: "📥", to: "/me/requests?view=pending", requiresApprover: true },
      { title: "门户申请", glyph: "📑", to: "/me/requests?view=mine" },
    ],
    ["/me/apply", "/me/requests"],
  ),
  group("claims", "申领记录", "📦", [
    {
      title: "办公用品申领记录",
      glyph: "🗂",
      to: "/me/records?tab=office",
      aliases: ["/me/apply?view=mine&type=supply"],
    },
    { title: "样品申领记录", glyph: "🧪", to: "/me/records?tab=sample" },
    { title: "审批记录", glyph: "📒", to: "/me/records?tab=approvals", requiresMaterialsApprover: true },
  ]),
  {
    ...group(
      "report",
      "报告中心",
      "📊",
      [
        {
          key: "report-weekly",
          title: "周报",
          glyph: "📅",
          to: "/me/report?type=2",
        },
        { title: "月报", glyph: "🗓", to: "/me/report?type=3" },
        { title: "述职报告", glyph: "🎤", to: "/me/review-reports" },
        {
          title: "日报",
          icon: "file",
          to: "/me/report?type=1",
          sidebarParent: "report",
        },
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
        { title: "团队祝贺", glyph: "🎉", to: "/me/team?tab=milestone" },
        { title: "团队成员", glyph: "👥", to: "/me/team?tab=members" },
        { title: "团队考勤", glyph: "⏰", to: "/me/team-attendance" },
        { title: "团队审批", glyph: "📥", to: "/me/requests?view=team" },
        { title: "待我点评", icon: "message", to: "/me/team?tab=review", sidebarParent: "team" },
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
        to: "/me/voice?channel=suggestion",
        aliases: ["/me/profile?tab=voice", "/me/profile?tab=voice&channel=suggestion"],
      },
      {
        title: "CEO信箱",
        glyph: "✈️",
        to: "/me/voice?channel=ceo",
        aliases: ["/me/profile?tab=voice&channel=ceo"],
      },
      { title: "匿名吐槽", glyph: "📢", to: "/me/voice?channel=anon" },
      { title: "满意度调研", glyph: "✅", to: "/me/voice?channel=survey" },
    ],
    ["/me/profile?tab=voice"],
  ),
  group("okr", "目标·OKR", "🎯", [
    { title: "我的目标", glyph: "🎯", to: "/me/okr?tab=my" },
    { title: "对齐视图", glyph: "🔗", to: "/me/okr?tab=align" },
  ], ["/me/okr"]),
  group("profile", "我的资料", "👤", [
    { key: "profile-base", title: "基本信息", glyph: "📋", to: "/me/profile" },
    { title: "我的祝贺", glyph: "🎉", to: "/me/records?tab=celebrations" },
    { title: "考勤与请假", glyph: "⏰", to: "/me/records?tab=attendance" },
    { title: "培训记录", glyph: "📚", to: "/me/records?tab=training" },
    { title: "操作足迹", glyph: "👣", to: "/me/profile?tab=footprint" },
    {
      title: "提醒偏好",
      icon: "bell",
      to: "/me/profile?tab=preference",
      sidebarParent: "profile",
    },
    {
      title: "收到的认可",
      icon: "heart",
      to: "/me/profile?tab=recognition",
      sidebarParent: "profile",
    },
    { key: "reminder", title: "我的提醒", icon: "bell", to: "/me/reminder" },
  ]),
];

const management: NavItem[] = [
  group("permissions", "权限管理", "🔐", [
    existing("portal-roles", { title: "账号与权限", glyph: "🎭" }),
    existing("sys-users", { title: "账号分配", glyph: "🧩" }),
    existing("portal-workspace-rules", { title: "数据权限", glyph: "🗄", to: "/portal/workspace-rules?section=scope", systemOnly: true }),
  ]),
  group("approval-templates", "审批流程配置", "✅", [
    existing("wecom-templates", { title: "流程模板", glyph: "🔀" }),
    existing("portal-workspace-rules", { title: "节点与规则", glyph: "⚙️", to: "/portal/workspace-rules?section=flow&tab=leave" }),
  ]),
  group("attendance-rules", "考勤与报销规则", "📊", [
    existing("portal-workspace-rules", { title: "考勤规则", glyph: "⏰", to: "/portal/workspace-rules?tab=attendance" }),
    existing("portal-workspace-rules", { title: "请假额度", glyph: "🏖", to: "/portal/workspace-rules?tab=leave" }),
    existing("portal-workspace-rules", { title: "报销标准", glyph: "💰", to: "/portal/workspace-rules?tab=expense" }),
    existing("hrm-group", { title: "打卡排班", glyph: "⏱" }),
    existing("hrm-clock", { sidebarParent: "hrm-group" }),
    existing("hrm-holiday", { sidebarParent: "hrm-group" }),
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
  group("voice-management", "发声管理", "🗣", [
    existing("portal-voice", { title: "建议箱管理", glyph: "📬", to: "/portal/voice?tab=suggestion" }),
    existing("portal-voice", { title: "CEO信箱管理", glyph: "✈️", to: "/portal/voice?tab=ceo", systemOnly: true }),
    existing("portal-voice", { title: "吐槽审核", glyph: "📢", to: "/portal/voice?tab=anon" }),
    existing("portal-voice", { title: "问卷配置", glyph: "📝", to: "/portal/voice?tab=questionnaire" }),
    existing("portal-voice", { title: "发布管理", glyph: "🚀", to: "/portal/voice?tab=release" }),
    existing("portal-voice", { title: "调研结果", glyph: "📊", to: "/portal/voice?tab=results" }),
  ], ["/portal/voice"]),
  group("celebrations", "祝贺管理", "🎉", [
    existing("portal-celebrations", { title: "生日祝贺", glyph: "🎂", to: "/portal/celebrations?tab=settings&template=birthday" }),
    existing("portal-celebrations", { title: "入职周年祝贺", glyph: "🎊", to: "/portal/celebrations?tab=settings&template=anniversary" }),
    existing("portal-celebrations", { title: "祝贺日历", glyph: "🗓", sidebarParent: "portal-celebrations" }),
    existing("portal-employee-info", {
      title: "员工与祝贺偏好",
      glyph: "👥",
      sidebarParent: "portal-celebrations",
    }),
  ]),
  group("people-management", "人事管理", "👥", [
    {
      key: "hr-workbench",
      title: "人事工作台",
      glyph: "🏠",
      to: "/hr/workbench",
      permission: "hrm:home:hr-query",
    },
    {
      key: "hr-candidates",
      title: "招聘管理",
      glyph: "🧑‍💼",
      to: "/hr/recruit?tab=candidates",
      permission: "hrm:recruit:candidate:query",
    },
    {
      key: "hr-interviews",
      title: "面试安排与记录",
      glyph: "🗓",
      to: "/hr/recruit?tab=interviews",
      permission: "hrm:recruit:interview:query",
      sidebarParent: "hr-candidates",
    },
    {
      key: "hr-posts",
      title: "招聘职位",
      glyph: "📋",
      to: "/hr/recruit?tab=posts",
      permission: "hrm:recruit:post:query",
      sidebarParent: "hr-candidates",
    },
    existing("portal-resumes", { sidebarParent: "hr-candidates" }),
    existing("hrm-employee", {
      title: "花名册",
      glyph: "📒",
      to: "/page/hrm-employee?roster=active",
      aliases: ["/page/hrm-employee"],
      permission: "hrm:employee:query",
      systemOnly: false,
    }),
    {
      key: "hr-roster-active",
      title: "在职员工",
      glyph: "👥",
      to: "/page/hrm-employee?roster=active",
      permission: "hrm:employee:query",
      sidebarParent: "hrm-employee",
    },
    {
      key: "hr-roster-left",
      title: "离职员工",
      glyph: "🚪",
      to: "/page/hrm-employee?roster=left",
      permission: "hrm:employee:query",
      sidebarParent: "hrm-employee",
    },
    {
      key: "hr-onboard",
      title: "入离职与合同",
      glyph: "📄",
      to: "/hr/employment?mode=onboard",
      permission: "hrm:employee:update",
      allPermissions: ["hrm:employee:query", "hrm:contract-template:query"],
    },
    {
      key: "hr-offboard",
      title: "离职办理",
      glyph: "📤",
      to: "/hr/employment?mode=offboard",
      permission: "hrm:employee:update",
      allPermissions: ["hrm:employee:query", "hrm:contract-template:query"],
      sidebarParent: "hr-onboard",
    },
    {
      key: "hr-templates",
      title: "合同模板",
      glyph: "📑",
      to: "/hr/templates",
      permission: "hrm:contract-template:query",
      sidebarParent: "hr-onboard",
    },
    existing("portal-contracts", { sidebarParent: "hr-onboard" }),
    {
      key: "hr-lifecycle",
      title: "员工生命周期",
      glyph: "🔄",
      to: "/hr/lifecycle",
      permission: "hrm:employee:query",
    },
  ]),
];

// 辅助工具统一归入总览，避免把旧目录再次追加到侧栏。
const toolSection = (key: string) => {
  if (
    [
      "portal-wiki",
      "portal-brand-docs",
      "portal-training",
      "portal-categories",
    ].includes(key)
  )
    return "内容与培训";
  if (["portal-ai", "portal-api-costs", "portal-analytics"].includes(key))
    return "数据与 AI";
  if (key === "portal-import") return "批量导入";
  if (key.startsWith("oa-")) return "原有业务台账";
  return "系统设置";
};
const tools: NavSub[] = [
  existing("portal-hub", { title: "管理工具总览", to: "/portal?view=tools" }),
  ...menuGroups.flatMap((source) =>
    source.items
      .filter((item) => !consumed.has(item.key))
      .map((item) => ({
        ...item,
        to: leafTarget(item),
        systemOnly: !source.portal,
        section: toolSection(item.key),
      })),
  ),
  {
    key: "console",
    title: "企业工作台",
    icon: "trending-up",
    to: "/admin",
    systemOnly: true,
    section: "系统设置",
  },
];
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
        ...group("management-tools", "管理工具", "⚙️", tools),
        sidebarLink: true,
      },
    ],
  },
];
