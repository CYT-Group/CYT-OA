/**
 * 前端共享类型 — 与 backend/schemas.py / 各 router 序列化字段对齐。
 *
 * 约定：
 * - 字段名以后端返回为准（snake_case），历史踩坑字段（name/title、dept/department、
 *   content_url/doc_url）在注释里标明，避免再犯。
 * - 后端新增字段时同步补这里；不确定的字段一律 optional。
 */
// 只借封面配置的形状（纯类型 import，无运行时依赖；data/hr-cover.ts 不反向依赖本文件）
import type { HrCover } from './data/hr-cover'

// ── 角色与用户 ──
/**
 * 角色 key。角色已数据化（portal_roles 表支持自定义角色，口径-角色权限动态化），
 * 不再是封闭联合类型；内置 8 角色 admin/management/pm/market/product/ops/finance/viewer
 * 只是 seed 基线。权限判断一律走能力位（policies.ts / capabilities），不要比对角色字符串。
 */
export type Role = string

export interface PortalUser {
  username: string
  display_name?: string
  /** 前端登录时补写的驼峰别名（LoginView / auth store） */
  displayName?: string
  role?: Role
  /** 多部门数组（主部门排第 0 位）；老缓存/旧后端是单字符串——读取一律走 utils/dept 的 deptList() */
  department?: string[] | string | null
  /** 企微部门 id 数组（事实源）；SSO 陈旧缓存可能缺失，auth store init 拉 /me 后治愈 */
  department_ids?: number[] | null
  /** 后端 UserOut 派生：主部门名（department 第 0 位） */
  main_department?: string | null
  /**
   * 能力位列表（后端按角色矩阵 ∪ 特殊规则算好下发，含 admin 恒真展开）。
   * 旧后端响应无此字段——policies.ts 对缺失值回退本地角色集判断（灰度期兜底）。
   */
  capabilities?: string[]
  email?: string
  phone?: string
  avatar_url?: string
  /** 前端本地登录态过期戳（ms），非后端字段 */
  exp?: number
}

// ── 角色权限动态化（GET /roles、GET /roles/permissions；口径-角色权限动态化-后端.md §5）──
/**
 * 角色行（portal_roles）：is_system=系统角色不可删（v1.2 迁移后仅 admin/viewer 为 true，
 * 其余老内置角色收缩为可删的普通角色）；前端只认这个字段，不写死角色名清单。
 * user_count 由列表接口聚合。
 */
export interface RoleInfo {
  key: string
  name: string
  description?: string | null
  is_system?: boolean
  sort_order?: number
  user_count?: number
  created_at?: string
  updated_at?: string
  [key: string]: unknown
}

/** 能力注册表条目（代码唯一真相 domain/permissions_registry.py 的序列化） */
export interface PermissionInfo {
  key: string
  name: string
  description?: string | null
  /** 分组标题（内容与审批 / 用户与角色 / ...），单角色面板按组为行渲染 */
  group: string
  /**
   * 操作维度（view/create/edit/review/delete/manage/other），单角色面板按此摆列。
   * 旧后端条目无此字段——前端兜底归「其他」列（灰度安全，不自建映射）。
   */
  action?: string
  /** 危险能力（UI 红点）：can_delete / can_import / can_manage_users 等 */
  dangerous?: boolean
  /** 锁定行不可配（can_manage_roles 固定仅 admin） */
  locked?: boolean
  default_roles?: string[]
  [key: string]: unknown
}

/** GET /roles/permissions 的 data：注册表全量 + 当前矩阵（role_key → 能力 key 数组） */
export interface RoleMatrix {
  permissions: PermissionInfo[]
  matrix: Record<string, string[]>
  [key: string]: unknown
}

/**
 * GET /auth/users 行（用户管理后台 / 角色成员区）：routers/auth.list_users 序列化。
 * is_active 已随 2026-09-03 全量口径下发（含停用账号）；灰度兜底：旧后端无此字段时不渲染停用徽标。
 */
export interface AdminUserItem {
  id: number
  username: string
  display_name?: string | null
  role?: string
  department?: string[] | string | null
  is_active?: boolean
  [key: string]: unknown
}

/** 员工页的只读部门上下文，两种管理能力均可读取，不包含日期或角色。 */
export interface EmployeeDepartment {
  id: number
  name: string
  parent_id: number
  sort_order: number
  is_active: boolean
}
export interface EmployeeDepartmentMembership {
  username: string
  /** 与 department 同序，主部门排第一；非空 ID 为归属事实源。 */
  department_ids: number[]
  department: string[]
}
export interface EmployeeDepartmentFilterContext {
  departments: EmployeeDepartment[]
  memberships: EmployeeDepartmentMembership[]
}

// ── 响应信封（axios 拦截器解包后调用方拿到的形状）──
export interface ApiEnvelope<T> {
  success: boolean
  data: T
}

export interface ListEnvelope<T> {
  success: boolean
  data: T[]
  /** news/wiki/feedback 有真分页；training/events/products 只截断无 total */
  total?: number
  page?: number
  limit?: number
}

export type ContentStatus = 'draft' | 'pending' | 'published' | 'rejected'

// ── 公司动态 ──（routers/news.py 列表序列化字段）
export interface NewsItem {
  id: number
  post_id?: number
  title: string
  summary?: string | null
  excerpt?: string
  category?: string
  cover_image?: string | null
  template_type?: string
  blocks?: Record<string, unknown> | null
  author?: string
  author_username?: string
  /** 取自 blocks.dept，跨部门通知卡片用 */
  dept?: string | null
  published_at?: string | null
  is_featured?: boolean
  is_pinned?: boolean
  status?: ContentStatus
  /** 详情接口才有正文 */
  content?: string
}

// ── 展会 / 日程 ──
export interface EventItem {
  id: number
  title: string
  location?: string | null
  event_type?: string | null
  start_date?: string | null
  end_date?: string | null
  status?: ContentStatus
  description?: string
}

// ── 知识库 ──
export interface WikiItem {
  id: number
  title: string
  category?: string
  department?: string
  summary?: string
  /** ⚠️ 历史坑：文档链接字段是 doc_url（不是 content_url） */
  doc_url?: string
  view_count?: number
  status?: ContentStatus
  author?: string
}

// ── 培训课程（老 GET /training 列表行，legacy training.html 仍在用；新培训中心看下面的 Training* 系列）──
export interface TrainingItem {
  id: number
  title: string
  department?: string
  level?: string
  description?: string
  status?: ContentStatus
}

// ── 培训中心 v1（2026-09 重做）——契约见 doc_ww/口径-培训中心-后端.md §4，键名照抄 ──
// 资料在 NAS MinIO，四种课时形式 + 老数据外链；文件不经后端，前端拿预签名 URL 直连。
/** 课时形态：视频 mp4 / 单 PDF / 图文（自包含 output.html）/ 图卡（图片序列翻页）/ 外链（老数据迁移来的） */
export type TrainingLessonKind = 'video' | 'pdf' | 'article' | 'cards' | 'link'
/** 课程级别：v1 只剩必修/选修两值（老三值 optional/advanced 已迁成 elective） */
export type TrainingLevel = 'required' | 'elective'
/** 课程状态：老审批流 pending/rejected 已并入 draft */
export type TrainingCourseStatus = 'draft' | 'published'

/** GET /training/catalog 里每门课的卡片字段（员工端只见 published 且 dept_scope 命中的课） */
export interface TrainingCourseCard {
  id: number
  title: string
  description?: string | null
  cover_image?: string | null
  level?: TrainingLevel | string
  instructor?: string | null
  lesson_count: number
  /** 全部视频课时时长之和（秒）；没有视频的课为 0/null */
  duration_total_sec?: number | null
  /** 本人进度 0-100（按已完成课时数折算） */
  progress_percent?: number
  completed?: boolean
  [key: string]: unknown
}

/** GET /training/catalog 的 data：按小类（=NAS 顶层主题目录，字段名仍叫 category）分组 */
export interface TrainingCatalogGroup {
  category: string
  courses: TrainingCourseCard[]
}

// ── 大类（v1.3：大类 → 小类 → 课程 → 课时四层）契约见 口径-培训中心-后端.md §9.3 ──
/**
 * GET /training/groups 的行：只含有可见课程的大类。
 * ⚠ key 可能是 null——未归类小类若有可见课程，后端补一条 `{key:null, name:'未分类'}` 的伪大类；
 * 跳 `/training/group/:key` 与查 catalog 时都换成保留字 `__unassigned__`（见 views/training/trainingGroupKey.ts）。
 */
export interface TrainingGroup {
  key: string | null
  name: string
  /** emoji，可空 */
  icon?: string | null
  description?: string | null
  /** 该大类下有可见课程的小类数 */
  category_count: number
  course_count: number
  /** 小类名列表，供入口页 chips 预览 */
  categories: string[]
  [key: string]: unknown
}

