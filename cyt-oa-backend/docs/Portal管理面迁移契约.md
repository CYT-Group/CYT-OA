# Portal 管理面迁移契约

> 版本 v1 ｜ 2026-11-10 ｜ 状态：迁移进行中（批次 1、2 已执行，批次 3、4 规划中）
> 口径来源：`D:\code\CYT-Portal`（老门户，FastAPI + PostgreSQL）→ 本仓库（CYT OA，Java/Spring Boot + MySQL）
> 配套脚本：`scripts/migrate_portal_identity.py`（批次 1）、`scripts/migrate_portal_content.py`（批次 2）、`scripts/portal_time.py`（时间换算）

> 2026-11-10 更新：本轮已将截图中的「后台管理 / HR 管理」接入 OA，并执行本机增量迁移；新增培训/HR 编辑器、物资、品牌、AI、合同、庆祝、简历面试及旧管理入口切流。当前落地范围、5 个历史孤立文档索引、附件缺失和部署配置以 [两组管理迁移执行记录](portal-admin-migration.md) 为准。下文批次规划保留为迁移历史，不代表所有旧状态仍有效。

## 0. 目标架构与原则

1. **OA 是唯一数据处理与管理中心**：所有写操作（审批、配置、台账、人事、上传、AI 配置）只在 OA 发生；唯一业务库是 OA MySQL。
2. **门户只做只读展示**：门户重写为**纯静态前端**（Vue 3 + Vite），直接调 OA 的 Open API（`/api/portal/*`）；不再有 Python 后端，不再读写 Portal PG。
3. **身份统一在 OA**：企微为身份源；OA 完成登录与 SSO 签发，门户 token 即 OA token。
4. **能力位不改名**：Portal 的 `can_*` 能力位原样映射为 OA 权限串（按钮级 `sys_menu`），SSO token 携带同一批字符串，直接复用 Portal 时代的能力矩阵。
5. **迁移幂等可重入**：行 id 原样保留，MySQL 已存在的 id 跳过；中途失败可重复执行同一命令接着跑。脚本均支持 `--dry-run`（只报数不写库）。

---

## 一、已迁移域（批次 1 / 2）

### 1.1 身份与权限（批次 1 · 已执行）

脚本：`scripts/migrate_portal_identity.py`

| Portal PG | OA MySQL | 迁移口径 |
|---|---|---|
| `portal_departments` | `sys_dept` | 由 parent_id 链现算全路径 `path`（`/1/100/` 形式）；`leader_userids` 落 JSON |
| `portal_users` | `sys_user` | Portal username 即企微 userid，按 username + wecom_userid **双键认人**（本地 admin 不与企微同名账号撞行） |
| `portal_users.birthday / hire_date` | `hrm_employee` | 168 份生日与入职日期只存在于 Portal，落员工档案 |
| `portal_roles` | `sys_role` | code = Portal 角色 key；已有 admin/hr 角色**不覆盖** |
| `portal_role_permissions` | `sys_role_menu` | 能力位 → `type='button'` 的 `sys_menu`（permission=能力位字符串）；权限矩阵 → 角色-菜单关联 |
| `portal_admin_audit` | `sys_operate_log` | `user_type=2`「后台管理」；`target_id` 取数字前缀当业务编号 |
| `portal_employee_sync_jobs` | `sys_employee_sync_job` | **只建表不搬数据**（主键 PGUUID、状态带租约，是运行时态） |

### 1.2 内容管理（批次 2 · 已执行）

脚本：`scripts/migrate_portal_content.py`

| Portal PG | OA MySQL |
|---|---|
| `portal_categories` | `portal_categories` |
| `portal_news` | `portal_news` |
| `portal_news_likes` | `portal_news_like` |
| `portal_news_comments` | `portal_news_comment` |
| `portal_wiki` | `portal_wiki` |
| `portal_page_views` | `portal_page_views` |

- 行 id **原样保留**：点赞/评论按 news_id 引用动态，重新编号会打断关系。
- `parent_id` 为空的先行插入（categories / comments 的 self-FK 不吃未来 id）。
- 埋点表按 500 行/批写入（`max_allowed_packet` 限制）。

