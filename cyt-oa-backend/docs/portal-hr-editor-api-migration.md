# HR 制度编辑器接口迁移

> 数据库使用 PostgreSQL，初始化和升级统一通过 `src/main/resources/db/postgresql/migration/` 的版本迁移。
> 操作见[后端 README](../README.md#3-建表与初始数据)与[数据库规范](../src/main/resources/db/postgresql/migration/README.md)。

核对来源为 `CYT-Portal/frontend/src/views/admin/HrDocEditorView.vue`、`HrDocAdminView.vue`、`HrDocPreviewView.vue` 及其实际使用的历史、排版、推送、封面设置组件。新接口统一 `/admin-api/portal/hrdoc`，能力位统一 `can_edit_hr_documents`。员工公开读制度仍使用 app 接口；管理接口允许编辑人读取草稿，不应用员工部门范围过滤。

## 文档、草稿与版本

- 旧 `GET /articles/admin/list` → `GET /page?pageNo=1&pageSize=100&deleted=false`。返回 OA 分页 envelope；原页面本地筛选时由 bridge 聚合所有分页。回收站传 `deleted=true`。
- 旧 `GET /articles/admin/{id}` → `GET /get?id=`。详情包含正式列、全部 `draft*`、`legacySlug`、`canonicalUrl`、`revision`、`publicationState`、`lastPush`。
- 旧 `POST /articles/admin` → `POST /create`。原块编辑器先以空 HTML 创建，再 `/update` 保存 blocks 的流程可以保留。
- 旧 `PUT /articles/admin/{id}` → `PUT /update`，body 增加 `id`。保留 PATCH 语义；`blocks` 与 `contentHtml` 互斥，空数组/空串表示清空，null 表示本次不改。
- 创建、保存、发布、下架、重新上架、移入及恢复回收站均返回完整详情和本次快照的 `publicationState`；发布凭据由同一份写入结果计算，避免另查详情夹入他人改稿。
- 旧 `POST /articles/admin/{id}/publish` → `POST /publish?id=`，body `{expectedRevision?,version?,notifyWecom?,notifyDeptScope?}`。原编辑器保存后使用返回凭据；列表快捷发布需 bridge 取最新凭据。正文修改且沿用已发布版本号仍报错；仅设置变更可沿用版本且保留原签收。
- 旧 `offline/restore/undelete` → 同名路径 + `?id=`。旧 DELETE 主文档 → `DELETE /delete?id=`。下架与删除独立，重新上架不升版本。
- 发布版本 `GET /versions?id=` 和 `GET /versions/{version}?id=`；草稿版本 `GET /draft-versions?id=`、`GET /draft-versions/{versionId}?id=`、`DELETE /draft-versions/{versionId}?id=`。
- 原“恢复草稿版本”是读取快照后载入编辑器内存，再由用户明确保存；没有恢复即发布的服务端 API。发布快照不可删除或覆盖。
- 旧 slug 检查 → `GET /slug-available?slug=&exclude=`；旧编辑深链（包括历史 alias）→ `GET /resolve?slug=`，返回 `{id}`。解析不要求已发布，也不应用员工可见部门过滤。

## 全部文档设置与预览

已有字段全量保留：引言 `intro`、主题 `theme`、封面图 `coverImage`、标签 `tags`、目录 `tocEnabled/tocPosition`、整屏封面 `cover`、PDF 下载开关 `downloadPdfEnabled`、签收卡 `signoff`，正式/草稿双列按原三态晋升。整体 cover/signoff JSON 键名保留原值，例如 `bg_type`、`nav_tabs`、`ack_label`、`button_text`。`blocks` 内 `activeTab/subHeader/document_id/button_text` 等原键不做递归命名转换。

发布凭据递归规范化所有 JSON 对象键顺序，并按秒精度规范化状态时间；JSON 序列化的键顺序不会造成“保存后立即发布”误报并发冲突。

预览读 `/get`，客户端自行选择草稿或发布版本。原 PDF 是浏览器打印/CSS 导出，无服务端 PDF 生成 API。图片仍通过已迁移的 multipart 图片上传 API；新私有卷保存图片，公开图片读路径供渲染使用。

服务端 HTML 清洗使用只用于协议验证的可信虚拟 base，并保留原相对链接；新上传图片路径、站内文档链接和目录锚点不会在保存时丢失。仍拒绝 javascript/data URL、事件属性和危险 CSS，保留表格 class、单元格列宽、表头底色和引言/引用常用安全格式。原 HR 文档 ID 6 引用 `/api/portal/uploads/images/b2e2421af8b6.png`：旧仓本地无原件且对应云对象不可取，本次保留原引用；此资源缺口与已记录的品牌/简历原件缺失一致，不伪造图片或兼容下载成功。

## 智能排版与 Word 导入

- `POST /layout` body `{text}` → `{blocks,summary,sourceText}`，同步返回。
- `POST /layout-file` multipart `file` → 同上。允许 docx/txt/md/csv/PDF/PNG/JPEG/WebP，最大 10MB；扩展名与二进制 magic bytes 校验。PDF/图片通过托管 AI 附件通道识别。
- `POST /layout/jobs` body `{text,sourceType:'paste'|'upload'|'current',sourceName?}` → 任务详情。
- `POST /layout/jobs/file` multipart `file` → 任务详情。与旧异步链路一致，只接受可先提取成文本的 docx/txt/md/csv；PDF 提示先转换为 docx。Word 段落和表格按正文顺序提取；XML 禁用 DTD/外部实体，限制压缩条目和解压量，不执行宏或下载外链。
- `GET /layout/jobs` → 当前提交人最近 20 个任务，省略 blocks；`GET /layout/jobs/{id}` → 详情带 blocks。其他人的任务不可查询。
- `DELETE /layout/jobs/{id}` → 仅成功/失败任务可删除，进行中任务拒绝。
- `POST /layout/jobs/{id}/retry` → 仅自己的失败任务可重试。旧 UI 有重试入口但旧后端缺路由，本次补全。
- 任务字段：`id,status,sourceType,sourceName,inputChars,progress,stage,error,summary,blockCount,createdAt,finishedAt`；详情再加 `blocks`。状态是 `pending/running/succeeded/failed`。创建返回后后台执行；重启将进行中的任务标记失败，允许明确重试。
- 原文最多 20000 字；每人每小时 20 次，包含重试，同步与异步共享配额。长文按章节及 1800 字拆分、最多 3 段并发，反馈结构错误后重试一次。模型遗漏的原文安全补入文本块；部分段落失败保留原文并明确提示；所有段落失败不能报排版成功。
- 输出复用同一份文档块字段形状和 HTML 清洗。任务结果只保存在任务表，点击应用后进入编辑器内存；不自动保存、发布或改变制度状态。后台 AI 使用显式提交人审计，不依赖已结束请求。

## 部门、收件人与企微推送

- `GET /departments` → 有效部门名称 `string[]`；`GET /departments/stats` → `{name,userCount}[]`，供原 DeptScopePicker 真实人数展示，不要求 `system:dept:query`。
- `GET /users/search?q=` → 匹配用户最多 20 条，空查询返回空数组。条目 `{username,displayName,department:string[],isActive:boolean}`；包含停用员工供 UI 标注原因，不提供空搜索全量名单。
- `POST /compat/notify?id=` body `{audience:'all'|'scope'|'users',userIds?,notifyDeptScope?,force?,customMessage?}`。只推已发布且在线的正式快照；允许正式内容仍在线但另有草稿的文档。无明确选择不会调用该 API。
- 与旧推送对话框一致，文档未限定部门时第一项是显式 `all`，全员不限人数；`scope/users` 保留 50 人防误发限制。scope 空时按全员候选计算并继续校验上限，不能假成功空发送；点名用户须存在且在职。
- `POST /compat/test-push-to-me?id=` body 可选 `{title,version,requiresSignoff}`。优先测试输入、其次草稿、最后正式值；仅发当前人，强制绕过冷却，使用独立 `hr_doc_test` 审计，绝不写文档草稿或状态。
- 两个兼容推送同步等待真实网关结果：`sent,failed,skipped,noRecipients,errors,status,jobId,targetCount,allowRetry`；冷却时还有 `lastPushedAt,cooldownHours`。仅 enqueue 成功不能伪装发送成功。消息链接由 `PORTAL_DISPLAY_BASE_URL` 替换为实际员工入口；未配置时记录真实失败，不发不可用链接。
- 普通 OA `/notify` 与发布时显式 `notifyWecom` 仍保留现有提交后队列通道；原编辑器推送弹窗使用 `/compat/*` 以保留同步结果交互。

## 签收看板的独立元数据权限与统计口径

签收看板不再借用编辑接口获取文档选项：`GET /admin-api/portal/handbook/documents/options` 只要求 `can_view_signoff_stats`，返回 `{id,title,version,requiresSignoff,publishedAt,offlineAt}[]`。只列已发布、需签收且未删除的正式快照元数据；保留下架条目供历史核对，不应用员工部门/SOP阅读过滤，也不返回正文、草稿或编辑设置。HR 草稿管理接口仍要求 `can_edit_hr_documents`。

统计严格使用本次指定版本（未指定则当前已发布版本）。稳定文档 ID、当前 slug、该文档历史 alias、`/hr-doc/` 路径对应 slug，以及 `handbook/employee-handbook`、`incentive/hr-incentive` 两组旧标识统一为查询候选；有稳定 `document_id` 的证据以该 ID 为准，只有旧 `document_id IS NULL` 的证据才按字符串补匹配，历史字符串和时间均不重写。旧 legacy 下拉入口仍兼容。

两组固定 legacy 制度还兼容旧静态版本：仅历史统计时，若迁入 alias 指向未发布、不需签收或没有有效版本号的索引，仍回退 `handbook=v1.0`、`incentive=v2.0`；已有有效正式 CMS 版本时优先使用正式版本。此兼容不应用于普通未知 slug、稳定 ID 或指向不可用 CMS 文档的签收/催签执行请求。

目标集合沿用旧门户 `is_active + department overlap` 语义：OA 中只纳入 enabled 员工，主部门或任一兼任部门命中制度适用范围即可；未限定范围则纳入全体在职员工。已签人数为“本次版本签收用户 ID 集合”和“当前目标用户 ID 集合”的交集，同人多条别名/ID签收只计一次，已离职/转出范围人员的旧证据保留但不增加当前覆盖率。不存在靠截断百分比掩盖超额的问题。

## 历史任务迁移与验证

任务表 `portal_hr_layout_jobs` 和限流记次表 `portal_hr_layout_attempts` 已纳入 PostgreSQL 初始迁移。先按[后端 README](../README.md#3-建表与初始数据)初始化目标库，再使用 `scripts/migrate_portal_hr_layout_jobs.py` 搬迁历史任务；脚本默认 dry-run，`--apply` 才写入。保留源任务 ID、输入、成功 blocks、提交人、原始 aware 时间；按 username 映射新用户。源 `pending/running` 转失败并注明迁移中断，不自动调用付费 AI；未映射提交人或目标 ID 内容冲突均报错，绝不覆盖新 OA 任务。

HR 本域 22 项 Java 单测及 3 项 Python 纯转换测试通过，覆盖结构补齐与过滤、相对图片/链接及表格格式保存、危险 HTML/CSS 清洗、遗漏原文保留、章节分段、Word 表格/段落顺序与英文空格、DTD/实体及压缩膨胀拒绝、文件 magic bytes、超长正文、测试推送不改稿、在线草稿正式快照推送、全员/定向上限、网关失败不伪装成功、历史别名管理解析、JSON 重排后的发布凭据和带时区任务迁移。实际 AI 付费调用、真实企微发送和生产库写入需另行联调，本次单测不触发。