/** 小类归属行（portal_training_categories）：name 是小类名本身，同时是业务主键 */
export interface TrainingCategoryRow {
  name: string
  course_count: number
  sort_order: number
  /** 所属大类 key；null/缺省 = 未归类 */
  group_key?: string | null
  [key: string]: unknown
}

/** 大类行——增删改端点的回包形状（与 admin/groups 里的大类块同键，少 categories） */
export interface TrainingGroupRow {
  key: string
  name: string
  icon?: string | null
  description?: string | null
  sort_order?: number
  [key: string]: unknown
}

/** GET /training/admin/groups 里的大类块：含空大类，小类是带计数的对象而不是名字串 */
export interface TrainingAdminGroupRow extends TrainingGroupRow {
  category_count?: number
  course_count?: number
  categories: TrainingCategoryRow[]
}

/** GET /training/admin/groups：全部大类 + 顶层未归类小类 */
export interface TrainingAdminGroups {
  groups: TrainingAdminGroupRow[]
  unassigned: TrainingCategoryRow[]
  [key: string]: unknown
}

/** POST /training/admin/groups：key 建后不可改（slug ^[a-z][a-z0-9_-]{1,31}$，撞 key 409） */
export interface TrainingGroupInput {
  key: string
  name: string
  icon?: string | null
  description?: string | null
  sort_order?: number
}

/** PUT /training/admin/groups/{key}：PATCH 语义，只发改动键；key 不在 body 里 */
export interface TrainingGroupPatch {
  name?: string
  icon?: string | null
  description?: string | null
  sort_order?: number
}

/**
 * PUT /training/admin/categories：改小类归属/排序。
 * ⚠ name 走 body 不走 path——小类名含中文与空格，塞 URL 会踩编码坑（口径 §9.3）；group_key 传 null 是取消归类。
 */
export interface TrainingCategoryPatch {
  name: string
  group_key?: string | null
  sort_order?: number
}

/** 课时级本人进度（portal_training_lesson_progress 行的裁剪）；从未打开过的课时为 null */
export interface TrainingLessonProgress {
  status?: 'started' | 'completed' | string
  /** 视频=秒 / 图卡与图片型图文=图索引 / PDF=页（v1 不用） */
  position?: number | null
  percent?: number
  [key: string]: unknown
}

/** 课程详情里的课时行——不含对象键，媒体地址另走 media 端点拿预签名 */
export interface TrainingLesson {
  id: number
  title: string
  kind: TrainingLessonKind | string
  sort_order: number
  /** 视频时长（首次心跳回报后才有） */
  duration_sec?: number | null
  size_bytes?: number | null
  /** html 型 article 课时是否附带同目录 PDF（图片型恒为 false） */
  has_pdf?: boolean
  /** cards / 图片型 article 课时的图片张数 */
  page_count?: number | null
  my?: TrainingLessonProgress | null
  [key: string]: unknown
}

/** GET /training/{id}：课程 + 课时列表（课程须 published 且 dept_scope 命中，否则 404） */
export interface TrainingCourseDetail {
  id: number
  title: string
  description?: string | null
  cover_image?: string | null
  category?: string | null
  level?: TrainingLevel | string
  instructor?: string | null
  status?: TrainingCourseStatus | string
  lessons: TrainingLesson[]
  [key: string]: unknown
}

/**
 * GET /training/lessons/{id}/media：按 kind 形状不同，统一成一个宽接口——
 * video/pdf → {url, expires_in}；article html 型 → {url, pdf_url?, expires_in}、图片型 → 与 cards 同形 {pages, expires_in}；
 * link → {url}（外链不过期）。
 * ⚠ 预签名 URL 短时效，只放组件内存，禁止写 localStorage（口径 §2/§4 红线）
 */
export interface TrainingLessonMedia {
  url?: string
  pdf_url?: string | null
  pages?: string[]
  /** 预签名剩余秒数；link 课时没有 */
  expires_in?: number
  [key: string]: unknown
}

/** POST /training/lessons/{id}/progress 请求体：percent 后端只增不减；完成阈值 video ≥90 / 其余 ≥100 */
export interface TrainingProgressPayload {
  percent: number
  position?: number
  duration_sec?: number
}

/**
 * 课时组件 emit 的 progress 事件 = 请求体 + 归属课时 id。
 * 身份必须在 emit 侧绑定：课时组件按 :key=lesson.id 切换，旧组件在 onBeforeUnmount 里补报最后进度时
 * 父页面的 current 已经指向新课时，靠父页面「当前课时」归属会把 A 的进度记到 B 头上。
 */
export interface TrainingProgressEvent extends TrainingProgressPayload {
  lesson_id: number
}

// ── 培训中心管理端（can_manage_training）──
/** GET /training/admin/courses 行：全部课程（含 draft）+ lesson_count */
export interface TrainingAdminCourse {
  id: number
  title: string
  description?: string | null
  category?: string | null
  level?: TrainingLevel | string
  status?: TrainingCourseStatus | string
  /** 部门可见范围；null/空 = 全员（语义同 HR 文档 dept_scope） */
  dept_scope?: string[] | null
  cover_image?: string | null
  instructor?: string | null
  sort_order?: number
  lesson_count?: number
  /** 各形式课时数 `{video,pdf,article,cards,link}`（0 也给键），供列表「内容」列徽标；老响应可能没有 */
  kind_counts?: Record<string, number>
  /** NAS 课程目录前缀（导入幂等键）；手建课程为 null */
  source_prefix?: string | null
  created_at?: string
  updated_at?: string
  [key: string]: unknown
}

/** POST/PUT /training/admin/courses：PUT 是 PATCH 语义（只发改动键，后端按 model_fields_set 判） */
export interface TrainingAdminCourseInput {
  title?: string
  category?: string
  description?: string | null
  level?: TrainingLevel
  dept_scope?: string[] | null
  cover_image?: string | null
  instructor?: string | null
  sort_order?: number
}

/**
 * 批量发布 / 下架里被跳过的一门课（后端口径 §10）。
 * reason 是机器码，中文文案在页面上映射；后端加了新码也不吞，原样显示。
 */
export interface TrainingBatchSkip {
  id: number
  /** not_found 时后端回 null（课都不在了，没标题可回），页面兜底显示 #id */
  title: string | null
  /** no_lessons 没有课时 / not_found 课程不存在 / already_published 已发布 / already_draft 已是草稿 */
  reason: string
}

/**
 * POST /training/admin/courses/batch-publish | batch-unpublish 的回包。
 * 部分成功语义：能办的办了，办不了的进 skipped，整体 200。
 * 成功数的键：发布是 published，下架口径只写「完全同上」没点名，两个键都兜一下。
 */
export interface TrainingBatchResult {
  published?: number
  unpublished?: number
  skipped?: TrainingBatchSkip[]
  [key: string]: unknown
}

/** POST /training/admin/courses/{id}/lessons：kind 与字段必须匹配（video/pdf 给 object_key，cards 给 pages，link 给 url） */
export interface TrainingAdminLessonInput {
  title: string
  kind: TrainingLessonKind
  object_key?: string
  pdf_key?: string
  pages?: string[]
  url?: string
}

/** POST /training/import/scan 返回摘要；skipped 是被忽略的对象键列表（可展开给 HR 看） */
export interface TrainingImportResult {
  /** 后端并行开发，形状留余地：可能是分类数或分类名列表 */
  categories?: number | string[]
  courses_created: number
  courses_seen: number
  lessons_created: number
  lessons_updated: number
  skipped: string[]
  [key: string]: unknown
}

/** POST /training/admin/upload-url：浏览器拿 upload_url 直接 PUT 到 MinIO，成功后再拿 object_key 去建课时 */
export interface TrainingUploadUrl {
  upload_url: string
  object_key: string
  expires_in: number
}

// ── 新品动态 ──
export interface ProductItem {
  id: number
  /** ⚠️ 历史坑：产品名字段是 name（不是 title） */
  name?: string
  title?: string
  brand?: string
  status?: ContentStatus
  launch_date?: string | null
  description?: string
}

// ── 通讯录 ──（routers/directory.py 序列化：name = 企微 userid）
export interface DirectoryMember {
  id: number
  /** 企微 userid（wxwork:// 深链 / openEnterpriseChat 用它） */
  name: string
  display_name: string
  department: string
  title?: string | null
  email?: string | null
  phone?: string | null
  wechat?: string | null
  avatar_url?: string | null
}

// /directory/departments 的实际返回形状（修正既有类型错误：之前声明成 {department, count}）
export interface DepartmentInfo {
  name: string
  user_count: number
}

// ── 审批 ──
export type ApprovalTable = 'news' | 'wiki' | 'training' | 'hr_doc'
export type ApprovalAction = 'approve' | 'reject' | 'recall'