### 1.3 已上线的管理面与 Open API（`module/portal`）

管理面（`/admin-api/portal/*`，OA 前端「门户内容」分组，见 `cyt-oa-frontend/src/configs/pages.ts`）：

- `news`：page / get / create / update / delete / approve / reject + 点赞评论管理
- `wiki`：page / get / create / update / delete / approve / reject
- `category`：分类树 CRUD + 排序
- `analytics`：浏览看板（汇总 / 日趋势 / 热门页 / 活跃用户 / 用户历史）

Open API（`/api/portal/*`，供门户纯静态前端直连）：

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/portal/categories/list?scope=` | 分类树 |
| GET | `/api/portal/news/page` `/get` `/engagement` | 动态列表 / 详情 / 互动计数 |
| POST / DELETE | `/api/portal/news/like` `/comment` | 点赞 / 评论（门户写操作也走 OA） |
| GET | `/api/portal/wiki/page` `/get` | 知识库列表 / 详情 |
| POST | `/api/portal/analytics/track` | 门户 PV 埋点 |

---

## 二、类型转换口径

| Portal PG 类型 | 处理方式 |
|---|---|
| `ARRAY(String)` | 元素统一转字符串后落 JSON 数组（Java 端按 `List<String>` 读，防止 `"423"` 变数字） |
| `boolean` | `tinyint(1)`（0/1） |
| `jsonb` | 文本原样交给 MySQL JSON 列校验 |
| 时间戳 | 经 `portal_time.py` 换算：Portal PG 服务端时钟在 **2026-07-23 从 UTC 切到 Asia/Shanghai**，历史时间戳须按 `server_clock()` 换算后落库，否则旧审计全变「迁移那一刻」 |

---

## 三、待迁移域（批次 3 / 4 规划）

### 3.1 门户内容剩余域（批次 3）

| Portal 模块 | 管理面（迁 OA） | 门户保留的展示面 | OA 落点建议 |
|---|---|---|---|
| `training.py` 培训中心 | 课程 CRUD、发布、必修配置、进度统计 | `/training/*` 浏览与学习；学习进度写操作走 OA | `module/portal` 新增 training 子域 |
| `events.py` 展会日历 | 展会 CRUD | `/events`、`/event-detail` | `module/portal` events 子域 |
| `products.py` 新品动态 | 新品 CRUD | `/product-center` 等展示页 | `module/portal` products 子域 |
| `hr_documents.py` HR 文档 CMS | 双人审 CMS、版本快照 | 49 个 HR/SOP 路由的 cmsOrLegacy 渲染 | `module/portal` hrdoc 子域（**保留双人审 + 版本快照语义**） |
| `handbook_signoff.py` 制度签收 | 签收看板、催签 | 员工签收动作（写 OA） | ✅ 已执行：签收 `/api/portal/handbook/sign` + 我的签收 `/api/portal/handbook/my-signoffs`（门户已切 OA）+ 签收统计 `/admin-api/portal/handbook/signoff-stats`；表 `portal-signoff-schema.sql`、迁移脚本 `scripts/migrate_portal_signoff.py`；**催签 notify-unsigned 依赖企微推送，随推送切片落地** |
| `brand_docs.py` 品牌手册库 | 上传 / 替换 / 回滚 | `/brand-doc/:brand/:type` 展示 | `module/portal` brand-doc 子域 |

### 3.2 人事 / 协作 / 边缘域（批次 4）

| Portal 模块 | 去向 |
|---|---|
| `samples.py` 样品库管理 | OA `module/oa` 新增样品台账 + 申请流程 |
| `celebrations.py` / `wework_lab.py` 庆祝日历 + 生日贺礼 | OA（hrm 或 oa 模块），推送复用 OA 企微通道 |
| `resumes.py` 简历识别 | 候选人结构化台账已由 ruoyi-hrm recruit 模块覆盖（已迁入 OA `module/hrm` recruit，含候选人/面试/渠道/岗位/岗位类型，表在 `hrm-schema.sql` + `dict-seed.sql`）；AI 解析「识别」部分依赖 AI 基建，随 AI 域一并落地 |
| `api_costs.py` API 成本看板 | OA system 或独立监控页 |
| `ai.py` / `ai_models.py` / `ai_settings.py` AI 管理 | OA 新增 AI 配置域（渠道 / 模型 / 提示词） |
| `feedback.py` 员工反馈 | ✅ 已执行：管理面 `/admin-api/portal/feedback/page`（can_view_dashboards）+ 员工提交 Open API `/api/portal/feedback/submit`（门户已切 OA）；表 `portal-feedback-schema.sql`、迁移脚本 `scripts/migrate_portal_feedback.py`；AI 富化（sentiment/ai_summary/状态流转/指派）随 bot 落地 |
| `esports.py` 电竞室 | ✅ 已执行：4 张表 `portal-esports-schema.sql`、迁移脚本 `scripts/migrate_portal_esports.py`、OA Open API `/api/portal/esports/*`（设置驱动多级审批 + 独占时段冲突检测，服务层能力判定 `can_manage_esports`/`can_view_all_esports`，门户 `esports.ts` 已切 OA）；通知（wecom_sync）依赖企微推送，随推送切片落地 |
| `wecom_bot.py` 企微智能助手（bot schema 4 表） | 迁 OA（新增 `module/bot` 或并入 portal） |
| `chat.py` | 随 bot 一并评估 |
| `upload.py` 文件上传（GCS） | OA 通用文件服务；门户展示文件改走 OA 文件 URL |
| `directory.py` 通讯录 | 已被 OA system/hrm + 企微同步覆盖，门户收尾下掉 |
| `messages.py` / `reports.py` / `my.py` | OA `module/me` 已覆盖，门户下掉 |

---

## 四、迁移批次与执行顺序

- **批次 1 身份与权限** ✅ 已执行（脚本保留，可重复执行）
- **批次 2 内容域** ✅ 已执行
- **批次 3-1 培训中心** ✅ 已执行（本切片：4 张数据表 `portal-training-schema.sql`、迁移脚本 `scripts/migrate_portal_training.py`、OA 课程/课时/进度管理 API 与读侧 Open API、OA 前端「门户内容-培训课程」页、门户员工端展示页切 OA（`frontend/src/api/oa.ts` 接缝 + Vite `/api/oa` 代理）。**已完成后置切片**：媒体预签名（OA 接入 MinIO 客户端 `PortalMediaStorage`）、NAS 目录导入（`PortalTrainingTreeParser` + `/portal/training/import/scan`）、培训统计看板（`/portal/training/stats`）；门户培训管理页暂留至 OA 课时/分类管理 UI 齐平后下线）
- **批次 3 门户内容剩余域**：展会 / 新品 / HR 文档 / 签收 / 品牌手册
- **批次 3-4 HR 文档 CMS** 🚧 已执行「后端地基 + 门户读侧切流」：5 张表 `portal-hrdoc-schema.sql`、迁移脚本 `scripts/migrate_portal_hr_documents.py`、OA 管理面 `/admin-api/portal/hrdoc/*` 与读侧 Open API `/api/portal/hrdoc/*`、领域校验（19 种块/封面/签收）、Portal 员工端读侧切 OA Open API（`articleApi` 读方法 + `oa.ts` 错误码 1-060-800 映射）。**剩余**：OA 控制台编辑器（19 种块编辑器）、企微推送（需核对 OA `WecomGatewayClient`）、AI 智能排版（依赖 OA 尚无的 AI 基建）。
- **批次 4 人事 / 协作 / 边缘域**：样品 / 庆祝 / 简历 / AI / 反馈 / 电竞 / bot / 上传
- **批次 4-1 电竞室** ✅ 已执行：4 张表 `portal-esports-schema.sql`、迁移脚本 `scripts/migrate_portal_esports.py`（时间列统一 `app_utc` 恒 +8h，因 Portal `application/esports.py` 全部用 `datetime.utcnow()` 写）、OA Open API `/api/portal/esports/*`（`PortalEsportsOpenController` + 领域规则 `EsportsRules` + `PortalEsportsServiceImpl` 持锁 settings→booking→steps 串行化）、能力位 `can_manage_esports`/`can_view_all_esports`（`PortalCapability`）、错误码段 `1-060-920`、门户 `esports.ts`/`esports.test.ts` 切 OA + `oa.ts` 新增 `oaPut` 与 1-060-920 映射。**剩余**：通知（wecom_sync）随企微推送切片落地；多部门名在审批人/参加人列表收敛到主部门。

每个模块的标准切换顺序：

1. OA 建表（`src/main/resources/db/` 脚本）
2. 迁移脚本 dry-run → 正式执行（幂等）
3. OA 管理面上线（`/admin-api/portal/*` + 前端「门户内容」分组）
4. Open API 上线（`/api/portal/*`）
5. 门户对应管理页下线，展示页切流到 Open API
6. Portal 对应路由与 PG 表冻结

---

## 五、门户重写方案（纯静态）

1. 门户 = Vue 3 静态站（Vite 构建），托管 CDN / 静态存储，**无 Python 后端**。
2. 登录：跳 OA 企微 OAuth 换取 OA token；门户不再自行签发 JWT。
3. 数据：全部通过 `/api/portal/*` Open API；点赞 / 评论 / 签收 / 学习进度等写操作同样走 OA。
4. 埋点：路由切换 `POST /api/portal/analytics/track`。
5. 子系统 SSO（sales / rd / marketing …）：由「门户换短时 token」改为从 OA 换；门户保留的静态跳转改为跳 OA SSO。
6. Python 后端（37 个 router）在对应模块切流完成后退役；Portal PG 在全部批次完成后归档下线。

---

## 六、验收标准

- **管理面等价**：OA 管理页可完成原门户管理页全部操作（含审批、看板、催签、回滚）。
- **展示面切流**：门户展示页数据来自 OA Open API；门户管理页已下线。
- **数据完整**：抽样核对行数与关键字段；news id 与点赞/评论关联不断。
- **身份一致**：门户登录后 token 由 OA 签发，能力位行为与 Portal 时代一致。
- **可回滚**：每批次保留 Portal 侧路由冻结点，可退回旧实现。

---

## 附：能力位清单（Portal → OA 权限串）

迁移脚本 `CAPABILITY_LABELS` 中的 33 个能力位原样作为 OA 权限串使用：

| 能力位 | 中文名 |
|---|---|
| `can_enter_admin` | 进入管理中心 |
| `can_manage_users` | 管理用户 |
| `can_manage_roles` | 管理角色权限 |
| `can_manage_directory` | 管理通讯录 |
| `can_import` | 批量导入 |
| `can_create_content` | 创建内容 |
| `can_edit_any` | 编辑任意内容 |
| `can_review` | 审核内容 |
| `can_delete` | 删除内容 |
| `can_view_any_status` | 查看任意状态内容 |
| `can_view_own_draft` | 查看本人草稿 |
| `can_moderate_comments` | 管理评论 |
| `can_edit_hr_documents` | 编辑 HR 文档 |
| `can_view_hr_sop` | 查看 HR SOP |
| `can_view_signoff_stats` | 查看签收统计 |
| `can_manage_training` | 管理培训 |
| `can_manage_events` | 管理展会活动 |
| `can_manage_products` | 管理新品 |
| `can_manage_brand_docs` | 管理品牌手册 |
| `can_manage_materials` | 管理物品台账 |
| `can_approve_any_material_apply` | 审批任意物品申请 |
| `can_configure_site_owners` | 配置站点负责人 |
| `can_manage_resumes` | 管理简历识别 |
| `can_manage_celebrations` | 管理庆祝日历 |
| `can_generate_contracts` | 生成劳动合同 |
| `can_send_messages` | 发送消息 |
| `can_manage_esports` | 管理电竞室 |
| `can_view_all_esports` | 查看全部电竞预约 |
| `can_view_all_reports` | 查看全部汇报 |
| `can_view_dashboards` | 查看数据看板 |
| `can_view_api_costs` | 查看 API 成本 |
| `can_manage_ai_settings` | 管理 AI 配置 |
| `can_manage_ai_keys` | 管理 AI 密钥 |