export interface ApprovalItem {
  id: number
  table?: ApprovalTable
  source?: string
  title?: string
  author?: string
  status?: ContentStatus
  created_at?: string
  [key: string]: unknown
}

// ── 工作汇报（周报 / 月报）──
export type ReportType = 'weekly' | 'monthly'
export type ReportStatus = 'draft' | 'submitted'

export interface ReportItem {
  id: number
  type: ReportType
  period: string            // weekly: '2026-W31'；monthly: '2026-07'
  title: string
  content?: string          // 富文本 HTML（后端 nh3 白名单消毒后回读）
  status: ReportStatus
  user_id?: number
  username?: string
  display_name?: string
  department?: string
  read_at?: string | null   // 管理层首次查看时间（null = 未读）
  read_by?: string | null
  comment_count?: number
  created_at?: string
  updated_at?: string
  [key: string]: unknown
}

export interface ReportComment {
  id: number
  report_id: number
  display_name?: string
  content: string
  created_at?: string
  [key: string]: unknown
}

// ── 品牌手册库（Brand Docs CMS）──
export type BrandDocStatus = 'published' | 'offline'

export interface BrandDocItem {
  id: number
  brand_code: string           // 品牌 slug，如 'epomaker'
  doc_type: string             // 文档类型 slug，如 'manual' / 'playbook'
  title: string                // 展示页工具条标题
  file_url: string             // 当前版本文件地址（GCS 或本地开发 /api/portal/uploads/...）
  file_size?: number
  status: BrandDocStatus
  updated_by?: string
  created_at?: string
  updated_at?: string
  /** 上传接口附带：true = 本次为替换（原坑位已有文件） */
  replaced?: boolean
  [key: string]: unknown
}

export interface BrandDocVersion {
  id: number
  file_url: string
  file_size?: number
  uploaded_by?: string
  created_at?: string
  [key: string]: unknown
}

// ── HR 文档 CMS ──

// 块格式判别联合（冻结契约：口径-HR文档块编辑-后端.md §2/§9，v1.3 补齐后 19 种）。
// 后端对缺字段 / 未知键 fail-loud 422，因此各 type 的键全部必填——
// 「可空」字段（icon / caption / divider.text 等）传空字符串而不是省略键。
export interface HrKeypointItem { icon: string; title: string; desc: string }
export interface HrCardItem { icon: string; title: string; text: string }
export interface HrStepItem { title: string; desc: string }

export type HrCalloutVariant = 'info' | 'success' | 'warning' | 'danger'

// ── v1.3 新增 9 种块的条目形状（对齐参考「制度编辑器」）──
export interface HrMetricItem { value: string; label: string }
export interface HrCompareSide { title: string; color: string; items: string[] }
export type HrStatusLevel = 'pass' | 'fail' | 'warn'
export interface HrStatusItem { status: HrStatusLevel; text: string; tag: string }
export type HrAlertVariant = 'info' | 'warn' | 'danger' | 'success'
export interface HrBentoCard { icon: string; title: string; desc: string; color: string }
export interface HrTabcardTab {
  name: string
  header: string
  subHeader: string
  metrics: HrMetricItem[]
  note: string
}
export interface HrAccordionItem { icon: string; title: string; lines: string[]; open: boolean }

export type HrDocBlock =
  | { type: 'text'; text: string }                                        // TipTap 富文本段（HTML）
  | { type: 'image'; image: string; caption: string }                     // 图片 + 图说
  | { type: 'imageText'; image: string; title: string; text: string }     // 图文段（text 为 HTML）
  | { type: 'section'; title: string }                                    // 小节标题，渲染端自动编号+锚点
  | { type: 'callout'; variant: HrCalloutVariant; text: string }          // 四色提示块（text 为 HTML）
  | { type: 'keypoints'; items: HrKeypointItem[] }                        // 要点列表
  | { type: 'cards'; columns: 2 | 3; items: HrCardItem[] }                // 卡片组
  | { type: 'steps'; items: HrStepItem[] }                                // 步骤（自动编号）
  | { type: 'divider'; text: string }                                     // 分隔线（text 可为空串）
  | { type: 'rawhtml'; html: string }                                     // 原样 HTML（高级块）
  // ── v1.3：对齐参考「制度编辑器」补齐的 9 种块（全部纯文本字段，无 HTML 注入面）──
  | { type: 'quote'; text: string; color?: string; bg?: string }         // 引用（color=竖线色 bg=背景色，均可选自定义、空串=跟随主题）
  // 文档引用卡片（v1.17 三层：默认文章标题 / 可编辑段落富文本 / 触达按钮）。
  // 后三个键是后加的展示字段，老块可能没有（后端 _OPTIONAL_BLOCK_FIELDS 已开口），
  // 新块用 document_id；已发布历史块中的 slug 仍可读取。
  | { type: 'docref'; document_id?: number; slug?: string; title: string; text?: string; button_text?: string; button_url?: string }
  | { type: 'metrics'; items: HrMetricItem[] }                            // 指标卡片（3 列大数字）
  | { type: 'compare'; left: HrCompareSide; right: HrCompareSide }        // 对比卡（左右两栏）
  | { type: 'status'; items: HrStatusItem[] }                             // 状态列表（三档圆点）
  | { type: 'alert'; variant: HrAlertVariant; title: string; text: string } // 警示横幅（四型）
  | { type: 'bento'; cards: HrBentoCard[] }                               // Bento 网格（2 列彩边卡）
  | { type: 'numsec'; num: string; title: string; text: string; color?: string } // 编号章节（大号编号；color 为空串=跟随文档主题）
  | { type: 'tabcard'; activeTab: number; tabs: HrTabcardTab[] }          // 标签指标卡（页签切换）
  | { type: 'accordion'; items: HrAccordionItem[] }                       // 下拉表格（折叠列表）

export interface HrPublicationState {
  published_before: boolean
  body_changed: boolean
  current_version: string
  revision: string
}

export interface HrDocument {
  id: number
  /** 管理详情/保存响应的只读发布判定；设置变化不计入正文变化。 */
  publication_state?: HrPublicationState
  canonical_url?: string
  legacy_slug?: string | null
  slug?: string | null
  title?: string
  category?: string
  /** 二级分类（制度中心 6 主题导航，仅 HR 制度有）；缺省 = admin（行政规范） */
  topic?: string
  status?: ContentStatus
  content_html?: string | null
  /** 已发布块快照（员工读这个）；与 content_html 互斥非空，两者皆空 = 未发布 */
  content_blocks?: HrDocBlock[] | null
  /** 在编草稿块（仅 admin 口径返回）；与 draft_content 互斥非 null */
  draft_blocks?: HrDocBlock[] | null
  version?: number
  requires_signoff?: boolean
  dept_scope?: string[] | null
  /** v1.19：阅读页是否显示「下载 PDF」按钮（默认关）+ 其草稿三态列 */
  download_pdf_enabled?: boolean
  draft_download_pdf_enabled?: boolean
  /** v1.19：员工签收卡配置（每项可自定义、空值回落默认；键集见 utils/hr-signoff.ts）+ 草稿三态列 */
  signoff?: Record<string, string>
  draft_signoff?: Record<string, string> | null
  updated_at?: string
  /** v1.8：最近一次企微推送记录（仅 admin 详情端点返回），推送弹框显示「上次推送时间」用 */
  last_push?: HrDocLastPush | null
  submitted_by?: string | null
  submitted_at?: string | null
  published_by?: string | null
  /** v1.20 下架：offline_at 非空 = 已下架（员工端不可见）。**不影响 status/version**。 */
  offline_at?: string | null
  offline_by?: string | null
  offline_reason?: string | null
  [key: string]: unknown
}

/** 审批流水行（内容审批中心「我已审」tab） */
export interface ApprovalHistoryRow {
  source_table: ApprovalTable | string
  item_id: number
  title: string
  author_name?: string
  review_comment?: string | null
  approval_status: string
  reviewer_name?: string | null
  reviewed_at?: string | null
  slug?: string
  preview_url?: string
  version?: string
  [key: string]: unknown
}

/** 最近一次企微推送记录（v1.8：推送与审核拆开后，前端要能看到频控状态） */
export interface HrDocLastPush {
  pushed_at: string | null
  recipient_count: number
  failed_count: number
  pushed_by: string
}

/**
 * 员工端「已发布 HR 文档」摘要（GET /hr-documents/published；v1.9.1 栏目页，v1.16 兼作文档引用块的数据源）
 *
 * `theme` / `cover` 是 v1.16 为「引用块画封面预览」补的两键：引用卡片要在**渲染时**
 * 拿到被引用文档的主题与封面配置（背景/图标/说明），所以随这个列表一起回。
 * 无权查看的文档整条都不在列表里（用例层逐篇过滤），封面不会绕过部门可见性泄漏。
 */
export interface HrDocPublishedItem {
  id: number
  canonical_url: string
  legacy_slug?: string | null
  slug?: string | null
  title: string
  category: string
  /** 二级分类（制度中心 6 主题导航，仅 HR 制度有）；缺省 = admin（行政规范） */
  topic?: string
  intro?: string
  requires_signoff?: boolean
  updated_at?: string | null
  published_at?: string | null
  /** 文档主题（'' = 默认品牌紫 / 预设 key / #RRGGBB），引用卡片背景用它 */
  theme?: string
  /** 整屏封面配置（键集见 data/hr-cover.ts 的 HrCover）；`{}` = 全默认 */
  cover?: Partial<HrCover> | null
}

/** 手动推送企微的结果（v1.8） */
export interface HrDocPushResult {
  success: boolean
  /** 被 24h 频控拦截时为 true（reason = 'cooldown'），此时一条都没发 */
  skipped: boolean
  reason?: string
  last_pushed_at?: string | null
  sent: number
  failed: number
  /** 圈出来的人为空时为 true */
  no_recipients?: boolean
  /** 实际使用的档位与圈到的人数 */
  audience: 'all' | 'scope' | 'users'
  recipients: number
}

/** 「指定员工」搜索候选 —— 后端只吐这三样，不返回完整用户对象 */
export interface HrDocPushCandidate {
  username: string
  display_name: string
  department: string[]
  /** false = 已离职 / 未关注企微（同步会把 status 2/4 写成非在职）→ 不可推送 */
  is_active: boolean
}

// 草稿版本历史条目（口径-HR文档块编辑-后端.md §7）：
// 每次保存草稿后端自动留的快照，与已发布版本分表（发布快照不可删，这个可删）。
// 列表端点只回元信息 + summary；详情端点才带 content_html / content_blocks（互斥非空）
export interface DraftVersionItem {
  id: number
  title?: string
  saved_by?: string
  created_at?: string
  /** 快照格式：blocks = 块草稿 / html = 整篇草稿 */
  format?: 'blocks' | 'html'
  /** 后端算好的摘要文案：块格式 = 块数，HTML 格式 = 字数 */
  summary?: string
  /** 详情端点才有：HTML 快照正文 */
  content_html?: string | null
  /** 详情端点才有：块快照正文 */
  content_blocks?: HrDocBlock[] | null
  [key: string]: unknown
}

// ── 通用分类（/categories 八端点；样品品类 / 办公用品品类 / 培训分类共用一张 portal_categories）──
// 口径：doc_ww/口径-通用分类表.md §2 / §6。行形状冻结在 backend/routers/categories.py。

/**
 * 分类归属。唯一真相是后端注册表（backend/domain/categories.py SCOPES）——加业务只加一行
 * 注册表、不改表；前端目前只用得到这三个，后端再登记新 scope 时这里补一个成员即可。
 */
export type CategoryScope = 'material.sample' | 'material.office' | 'training.course'

/**
 * 分类上的开关位（jsonb）。键按 scope + depth 白名单登记，后端对未登记的键 422，所以能穷举。
 * 没做成按 scope 的联合类型：同一套分类面板要通吃三个 scope，联合会逼每个读取点先窄化，
 * 麻烦大于收益；键之间互不冲突，合成一个全可选的对象够用。
 */
export interface CategoryAttrs {
  /** material.office：员工自助目录隐藏该品类（入职礼包走行政直发，不开放自助申请） */
  employee_hidden?: boolean
  /** training.course 大类：卡片 emoji */
  icon?: string
  /** training.course 大类：卡片副标题 */
  description?: string
}

/** GET /categories?scope= 的行（登录即可）：只有在役行，平铺返回按 (depth, parent_id, sort_order, name)，两层 scope 前端自己组树 */
export interface Category {
  id: number
  scope: CategoryScope
  name: string
  /** slug；培训大类必填且建后不可改，物资侧可空 */
  code: string | null
  /** 两层形状（培训）的小类挂大类；平铺 scope（物资）恒 null */
  parent_id: number | null
  /** 1 = 顶层，培训小类 = 2 */
  depth: number
  sort_order: number
  /** 列 NOT NULL default '{}'，恒为对象，不会是 null */
  attrs: CategoryAttrs
}

/** GET /categories/admin?scope= 的行（该 scope 管理能力，否则 403）：多归档信息与两个计数 */
export interface CategoryAdminRow extends Category {
  /** 在役为 null。归档 = 从下拉里拿掉一个选项，不动任何库存 / 单据 / 课程 */
  archived_at: string | null
  archived_by: string | null
  created_by: string | null
  /** 引用数：物资 = 在役商品（SPU）数，培训 = 课程数含 draft；非零时归档 409（培训大类除外） */
  ref_count: number
  /** 在役子行数，平铺 scope 恒 0；非零时硬删 409 */
  children_count: number
}

// ── 样品领用（Sample Borrowing）──
// 口径：doc_ww/口径-样品领用-后端.md；表结构见 docs/DATA_CONTRACT.md SampleItem 等四节
export type SampleBorrowStatus = 'pending' | 'rejected' | 'borrowed' | 'returned' | 'closed'

export interface SampleItem {
  id: number
  category: string
  sku: string
  name: string
  brand?: string | null
  image_urls: string[]
  stock_qty: number              // 0 = 已借空（在库状态派生，无独立列）
  /** 站点 code：'SZ' | 'CD'（口径-物资站点 §2.1；后端未上线时缺失，前端降级不渲染徽标） */
  site?: string
  location: string
  current_borrowers: string[]    // 借用人姓名缓存（借/还事务维护）
  owner_username: string         // 样品负责人 = 借用单审批人
  owner_name: string
  note?: string | null
  created_by?: string
  created_at?: string
  updated_at?: string
  [key: string]: unknown
}

export interface SampleItemsMeta {
  categories: string[]
  brands: string[]
  locations: string[]
  /** 站点 code 列表，唯一真相=后端配置表（口径-物资站点 §2.1）；缺失/空 = 站点功能未上线 */
  sites?: string[]
}

export interface SampleBorrowItemRow {
  id: number
  item_id: number
  sample_name: string            // 快照，防目录改名对不上历史单据
  sample_sku: string
  quantity: number
  returned_quantity: number
  /** 仅详情接口附带：目录首图 / 目录当前负责人（找谁归还） */
  image_url?: string | null
  owner_name?: string | null
  site?: string | null
  [key: string]: unknown
}

export interface SampleBorrowLog {
  id: number
  operation: string              // approve / reject / extend / return / close
  operator_username: string
  operator_name: string
  before_value?: string | null   // 延期：旧预计归还日
  after_value?: string | null
  remark?: string | null
  created_at?: string
  [key: string]: unknown
}

// ── 物资申请（/materials/* 新接口面：办公用品领用）──
// 定稿：doc_ww/口径-办公用品-前端定稿.md。样品页仍走上面的 Sample*（/samples/* 冻结面），两套并存到样品页迁移为止
export type MaterialApplyStatus = 'pending' | 'rejected' | 'issued' | 'closed'

export interface MaterialItem {
  id: number
  kind: 'sample' | 'office'
  category: string
  sku?: string | null            // office 档案可空
  name: string
  brand?: string | null
  spec?: string | null           // 规格/尺码（工服 M~XXXL 同名多规格各一行库存）
  /** 所属商品（SPU）。S2 起每行档案都挂商品，老响应缺这两键时按 1:1 降级显示 */
  product_id?: number | null
  product_name?: string | null
  /** 分类挂在商品上（本表不加 category_id 列），值来自 SPU；上面的 category 是同源文本镜像 */
  category_id?: number | null
  /** 规格取值，如 {"颜色":"黑色","尺码":"XL"}；键集恒等于所属商品的 variant_dims，无维度商品为 {} */
  variant_values?: Record<string, string>
  unit_price?: number | null     // 仅 can_manage_materials 的响应携带（后端按能力裁剪字段）
  image_urls: string[]
  stock_qty: number
  /** 站点 code：'SZ' | 'CD'（新面建档必填；老响应缺失时前端降级） */
  site?: string
  location: string
  current_borrowers: string[]
  owner_username: string
  owner_name: string
  note?: string | null
  created_by?: string
  created_at?: string
  updated_at?: string
  [key: string]: unknown
}

// ── 物资商品（SPU）/ 规格（SKU）——/materials/products* ──
// 口径：doc_ww/口径-物资SPU-SKU与商城式领用.md §5.1 与 v1 追记 1。
// 商品是员工看到的「一件东西」，规格是真正扣库存的那一行（就是 portal_materials，没改名）。

/**
 * 商品旗下的一个规格行。products 接口把规格内联返回（规格数是个位数，省一次点开再请求、
 * 也免 N+1），员工端多维联动全靠它在前端算，不另开端点。
 */
export interface MaterialProductSku {
  id: number
  /** office 档案可空 */
  sku: string | null
  /**
   * 规格展示文本。variant_values 非空时由后端按商品 variant_dims 的顺序拼成「黑色 / XL」，
   * 是派生列——前端只读，传回去也会被忽略；values 为 {} 时它才是可手填的自由文本。
   */
  spec: string | null
  /**
   * 规格取值，如 {"颜色":"黑色","尺码":"XL"}。键集恒等于所属商品的 variant_dims
   * （商品 dims 为 [] 时恒为 {}），后端校验，前端按 dims 顺序渲染即可。
   */
  variant_values: Record<string, string>
  name: string
  /** 站点 code：'SZ' | 'CD'。同商品同组合两地各有库存行是允许的，「全部城市」视图下要按站点分行显示 */
  site?: string | null
  location: string
  stock_qty: number
  image_urls: string[]
  current_borrowers: string[]
  owner_name: string
  /** 在役为 null；员工身份的响应里不含归档规格 */
  archived_at: string | null
  /** 仅 can_manage_materials 的响应携带（后端按能力裁剪字段，拿不到就别渲染） */
  unit_price?: number | null
}

/** GET /materials/products 的行。员工端目录主接口只出「至少有 1 个在役规格」的在役商品 */
export interface MaterialProduct {
  id: number
  kind: 'sample' | 'office'
  /** 分类只挂在商品上（规格表不存 category_id） */
  category_id: number | null
  /** 分类名，后端连表带出省一次分类请求；分类改名后端同事务级联，前端不自己映射 */
  category: string | null
  name: string
  brand?: string | null
  description?: string | null
  image_urls: string[]
  /**
   * 有序的规格维度名，如 ["颜色","尺码"]，最多 3 个。
   * [] = 单规格商品，或多库存行但没登记维度的存量数据——员工端退回单行平铺 chip。
   * 顺序即 spec 的拼接顺序，也是员工端 chip 行的上下顺序。
   */
  variant_dims: string[]
  /** 旗下在役规格数与库存合计；请求带 site 时只统计该站点 */
  sku_count: number
  total_stock: number
  /**
   * 旗下规格覆盖的站点 code 去重列表，卡片直接显示「深圳 / 成都」。
   * ⚠ 口径 §5.1 的行清单里没列这个键，等后端实现确认；缺失时前端从 skus 自行汇总
   */
  sites?: string[]
  skus: MaterialProductSku[]
  archived_at: string | null
  created_at?: string
  updated_at?: string
}

/**
 * GET /materials/products/merge-suggestions 的一组：实时算的建议合并清单（不落库、不记「忽略」）。
 * 归并键是精确同名同品牌；一键合并没有专门端点 —— 选好目标商品后走 attach。
 */
export interface MergeSuggestionGroup {
  /** 分组键（品牌 + 词干归一化的结果），前端只当列表 key 用 */
  key: string
  brand: string | null
  /** 去掉规格尾缀后的共同词干 */
  stem: string
  /** 建议的商品名，合并弹窗预填、管理员可改 */
  suggested_name: string
  products: Array<{
    id: number
    name: string
    skus: Array<{ id: number; sku: string | null; spec: string | null }>
  }>
}

export interface MaterialApplyLine {
  id: number
  material_id: number
  material_name: string
  material_sku?: string | null
  spec?: string | null
  quantity: number
  approved_qty?: number | null   // NULL=按申请量发放；0=该行不批（审批减量）
  returned_quantity: number
  /** 仅详情接口追加：到 site·location 找 owner_name 领取 */
  image_url?: string | null
  owner_name?: string | null
  location?: string | null
  site?: string | null
  [key: string]: unknown
}

export interface MaterialApplyLog {
  id: number
  operation: string              // approve / reject / issue（直接发放）/ close
  operator_username: string
  operator_name: string
  before_value?: string | null
  after_value?: string | null
  remark?: string | null
  created_at?: string
  [key: string]: unknown
}

/** 站点负责人配置格（口径-物资站点 §2.3）：(site, kind) → 负责人，配置表=唯一真相 */
export interface SiteOwnerCell {
  site: string                   // 'SZ' | 'CD'
  kind: 'sample' | 'office'
  owner_username: string
  owner_name: string
  updated_at?: string
  updated_by_name?: string
  [key: string]: unknown
}

/** PUT site-owners（dry_run 或落库后）的影响预告行 */
export interface SiteOwnerChange {
  site: string
  kind: string
  old_owner_name: string
  new_owner_name: string
  material_count: number
  pending_apply_count: number
  [key: string]: unknown
}

export interface MaterialApply {
  id: number
  apply_no: string               // SQ 前缀（样品单 BY）
  applicant_username: string
  applicant_name: string
  department?: string | null
  purpose: string
  status: MaterialApplyStatus    // office 状态机：pending → issued → closed / rejected，无归还逾期概念
  /** 后端派生：当前用户 ∈ 涉及物资负责人 ∪ admin */
  can_handle?: boolean
  /** 后端派生：EXISTS issue 流水 — 直发单挂「直发」标与员工申请区分 */
  is_direct?: boolean
  remark?: string | null
  created_at?: string
  updated_at?: string
  items: MaterialApplyLine[]
  logs?: MaterialApplyLog[]      // 仅详情返回，created_at 正序
  [key: string]: unknown
}

/** 批量审批里没办成的一单（整体 200，逐单归因；reason 见后端 domain 的 BATCH_SKIP_*） */
export interface BatchHandleSkip {
  id: number
  apply_no: string | null        // null = 这个 id 的单根本不存在
  /** status=已被处理 / forbidden=无权 / stock=库存不足 / not_found=单不存在 / error=其他 */
  reason: 'status' | 'forbidden' | 'stock' | 'not_found' | 'error'
  detail: string                 // 后端人话文案，前端直接显示
}

/** 批量通过/驳回回包：成功数键名按操作不同（approved / rejected），其余进 skipped */
export interface BatchHandleResult {
  approved?: number
  rejected?: number
  skipped: BatchHandleSkip[]
}

export interface SampleBorrow {
  id: number
  borrow_no: string
  applicant_username: string
  applicant_name: string
  department?: string | null
  purpose: string
  expect_return_date: string     // YYYY-MM-DD
  actual_return_date?: string | null
  status: SampleBorrowStatus
  /** 后端派生：borrowed 且今天已过预计归还日 */
  is_overdue?: boolean
  /** 后端派生：当前用户 ∈ 涉及样品负责人 ∪ admin — 操作按钮显隐的唯一依据 */
  can_handle?: boolean
  /** 后端派生：extend 流水条数 — 列表/详情挂「已延期」标（区分正常借出流程与延期过的单） */
  extend_count?: number
  remark?: string | null
  created_at?: string
  updated_at?: string
  items: SampleBorrowItemRow[]
  logs?: SampleBorrowLog[]       // 仅详情接口返回，created_at 正序
  [key: string]: unknown
}

// ── 消息盒子（站内信）——契约见 doc_ww/口径-消息盒子-后端.md ──
/** 收件箱行（本人视角：消息本体 + 本人已读状态） */
export interface MessageItem {
  id: number                     // message_id
  title: string
  content: string                // 纯文本，前端不渲染 HTML
  link?: string | null           // '/' 开头站内路径或 https 外链
  sender_name: string            // 内部口子发的显示「系统通知」
  read_at?: string | null        // null = 未读
  created_at: string
  [key: string]: unknown
}

// ── 劳动合同生成 ——契约见 doc_ww/口径-劳动合同生成-后端.md §2/§9.1/§10 ──
/** 签约主体（v1.1 起库表化，业务可自助新增/编辑；generate 请求体用 company_id 指向它）
 *  v1.2 去 signer：签署人 = 员工姓名（模板签名栏直接绑员工名），不是主体属性 */
export interface ContractEntity {
  id: number
  name: string                   // 公司全称（库表唯一，重名创建返回 409）
  address: string
  legal_rep: string              // 法定代表人
}

/** POST /contracts/entities 与 PUT /contracts/entities/{id} 共用入参（长度上限 100/200/50 与后端同构） */
export interface ContractEntityCreate {
  name: string
  address: string
  legal_rep: string
}

/** GET /contracts/config 的 data：主体列表 + 表单默认值 */
export interface ContractConfig {
  entities: ContractEntity[]
  defaults: {
    report_to: string            // 汇报对象默认「上级」
    work_city: string            // 工作地点市默认「成都」
    work_cities?: string[]       // 工作城市下拉数据源；接口没给时前端兜底 深圳/成都/东莞
    work_schedule: string        // 模板第四条默认工时段全文
    contract_years: number       // 合同期默认年数（期限选择器默认值）
    probation_months: number     // 试用期默认月数（试用期截止联动用）
  }
}

// ── 员工庆祝提醒（生日/入职周年）——契约见 doc_ww/口径-员工庆祝提醒-后端.md §3 ──
/** GET /celebrations/calendar 行：生日+周年合并按日排序 */
export interface CelebrationCalendarItem {
  username: string
  display_name?: string | null
  main_department?: string | null
  /** birthday | anniversary */
  kind: string
  /** 展示红线：生日全站只回月-日，这里是当月的 MM-DD */
  date: string
  /** 周年才有：第 N 年 */
  years?: number | null
  /** 本人不参与庆祝（不发本人祝福、不进公示；HR 清单仍可见并标注） */
  opt_out?: boolean
  birthday_set?: boolean
  hire_set?: boolean
  [key: string]: unknown
}

/** portal_celebration_runs 行（提醒引擎运行记录） */
export interface CelebrationRunItem {
  id?: number
  run_date?: string
  /** monthly_digest | daily */
  kind?: string
  /** ok | partial | failed */
  status?: string
  /** 发送统计 JSON 文本（后端如实存，前端尽力解析展示） */
  detail?: string | null
  created_at?: string
  [key: string]: unknown
}

/** 推送文案模板键——后端口径 §6 冻结的 8 个，别自造（未知键后端 422 fail-loud） */
export type CelebrationTemplateKey =
  | 'birthday_title' | 'birthday_body'          // 生日祝福，占位符 {name}
  | 'anniversary_title' | 'anniversary_body'    // 周年祝福，占位符 {name} {years}
  | 'monthly_title' | 'monthly_intro'           // 月度预告：正文清单自动生成，只放开标题与开头语
  | 'daily_title' | 'daily_intro'               // 当日清单：同上

/** 模板键→文案。GET 里 templates=生效值（存储与默认合并）、template_defaults=代码默认值 */
export type CelebrationTemplates = Partial<Record<CelebrationTemplateKey, string>>

/**
 * PUT settings 的 templates：部分合并语义——传键覆盖、传 null 恢复默认（后端删存储键）、
 * 没传的键不动。所以前端只发改动过的键。
 */
export type CelebrationTemplatePatch = Partial<Record<CelebrationTemplateKey, string | null>>

/** POST /celebrations/test-push 的 kind——四组文案各对应一种（v1.3，后端口径 §7 冻结的枚举） */
export type CelebrationTestPushKind = 'birthday' | 'anniversary' | 'monthly_digest' | 'daily_list'

/** GET /celebrations/settings 的 data：设置 + 最近 20 条 runs（后端并行开发，形状留宽容余地） */
export interface CelebrationSettings {
  enabled?: boolean
  /** 北京时间发送时刻 0-23，默认 9 */
  send_hour?: number
  /** 北京时间发送时刻 0-59（v1.5）；与 send_hour 组成「到点之后才发」的窗口下界 */
  send_minute?: number
  /** 生日发给本人那条链的开关（站内信+企微文本+图片贺卡），关掉不停周年/HR 清单/预告 */
  birthday_enabled?: boolean
  /** 生日图片贺卡子开关：新功能默认关，HR 打开后自动任务才发 */
  birthday_card_enabled?: boolean
  updated_by?: string | null
  updated_at?: string | null
  runs?: CelebrationRunItem[]
  /** v1.2：生效模板文案（存储覆盖默认后的合并结果） */
  templates?: CelebrationTemplates
  /** v1.2：代码默认文案——前端判「当前=默认」灰徽标与恢复默认用 */
  template_defaults?: CelebrationTemplates
  [key: string]: unknown
}

/** GET /celebrations/push-logs 行（v1.5 逐人留痕）：一发一行，发给本人的按人一条 */
export interface CelebrationPushLog {
  trigger: string
  /** 收件人 userid；HR 当日清单/月度预告是批量发，这里是固定标签 hr_daily / monthly_digest */
  username: string
  /** 标题级短句（不含祝福语全文） */
  content: string
  /** 「这一发成功」，不代表员工已读 */
  ok: boolean
  error?: string | null
  /** ISO UTC，未推送时为空 */
  pushed_at?: string | null
  [key: string]: unknown
}

/** POST /celebrations/import 返回：部分成功允许（合法行落库），errors 逐行给原因 */
export interface CelebrationImportResult {
  ok_count: number
  errors: Array<{ row: number; reason: string }>
  [key: string]: unknown
}

/** GET /celebrations/employees 行：全员工清单（含停用与未录入）——员工信息管理页表格（v1.1） */
export interface CelebrationEmployeeItem {
  username: string
  display_name?: string | null
  main_department?: string | null
  /** false = 已停用（表格里打「停用」徽标，仍展示便于看清数据缺口） */
  is_active?: boolean
  /** 展示红线：只回 MM-DD；未录入为 null */
  birthday?: string | null
  birthday_set?: boolean
  /** 完整日期（工龄非敏感）；未录入为 null */
  hire_date?: string | null
  celebration_opt_out?: boolean
  [key: string]: unknown
}

export type CelebrationCardKind = 'birthday' | 'anniversary' | 'custom'

/** 公开贺卡只包含展示文案，不包含账号、生日年份或发送记录。 */
export interface CelebrationCardContent {
  kind: CelebrationCardKind
  name: string
  title: string
  message: string
  years: number | null
  date: string
}

export interface CelebrationHtmlCard {
  id: number
  url: string
  kind: CelebrationCardKind
  recipient_username: string | null
  recipient_name: string
  title: string
  message: string
  years: number | null
  sent_ok: boolean
  error: string | null
  created_at: string | null
  sent_at: string | null
}

export interface CelebrationHtmlCardInput {
  kind: CelebrationCardKind
  recipient_username: string
  recipient_name: string
  title: string
  message: string
  years: number | null
}

export interface CelebrationHtmlCardResult {
  card: CelebrationHtmlCard
  url: string
  result: { sent: number; failed: number; errors?: string[] }
  ok: boolean
}

export type EmployeeSyncField = 'display_name' | 'department' | 'birthday' | 'hire_date'
export interface EmployeeSyncSelection {
  username: string
  fields: EmployeeSyncField[]
}
/** 获取企微资料只产生待确认变更；日期缺项仅计入汇总，不自动更新。 */
export interface EmployeeSyncJob {
  id: string
  status: 'queued' | 'running' | 'ready' | 'applying' | 'applied' | 'failed' | 'stale'
  stage: 'queued' | 'metadata' | 'scanning' | 'ready' | 'verifying' | 'saving' | 'complete' | 'failed'
  total: number
  processed: number
  expires_at: string
  summary: { total: number; changed: number; unchanged: number; skipped: number; failed: number } | null
  changes: Array<{
    username: string
    display_name: string
    fields: Array<{ key: EmployeeSyncField; label: string; old_value: string | null; new_value: string; display_equal: boolean }>
  }>
  unmatched_count: number
  can_deactivate: boolean
  unmatched_candidates: Array<{ username: string; display_name: string; is_active: boolean; selectable: boolean; protected_reason: string | null }>
  apply_summary: { selected: number; updated: number; unchanged: number } | null
  error: { code: string; message: string } | null
}
export interface EmployeeSyncApplyResult {
  selected: number
  updated: number
  unchanged: number
  employees: CelebrationEmployeeItem[]
}
export interface EmployeeSyncDeactivateResult {
  selected: number
  deactivated: number
  already_inactive: number
  employees: CelebrationEmployeeItem[]
}

/** GET /celebrations/me 的 data：本人庆祝信息（生日对本人也只回月-日，红线统一） */
export interface CelebrationMe {
  birthday?: string | null
  birthday_set?: boolean
  hire_date?: string | null
  hire_set?: boolean
  celebration_opt_out?: boolean
  [key: string]: unknown
}

// ══ 简历识别（/admin/resumes）══
// 契约：doc_ww/口径-简历识别-后端.md §5.2（extracted 结构）与 §8（wire 键名逐字）。
// 展示红线：列表接口只回 birth_month_day（MM-DD）与 phone_masked，明文电话 / 完整生日只在详情。

/** parsed = AI 已出结果待 HR 校对；confirmed = HR 已确认；failed = AI 失败或判定不是简历 */
export type ResumeStatus = 'parsed' | 'confirmed' | 'failed'

/** 最高学历六值（后端 Literal，HR 校对下拉与之同源） */
export type ResumeDegree = '博士' | '硕士' | '本科' | '大专' | '高中及以下' | '其他'

/** extracted.education 一条：日期一律字符串（YYYY-MM / YYYY / 「至今」），AI 拿不准给 null */
export interface ResumeEducationItem {
  school: string | null
  degree: ResumeDegree | null
  major: string | null
  start: string | null
  end: string | null
  full_time: boolean | null
}

/** extracted.work_history 一条（按时间倒序）；end 为「至今」表示在职 */
export interface ResumeWorkItem {
  company: string | null
  title: string | null
  start: string | null
  end: string | null
  /** ≤300 字职责与业绩摘要，不是原文照抄 */
  description: string | null
}

/**
 * AI 结构化输出的完整 JSON（当前生效版本，HR 修正会同步写进来）。
 * is_resume=false 时其余字段全空 / 空列表，后端已把该行标 failed。
 */
export interface ResumeExtracted {
  is_resume: boolean
  name: string | null
  gender: '男' | '女' | null
  /** YYYY-MM-DD；只有年月时 AI 给 null 并写进 warnings */
  birth_date: string | null
  phone: string | null
  email: string | null
  current_city: string | null
  highest_degree: ResumeDegree | null
  years_of_experience: number | null
  current_company: string | null
  current_title: string | null
  expected_position: string | null
  /** 原文字串（15-20K / 面议），不拆数值 */
  expected_salary: string | null
  education: ResumeEducationItem[]
  work_history: ResumeWorkItem[]
  skills: string[]
  certificates: string[]
  languages: string[]
  /** 2~3 句中文概述 */
  summary: string
  /** AI 拿不准的地方（模糊 / 缺失 / 多个候选值），校对重点 */
  warnings: string[]
}

/** GET /resumes 行（脱敏）：没有 phone / birth_date / extracted */
export interface ResumeListItem {
  interview_summary?: ResumeInterviewSummary | null
  id: number
  status: ResumeStatus
  name: string | null
  gender: string | null
  /** 只回 MM-DD；未知为 null */
  birth_month_day: string | null
  /** 138****5678 */
  phone_masked: string | null
  email: string | null
  current_city: string | null
  highest_degree: string | null
  years_of_experience: number | null
  current_company: string | null
  current_title: string | null
  expected_position: string | null
  summary: string | null
  /** 归一化 phone / email 或文件 sha256 命中已有记录 → 指向那条；HR 自行比对，不阻断入库 */
  duplicate_of_id: number | null
  uploaded_by: string
  created_at: string
  confirmed_at: string | null
}

export type InterviewStatus = 'active' | 'completed' | 'cancelled'
export type InterviewResult = 'passed' | 'rejected'
export type InterviewRoundStatus = 'pending' | 'completed' | 'cancelled'
export interface ResumeInterviewSummary {
  count: number
  active_process_id: number | null
  status: InterviewStatus
  current_round_name: string | null
}
export interface InterviewRoundInput {
  name: string
  interviewer?: string | null
  scheduled_at?: string | null
  location?: string | null
}
export interface InterviewRound extends InterviewRoundInput {
  id: number
  process_id: number
  sequence: number
  interviewer: string | null
  scheduled_at: string | null
  location: string | null
  status: InterviewRoundStatus
  result: InterviewResult | null
  feedback: string | null
  updated_by: string
  created_at: string
  updated_at: string
}
export interface InterviewProcess {
  id: number
  resume_id: number
  position: string
  status: InterviewStatus
  result: InterviewResult | null
  summary: string | null
  created_by: string
  updated_by: string
  created_at: string
  updated_at: string
  finished_at: string | null
  rounds: InterviewRound[]
}
export interface InterviewCreate {
  position: string
  rounds: InterviewRoundInput[]
}
export interface InterviewRoundPatch extends Partial<InterviewRoundInput> {
  status?: InterviewRoundStatus
  result?: InterviewResult | null
  feedback?: string | null
}

/** GET /resumes/{id} 与所有写操作的返回：列表字段 + 明文 / 全量 / 文件与 AI 元信息（不出 ai_raw / storage_key） */
export interface ResumeOut extends ResumeListItem {
  phone: string | null
  /** 完整 YYYY-MM-DD */
  birth_date: string | null
  expected_salary: string | null
  extracted: ResumeExtracted
  /** = extracted.warnings，方便直接渲染提示条 */
  warnings: string[]
  source_filename: string
  /** pdf / docx / png / jpg / jpeg / webp（小写不带点）——决定原件预览用 iframe 还是 img */
  file_ext: string
  file_size: number
  ai_model: string | null
  ai_tokens_in: number | null
  ai_tokens_out: number | null
  /** failed 原因 */
  error_msg: string | null
  confirmed_by: string | null
  updated_at: string
}

/**
 * PUT /resumes/{id}：常用列任意子集，只发改动的键；清空的字段发 null。
 * extracted 可整体替换（v1 前端不编辑列表型内容，暂不发）。
 */
export interface ResumePatch {
  name?: string | null
  gender?: string | null
  birth_date?: string | null
  phone?: string | null
  email?: string | null
  current_city?: string | null
  highest_degree?: string | null
  years_of_experience?: number | null
  current_company?: string | null
  current_title?: string | null
  expected_position?: string | null
  expected_salary?: string | null
  extracted?: ResumeExtracted
}

// ══ AI 管理：模型管理（/admin/ai/models）+ AI 功能与提示词（/admin/ai/features）══
// 契约：doc_ww/口径-AI管理-模型与提示词.md §4（wire 键名逐字）；取代 v1 单页的 /ai-settings。
// 红线：wire 里只有 key「是否已配置」的布尔，永远没有 key 值；页面也不许自己拼。

/** 厂商取值，同时是成本表 portal_api_call_log.provider 的值 */
export type AiProviderKey = 'claude' | 'gemini'

/** 思考档位：low 最快最省，high 更慢更准（后端 EFFORTS 只放开这三档） */
export type AiEffort = 'low' | 'medium' | 'high'

/** 模型档位：任务按档位取默认模型，档位 → 模型的映射在模型管理里按厂商配 */
export type AiTier = 'fast' | 'standard' | 'quality'

// ── 模型管理 ──

/** GET /ai-models 的 providers[] 一项（PUT /ai-providers/{provider} 也回这个形状） */
export interface AiModelProvider {
  key: AiProviderKey
  /** 展示名，如「Claude（Anthropic）」 */
  label: string
  /** 该厂商 key 是否非空——只是布尔 */
  configured: boolean
  /** 两家至多一家为 true；都为 false = 跟随环境变量 AI_PROVIDER */
  is_default: boolean
  /** 代码里的档位表（services/ai/routing），「代码默认（xxx）」显示它 */
  code_tier_models: Record<AiTier, string>
  /** 管理员配的覆盖；null = 用代码默认 */
  tier_overrides: Record<AiTier, string | null>
  /** 覆盖叠代码默认后的实际生效映射 */
  tier_models: Record<AiTier, string>
  updated_by: string | null
  updated_at: string | null
  // ↓ key 状态（§8.3）：只有来源与末 4 位提示，任何接口都不回原文
  /** 当前生效 key 的来源：page = 管理页设置（加密落库）、env = 环境变量 / Secret Manager、null = 都没有 */
  key_source: AiKeySource
  /** 当前生效 key 的末 4 位，形如「…ab12」（env key 也给）；无 key 为 null */
  key_hint: string | null
  /** 页面 key 的设置人 / 时间；key_source 不是 page 时为 null */
  key_updated_by: string | null
  key_updated_at: string | null
  /** 页面 key 不可用（主密钥缺失 / 解密失败）已回落环境变量时的中文说明 */
  key_error: string | null
  /** false = 现在不能在页面设置 key（如生产缺加密主密钥），原因在 key_editable_reason */
  key_editable: boolean
  key_editable_reason: string | null
}

export type AiKeySource = 'page' | 'env' | null

/** POST /ai-providers/{provider}/key/test：上游失败也是 200，ok=false、error 为中文原因 */
export interface AiKeyTestResult {
  ok: boolean
  latency_ms: number | null
  /** 该 key 能列出的模型数 */
  model_count: number | null
  error: string | null
}

/** 模型被谁引用：档位映射（只算实际生效的）或某个功能显式指定了它；有引用就不能删、不能停用 */
export type AiModelReference =
  | { kind: 'tier'; provider: AiProviderKey; tier: AiTier }
  | { kind: 'task'; task_name: string; label: string }

/** 模型目录一行；目录是模型的唯一白名单，调用前按它校验 */
export interface AiModel {
  id: number
  provider: AiProviderKey
  /** 厂商侧模型 ID，建后不可改 */
  model_id: string
  display_name: string
  /** 停用后不可被选、调用前校验会拒；连通测试仍可用 */
  enabled: boolean
  /** USD / 百万输入 token；null = 未填（成本回落代码费率表，都没有则 cost 为空） */
  input_price: number | null
  /** USD / 百万输出 token（含思考 token） */
  output_price: number | null
  note: string | null
  /** 同厂商内排序 */
  sort_order: number
  references: AiModelReference[]
  updated_by: string | null
  updated_at: string | null
}

/** GET /ai-models 的 data */
export interface AiModelsData {
  /** AI_PROVIDER 的生效值（没设 = claude）；配错时为 null */
  env_default_provider: AiProviderKey | null
  /** 仅 AI_PROVIDER 配错时为字符串，页面红条原样展示 */
  env_provider_error: string | null
  /** 默认厂商 → 环境变量 → claude 的结果；环境变量配错且没设默认厂商时为 null */
  effective_default_provider: AiProviderKey | null
  providers: AiModelProvider[]
  models: AiModel[]
}

/** POST /ai-models/sync：一次获取并导入，目录直接来自写库，供页面立即回填 */
export interface AiModelSyncResult {
  provider: AiProviderKey
  fetched: number
  added: number
  existing: number
  catalog: AiModelsData
}

/** PUT /ai-providers/{provider}：PATCH 语义；tier 传 null = 回代码默认；is_default false = 取消默认 */
export interface AiProviderPatch {
  is_default?: boolean
  tier_fast?: string | null
  tier_standard?: string | null
  tier_quality?: string | null
}

/** POST /ai-models */
export interface AiModelCreate {
  provider: AiProviderKey
  model_id: string
  display_name: string
  input_price?: number | null
  output_price?: number | null
  note?: string | null
  enabled?: boolean
}

/** PUT /ai-models/{id}：provider / model_id 不可改 */
export interface AiModelPatch {
  display_name?: string
  enabled?: boolean
  input_price?: number | null
  output_price?: number | null
  note?: string | null
  sort_order?: number
}

/** GET /ai-models/available 的 models[] 一项：厂商当前可用的模型 */
export interface AiRemoteModel {
  model_id: string
  display_name: string
  /** 已在目录里（添加弹层置灰，免得重复添加撞 409） */
  in_catalog: boolean
}

export interface AiAvailableModels {
  provider: AiProviderKey
  models: AiRemoteModel[]
}

/** POST /ai-models/{id}/test：上游失败也是 200，ok=false、error 为中文原因 */
export interface AiModelTestResult {
  ok: boolean
  latency_ms: number | null
  reply: string | null
  error: string | null
  tokens_in: number | null
  tokens_out: number | null
}

// ── AI 功能 ──

/** 任务的代码默认值：overrides 某项为 null 时就走这里（effort / 提示词），tier 决定默认模型 */
export interface AiTaskDefaults {
  /** 档位 fast / standard / quality；「跟随档位默认」的模型由它 + 生效厂商的档位映射得出 */
  tier: AiTier
  effort: AiEffort
  system_prompt: string
  /** 可含 placeholders 声明的占位符（如 {today}），调用时按变量替换 */
  user_template: string
}

/** 库里存的覆盖值：null = 该项跟随默认。版本快照也是这个形状（null = 当时走默认） */
export interface AiTaskOverrides {
  /** null = 跟随默认厂商 */
  provider: AiProviderKey | null
  /** null = 按 tier 取生效厂商的档位映射 */
  model: string | null
  effort: AiEffort | null
  system_prompt: string | null
  user_template: string | null
}

/** 后端按 §0 优先级算出的实际生效值；算不出（AI_PROVIDER 配错且无默认厂商）时 provider / model 为 null */
export interface AiTaskEffective {
  provider: AiProviderKey | null
  model: string | null
  effort: AiEffort
}

/** 任务会输出的顶层字段（由代码里的输出模型生成，只读）；type 是中文短语如「文本，可空」「列表」 */
export interface AiOutputField {
  name: string
  type: string
}

/** GET /ai-features 的 tasks[] 一项 */
export interface AiApplicationUsage {
  entry_label: string
  entry_path: string | null
  input_summary: string
  output_summary: string
}

export interface AiFeatureListItem {
  usage?: AiApplicationUsage
  configuration_notice?: string | null
  /** 任务名，如 resume.extract；详情路由参数用它 */
  name: string
  label: string
  module: string
  description: string
  /** 要不要带文件（简历提取要，润色 / 问答不要） */
  accepts_file: boolean
  placeholders: string[]
  effective: AiTaskEffective
  /** 有任何非 NULL 覆盖 */
  customized: boolean
  /** 无版本时 0 */
  current_version: number
  updated_by: string | null
  updated_at: string | null
}

/** 详情里的可选模型：只含已启用的目录模型 */
export interface AiCatalogOption {
  model_id: string
  display_name: string
}

/** GET /ai-features/{name} 的 providers[] 一项 */
export interface AiFeatureProvider {
  key: AiProviderKey
  label: string
  configured: boolean
  is_default: boolean
  /** 该厂商的生效档位映射；「跟随档位默认（当前 xxx）」的具体型号查它 */
  tier_models: Record<AiTier, string>
  models: AiCatalogOption[]
}

/** v1 单页 tasks[] 对象的全部字段；详情在此之上加文件、版本与厂商信息 */
export interface AiTaskSetting {
  usage?: AiApplicationUsage
  configuration_notice?: string | null
  /** 历史配置的当前可编辑投影；原始 overrides 保留历史信息。 */
  editable_overrides?: AiTaskOverrides
  name: string
  label: string
  module: string
  description: string
  /** user 模板支持的占位符名（不带花括号），如 ['today'] */
  placeholders: string[]
  defaults: AiTaskDefaults
  overrides: AiTaskOverrides
  effective: AiTaskEffective
  /** 文本任务为空数组 */
  output_fields: AiOutputField[]
  /** 从没改过时为 null */
  updated_by: string | null
  updated_at: string | null
}

/** GET /ai-features/{name}，也是 PUT 与恢复版本的响应体 */
export interface AiFeatureDetail extends AiTaskSetting {
  accepts_file: boolean
  current_version: number
  providers: AiFeatureProvider[]
  /** 默认厂商 → 环境变量 → claude；环境变量配错且无默认厂商时为 null */
  effective_default_provider: AiProviderKey | null
}

/**
 * 功能配置的五个可覆盖键：PATCH 语义，没传的键不动，显式 null = 恢复该项默认。
 * 也是试运行 draft 的形状：缺省的键 = 用当前配置，null = 代码默认。
 */
export interface AiTaskSettingPatch {
  provider?: AiProviderKey | null
  model?: string | null
  effort?: AiEffort | null
  system_prompt?: string | null
  user_template?: string | null
}

/** PUT /ai-features/{name}：note ≤200 只进版本行，不算配置改动 */
export interface AiFeatureUpdate extends AiTaskSettingPatch {
  note?: string | null
}

/** GET /ai-features/{name}/versions 的 versions[] 一项（新到旧，最多 50 条） */
export interface AiFeatureVersion {
  version_no: number
  created_by: string
  created_at: string
  note: string | null
  /** 与上一版比改了哪些键（v1 与代码默认比）：provider / model / effort / system_prompt / user_template */
  changed_fields: string[]
  /** 保存后的覆盖快照，null = 当时走默认 */
  snapshot: AiTaskOverrides
}

/** POST /ai-features/{name}/test-run 的 data：结构化任务 output 为对象、text 为 null；文本任务反之 */
export interface AiTestRunResult {
  provider: AiProviderKey
  model: string
  effort: AiEffort
  output: Record<string, unknown> | null
  text: string | null
  tokens_in: number | null
  tokens_out: number | null
  latency_ms: number | null
  cost_usd: number | null
  request_id: string | null
}

/** 企微推送样式实验室（POST /wework-lab/send）的五种探测样式 */
export type WeworkLabStyle =
  | 'textcard'
  | 'textcard_probe'
  | 'news'
  | 'image'
  | 'gif'
  | 'template_card'
  | 'mpnews'

/** 企微样式实验室发送结果：result 是企微真实回执，payload 是实际发出去的消息体 */
export interface WeworkLabSendResult {
  style: WeworkLabStyle
  /** 实际接收人 userid（留空时后端回落为当前登录账号） */
  touser: string[]
  result: { sent: number; failed: number; errors: string[] }
  /** 实际发给企微的 msgtype 消息体（textcard/news/template_card 的值，image 为 {media_id}） */
  payload: Record<string, unknown>
  /** 该样式的观察要点（后端常量，省得每次翻文档） */
  notes: string[]
}

/** 预览结果：payload 就是 send 会发的那份，前端只负责按企微版式画出来 */
export interface WeworkLabPreview {
  style: WeworkLabStyle
  payload: Record<string, unknown>
  notes: string[]
}

/** 专属生日贺卡：image 是生成结果的 data URL（可直接塞 img src） */
export interface WeworkLabCardResult {
  touser: string
  name: string
  /** 回显请求的贺卡类型，预览图与发送用的是同一套背景 */
  kind: 'birthday' | 'anniversary'
  bytes: number
  /** data:image/jpeg;base64,... */
  image: string
  /** dry_run 时为 null */
  result: { sent: number; failed: number; errors: string[] } | null
  media_id?: string
}

/** 生日贺卡发送记录（审计）：来自 portal_wecom_push_log，自动任务与手动发送同表 */
export interface WeworkLabCardLog {
  username: string
  content: string
  ok: boolean
  error: string | null
  /** ISO UTC */
  pushed_at: string | null
}
