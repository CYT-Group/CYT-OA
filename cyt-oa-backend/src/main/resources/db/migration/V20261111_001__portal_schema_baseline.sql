-- 首批纳管：兼容已初始化的 OA 库及已手工补齐门户表的测试库。
-- 从此文件开始，所有后续数据库变化通过新增版本文件交付；禁止修改已执行文件。

-- ===== 历史来源：portal-ai-schema.sql（本文件内冻结副本） =====
-- 迁自 CYT-Portal models.py；保留旧列和主键，历史关系不重新编号。

CREATE TABLE IF NOT EXISTS `portal_ai_models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider` VARCHAR(16) NOT NULL,
  `model_id` VARCHAR(64) NOT NULL,
  `display_name` VARCHAR(100) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `input_price` DECIMAL(10,4) NULL,
  `output_price` DECIMAL(10,4) NULL,
  `note` VARCHAR(200) NULL,
  `sort_order` BIGINT NOT NULL DEFAULT '0',
  `created_by` VARCHAR(50) NOT NULL,
  `updated_by` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_ai_model(provider,model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_ai_provider_settings` (
  `provider` VARCHAR(16) NOT NULL,
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  `tier_fast` VARCHAR(64) NULL,
  `tier_standard` VARCHAR(64) NULL,
  `tier_quality` VARCHAR(64) NULL,
  `updated_by` VARCHAR(50) NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `api_key_ciphertext` LONGTEXT NULL,
  `api_key_hint` VARCHAR(8) NULL,
  `api_key_updated_by` VARCHAR(50) NULL,
  `api_key_updated_at` DATETIME NULL,
  PRIMARY KEY (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_ai_task_settings` (
  `task_name` VARCHAR(64) NOT NULL,
  `provider` VARCHAR(16) NULL,
  `model` VARCHAR(64) NULL,
  `effort` VARCHAR(8) NULL,
  `system_prompt` LONGTEXT NULL,
  `user_template` LONGTEXT NULL,
  `updated_by` VARCHAR(50) NOT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_ai_task_setting_versions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_name` VARCHAR(64) NOT NULL,
  `version_no` BIGINT NOT NULL,
  `provider` VARCHAR(16) NULL,
  `model` VARCHAR(64) NULL,
  `effort` VARCHAR(8) NULL,
  `system_prompt` LONGTEXT NULL,
  `user_template` LONGTEXT NULL,
  `note` VARCHAR(200) NULL,
  `created_by` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_ai_task_version(task_name,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_api_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider` VARCHAR(32) NOT NULL,
  `model` VARCHAR(64) NULL,
  `endpoint` VARCHAR(128) NOT NULL,
  `tokens_in` BIGINT NULL,
  `tokens_out` BIGINT NULL,
  `cost_usd` DECIMAL(10,6) NULL,
  `latency_ms` BIGINT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ok',
  `error_msg` LONGTEXT NULL,
  `called_by` VARCHAR(50) NULL,
  `called_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 历史来源：portal-hrdoc-schema.sql（本文件内冻结副本） =====
-- =====================================================================
-- B3 HR 文档 CMS：Portal 的行政人事文档（草稿/发布双列模型）+ 旧地址别名 +
-- 固定入口绑定 + 发布版本快照 + 草稿版本快照，搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §一「HR 文档 CMS」、§二「类型转换口径」、§四 批次 3。
-- 与其他 db/*.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
-- 前置：无（本模块 5 张表自成一组；签收记录表属下一批次 handbook_signoff）。
--
-- Postgres → MySQL 的等价改写：
--   1) ARRAY(String) → JSON（dept_scope / tags，元素字符串化）；JSONB → JSON（blocks/cover/signoff）
--   2) Text → MEDIUMTEXT（content_html / draft_content 整篇 HTML，单块上限 200KB × 100 块，
--      64KB 的 TEXT 装不下，必须 MEDIUMTEXT）；Text(短) → TEXT（intro / offline_reason）
--   3) CHECK 约束保留（MySQL 8.0.16+ 生效），状态机/别名类型取值不靠应用层自觉
--   4) 双格式判别：content_blocks IS NOT NULL = 块格式（此时 content_html 存空串），
--      与 Portal 的 none_as_null 语义一致 —— JSON 列显式写 NULL 才是「老 HTML 格式」
-- =====================================================================

-- 1. 主表：行政人事文档（员工手册 / 奖惩 / SOP / HR 流程页）。
--    草稿/发布双列：content_* 是员工读的已发布快照，draft_* 是 admin 改稿区，publish 时晋升。
--    审批流已下线（v1.9），status 只剩 draft|published 会实际流转；pending/rejected 保留给存量数据。
CREATE TABLE IF NOT EXISTS portal_hr_documents (
    id                          BIGINT       NOT NULL AUTO_INCREMENT,
    slug                        VARCHAR(64)  NULL COMMENT '旧 URL 兼容身份；新文档不分配。删除不释放 slug',
    title                       VARCHAR(200) NOT NULL,
    category                    VARCHAR(32)  NOT NULL COMMENT 'handbook|incentive|hr-sop|hr|policy',
    topic                       VARCHAR(32)  NULL COMMENT '二级分类（制度中心 6 主题）；NULL=存量回落 admin',
    content_html                MEDIUMTEXT   NULL COMMENT '已发布快照（HTML 格式）；块格式文档此列为空串',
    draft_content               MEDIUMTEXT   NULL COMMENT '在编草稿（HTML 格式）；块格式草稿为 NULL',
    content_blocks              JSON         NULL COMMENT '已发布快照（块格式 JSON 数组）；NULL=老 HTML 格式',
    draft_blocks                JSON         NULL COMMENT '在编草稿（块格式）；与 draft_content 二选一非空',
    version                     VARCHAR(16)  NOT NULL DEFAULT 'v1.0',
    draft_version               VARCHAR(16)  NULL COMMENT '草稿目标版本号；发布后清空',
    status                      VARCHAR(20)  NOT NULL DEFAULT 'draft',
    requires_signoff            TINYINT(1)   NOT NULL DEFAULT 0,
    dept_scope                  JSON         NULL COMMENT '部门访问限制（部门名数组）；NULL=全员；原 PG ARRAY(String)',
    draft_title                 VARCHAR(200) NULL,
    draft_requires_signoff      TINYINT(1)   NULL,
    draft_dept_scope            JSON         NULL,
    intro                       TEXT         NULL COMMENT '引言（纯文本 ≤500 字）',
    draft_intro                 TEXT         NULL,
    cover_image                 VARCHAR(500) NULL COMMENT '封面图 https:// 或 / 开头；空串=主题渐变',
    draft_cover_image           VARCHAR(500) NULL,
    theme                       VARCHAR(32)  NULL COMMENT ''''' | 6 预设 | #RRGGBB',
    draft_theme                 VARCHAR(32)  NULL,
    tags                        JSON         NULL COMMENT '标签数组；原 PG ARRAY(String)',
    draft_tags                  JSON         NULL,
    toc_enabled                 TINYINT(1)   NOT NULL DEFAULT 1,
    draft_toc_enabled           TINYINT(1)   NULL,
    toc_position                VARCHAR(8)   NOT NULL DEFAULT 'left',
    draft_toc_position          VARCHAR(8)   NULL,
    cover                       JSON         NULL COMMENT '整屏封面配置（bg_type/image/label_*/icon/desc/date/scope/dept/nav_tabs）',
    draft_cover                 JSON         NULL,
    download_pdf_enabled        TINYINT(1)   NOT NULL DEFAULT 0,
    draft_download_pdf_enabled  TINYINT(1)   NULL,
    signoff                     JSON         NULL COMMENT '签收卡配置（icon/title/lead/ack_label/button_text/tip/signed_note）',
    draft_signoff               JSON         NULL,
    updated_by                  VARCHAR(50)  NULL,
    updated_at                  DATETIME     NULL,
    submitted_by                VARCHAR(50)  NULL,
    submitted_at                DATETIME     NULL,
    published_by                VARCHAR(50)  NULL,
    published_at                DATETIME     NULL COMMENT '判「已发布」用此列而非 status（改稿会切回 draft 但保留它）',
    offline_at                  DATETIME     NULL COMMENT '下架标记：员工端可见性 = published_at 非空 且 offline_at 空',
    offline_by                  VARCHAR(50)  NULL,
    offline_reason              TEXT         NULL,
    deleted_at                  DATETIME     NULL COMMENT '逻辑删除：进回收站，管理列表默认隐藏',
    deleted_by                  VARCHAR(50)  NULL,
    created_at                  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hr_documents_slug (slug),
    KEY ix_hr_documents_category (category),
    KEY ix_hr_documents_status (status),
    CONSTRAINT ck_hr_documents_status CHECK (status IN ('draft', 'pending', 'published', 'rejected'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'HR 文档 CMS 主表：草稿/发布双列 + 下架/逻辑删除 + 块/HTML 双格式';

-- 2. 旧地址别名：历史 slug / key / path → 稳定文档 ID 的兼容映射。
--    删除文档不释放 slug，别名也一并保留（对账语义）。
CREATE TABLE IF NOT EXISTS portal_hr_document_aliases (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    alias        VARCHAR(255) NOT NULL,
    alias_type   VARCHAR(20)  NOT NULL COMMENT 'slug|legacy_key|legacy_path',
    document_id  BIGINT       NOT NULL,
    created_by   VARCHAR(50)  NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hr_document_aliases_alias (alias),
    KEY ix_hr_document_aliases_document_id (document_id),
    CONSTRAINT fk_hr_document_aliases_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT,
    CONSTRAINT ck_hr_document_aliases_type CHECK (alias_type IN ('slug', 'legacy_key', 'legacy_path'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'HR 文档旧地址别名（历史 URL 兼容解析）';

-- 3. 固定入口绑定：业务入口 key → 稳定文档 ID（如 hr/index 页面的固定栏目）。
CREATE TABLE IF NOT EXISTS portal_hr_document_entry_bindings (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    entry_key    VARCHAR(64) NOT NULL,
    document_id  BIGINT      NOT NULL,
    created_by   VARCHAR(50) NULL,
    updated_by   VARCHAR(50) NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hr_document_entry_bindings_key (entry_key),
    KEY ix_hr_document_entry_bindings_document_id (document_id),
    CONSTRAINT fk_hr_document_entry_bindings_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'HR 文档固定入口绑定（业务入口 → 文档 ID）';

-- 4. 发布版本快照：publish 时写一份，劳动仲裁举证用，只增不改不可删。
CREATE TABLE IF NOT EXISTS portal_hr_document_versions (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    document_id    BIGINT       NOT NULL,
    slug           VARCHAR(64)  NULL COMMENT '发布时 slug 快照；新文档可为 NULL',
    version        VARCHAR(16)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content_html   MEDIUMTEXT   NOT NULL COMMENT '块格式快照此列存空串（判别靠 content_blocks）',
    content_blocks JSON         NULL COMMENT '块格式快照；NULL=老 HTML 版本',
    published_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by   VARCHAR(50)  NULL,
    approved_by    VARCHAR(50)  NULL,
    submitted_by   VARCHAR(50)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hr_doc_versions_doc_ver (document_id, version),
    UNIQUE KEY uq_hr_doc_versions_slug_ver (slug, version),
    KEY ix_hr_doc_versions_document_id (document_id),
    CONSTRAINT fk_hr_doc_versions_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'HR 文档发布版本快照（仲裁举证，只增不可删）';

-- 5. 草稿版本快照：admin 每次保存草稿自动存一份，可回看/恢复/删除（每文档只留最新 30 条）。
CREATE TABLE IF NOT EXISTS portal_hr_document_draft_versions (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    document_id    BIGINT       NOT NULL,
    slug           VARCHAR(64)  NULL,
    title          VARCHAR(200) NOT NULL,
    content_html   MEDIUMTEXT   NULL COMMENT 'HTML 快照；块格式快照此列为 NULL',
    content_blocks JSON         NULL COMMENT '块格式快照（判别同主表）',
    saved_by       VARCHAR(50)  NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_hr_doc_draft_versions_doc_created (document_id, created_at),
    CONSTRAINT fk_hr_doc_draft_versions_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'HR 文档草稿版本快照（工作历史，可删）';

-- ===== 历史来源：portal-hr-layout-jobs-schema.sql（本文件内冻结副本） =====
-- 原 HR 文档编辑器异步排版任务。仅保存提交人输入和排版结果，不发布/覆盖任何文档。
CREATE TABLE IF NOT EXISTS portal_hr_layout_jobs (
 id BIGINT NOT NULL AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 actor_username VARCHAR(100) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'pending',
 source_type VARCHAR(16) NOT NULL DEFAULT 'paste',
 source_name VARCHAR(255) NULL,
 input_text MEDIUMTEXT NOT NULL,
 input_chars INT NOT NULL,
 progress SMALLINT NOT NULL DEFAULT 0,
 stage VARCHAR(128) NOT NULL DEFAULT '等待处理',
 summary TEXT NULL,
 blocks JSON NULL,
 error TEXT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at DATETIME NULL,
 PRIMARY KEY(id),
 KEY ix_hr_layout_owner_created(user_id,created_at),
 KEY ix_hr_layout_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HR 编辑器智能排版任务（按用户隔离）';

CREATE TABLE IF NOT EXISTS portal_hr_layout_attempts (
 id BIGINT NOT NULL AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),
 KEY ix_hr_layout_rate(user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排版请求限额（包含重试，每人每小时20次）';

-- ===== 历史来源：portal-signoff-schema.sql（本文件内冻结副本） =====
-- =====================================================================
-- B3 制度签收：Portal 的 portal_handbook_signoff 搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §一「制度签收」、§二「类型转换口径」、§四 批次 3。
--
-- 签收记录：员工对某份 requires_signoff 文档的某个版本线上签收，人事档案存档。
-- UNIQUE(user_id, document, version) 保证一人对一版本只签一次（幂等）。
-- document_id 引用 portal_hr_documents.id（RESTRICT）；document 保留为历史字符串快照（slug）。
-- 催签（notify-unsigned）依赖企微推送通道，随推送切片落地；这里只迁签收记录本体。
-- =====================================================================

CREATE TABLE IF NOT EXISTS portal_handbook_signoff (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '签收人 sys_user.id（Portal 的 portal_users.id 原样保留）',
    username    VARCHAR(50)  NOT NULL COMMENT '签收人企微 userid 快照',
    document_id BIGINT       NULL COMMENT '稳定文档 ID → portal_hr_documents.id（RESTRICT）',
    document    VARCHAR(64)  NOT NULL COMMENT '文档标识快照（slug；老记录可能是 legacy alias）',
    version     VARCHAR(16)  NOT NULL,
    signed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip          VARCHAR(64)  NULL,
    user_agent  VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_handbook_signoff_user_doc_ver (user_id, document, version),
    KEY ix_handbook_signoff_document_id (document_id),
    CONSTRAINT fk_handbook_signoff_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '员工手册/制度文档线上签收记录（人事档案存档）';

-- ===== 历史来源：portal-training-schema.sql（本文件内冻结副本） =====
-- =====================================================================
-- B3 培训中心：Portal 的课程 / 课时 / 课时进度 / 课程级进度搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §一「培训中心」、§二「类型转换口径」、§四 批次 3。
-- 与其他 db/*.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
-- 前置：portal_categories 已在批次 2（portal-content-schema.sql）建好，category_id 的 FK 引用它，
--       迁移脚本 migrate_portal_training.py 里 category_id 天然满足 FK（id 原样保留）。
--
-- Postgres → MySQL 的等价改写：
--   1) ARRAY(String) → JSON（dept_scope，数组元素字符串化）；JSONB → JSON（pages）
--   2) CHECK 约束保留（MySQL 8.0.16+ 生效），状态机取值不靠应用层自觉
--   3) portal_training_progress 刻意无 FK、无唯一约束：Portal 原表如此
--      （课程删除后课程级进度留存；同一人同一门课可能有多条历史行）
-- =====================================================================

-- 1. 培训课程：一门课 = 若干课时（portal_training_lessons）。
--    老审批流作废，status 只剩 draft|published；发布要求至少一个课时。
--    category 文本列是 category_id 的过渡期镜像（S3a 停写、S3b 删列），老页面还在读它。
CREATE TABLE IF NOT EXISTS portal_training (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL,
    description     TEXT         NULL COMMENT '纯文本简介（不是富文本）',
    department      VARCHAR(50)  NULL COMMENT '老列：早期按单一部门字符串归属，新口径用 dept_scope，此列不再写',
    level           VARCHAR(20)  NOT NULL DEFAULT 'elective',
    duration_minutes INT         NULL,
    instructor      VARCHAR(100) NULL,
    doc_url         VARCHAR(500) NULL COMMENT '老列：老课程资料外链，重做后已各迁成一条 link 课时，不再写',
    video_url       VARCHAR(500) NULL COMMENT '老列：同上',
    cover_image     VARCHAR(255) NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',
    sort_order      INT          NOT NULL DEFAULT 0,
    category        VARCHAR(100) NULL COMMENT '小类名的过渡期镜像；事实源是 category_id',
    category_id     BIGINT       NULL COMMENT '小类 → portal_categories(scope=training.course, depth=2)；NULL = 员工端「未分类」兜底',
    source_prefix   VARCHAR(500) NULL COMMENT '导入幂等键（bucket 课程目录前缀），手建课程为 NULL',
    dept_scope      JSON         NULL COMMENT '可见部门名单（部门名），NULL = 全员；原 PG ARRAY(String)',
    author_username VARCHAR(50)  NULL COMMENT '手建课程的创建人；导入课程为 NULL',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_training_source_prefix (source_prefix),
    KEY ix_training_status (status),
    KEY ix_training_category_id (category_id),
    CONSTRAINT fk_training_category FOREIGN KEY (category_id) REFERENCES portal_categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_training_level CHECK (level IN ('required', 'elective')),
    CONSTRAINT ck_training_status CHECK (status IN ('draft', 'published'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '培训课程：draft|published + 小类归属 + 部门可见范围';

-- 2. 培训课时：课程下的一个可播放/可阅读单元。kind 决定哪几列有值：
--    video/pdf → object_key；article → object_key（自包含 html，可附 pdf_key）或 pages（图片序列）；
--    cards → pages；link → url。库里只存对象键不存 URL，媒体地址由后端按需签发预签名。
CREATE TABLE IF NOT EXISTS portal_training_lessons (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    course_id     BIGINT       NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    title         VARCHAR(200) NOT NULL,
    kind          VARCHAR(20)  NOT NULL,
    object_key    VARCHAR(500) NULL,
    pdf_key       VARCHAR(500) NULL,
    pages         JSON         NULL COMMENT '图片键数组（cards / 图片序列型 article）；原 PG JSONB',
    url           VARCHAR(500) NULL COMMENT '外链地址（仅 link 课时）',
    duration_sec  INT          NULL COMMENT '视频时长（秒），播放器首次心跳回报后写入',
    size_bytes    BIGINT       NULL,
    source_prefix VARCHAR(500) NULL COMMENT '导入幂等键（课时目录/文件键），手建课时为 NULL',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_training_lessons_source_prefix (source_prefix),
    KEY ix_training_lessons_course_sort (course_id, sort_order),
    CONSTRAINT fk_training_lessons_course FOREIGN KEY (course_id) REFERENCES portal_training (id) ON DELETE CASCADE,
    CONSTRAINT ck_training_lessons_kind CHECK (kind IN ('video', 'pdf', 'article', 'cards', 'link'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '培训课时：四种形态 + link，对象键不存 URL';

-- 3. 课时进度：员工×课时，percent 只增不减（应用层保证）；video ≥90 / 其余 ≥100 → completed。
--    UNIQUE(username, lesson_id) 幂等靠唯一索引，不靠先查后插。
CREATE TABLE IF NOT EXISTS portal_training_lesson_progress (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    lesson_id    BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'started',
    position     INT          NOT NULL DEFAULT 0 COMMENT '断点：视频秒 / PDF 页 / 卡片索引',
    percent      INT          NOT NULL DEFAULT 0,
    duration_sec INT          NULL COMMENT '播放器回报的媒体总时长',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_training_lesson_progress_user_lesson (username, lesson_id),
    CONSTRAINT fk_training_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES portal_training_lessons (id) ON DELETE CASCADE,
    CONSTRAINT ck_training_lesson_progress_status CHECK (status IN ('started', 'completed')),
    CONSTRAINT ck_training_lesson_progress_percent CHECK (percent >= 0 AND percent <= 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '员工×课时的学习进度：只增不减的 percent + 完成阈值';

-- 4. 课程级进度：退化为汇总（任一课时开始 → started；全部课时 completed → completed）。
--    真实进度在课时级表；这张表保留给 stats 面板与 legacy 的 start/complete 端点。
--    无 FK（课程删除后进度留存）、无唯一约束（Portal 原表如此，数据可能有重复）。
CREATE TABLE IF NOT EXISTS portal_training_progress (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    training_id  BIGINT       NOT NULL COMMENT '无 FK：Portal 语义如此，课程删除后进度留存',
    status       VARCHAR(20)  NOT NULL DEFAULT 'started',
    started_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME     NULL,
    PRIMARY KEY (id),
    KEY ix_training_progress_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '员工×课程级进度汇总（legacy，无唯一约束）';

-- ===== 历史来源：portal-events-products-schema.sql（本文件内冻结副本） =====
-- =====================================================================
-- B3 展会日历 / 新品动态：Portal Postgres → OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md 批次 3-2。
-- 与其他 db/*.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
--
-- 建表口径（契约 §二 末）：本项目自建表用 created_at/updated_at，不继承芋道 BaseDO。
-- 两表都无审批流：Portal 侧创建即 published，status 只有 published 一个取值（默认值），
-- 所以不加 CHECK 约束（Portal 的 models.py 里这两张表的 status 也只有 default，没有 CheckConstraint）。
-- =====================================================================

-- 1. 展会/活动日历：无作者列（Portal 的 events 不记创建人），写操作由 can_manage_events 整端点闸。
CREATE TABLE IF NOT EXISTS portal_events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '活动标题',
    description TEXT         NULL COMMENT '活动介绍',
    location    VARCHAR(200) NULL COMMENT '举办地点',
    start_date  DATE         NOT NULL COMMENT '开始日期（新建缺省今天）',
    end_date    DATE         NULL COMMENT '结束日期，可空',
    event_type  VARCHAR(50)  NOT NULL DEFAULT 'exhibition' COMMENT '活动类型',
    status      VARCHAR(20)  NOT NULL DEFAULT 'published' COMMENT '状态：只有 published',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_events_start_date (start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '展会/活动日历';

-- 2. 新品动态：无作者列（Portal 的 products 不记创建人），写操作由 can_manage_products 整端点闸。
CREATE TABLE IF NOT EXISTS portal_products (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(200) NOT NULL COMMENT '产品名称',
    brand       VARCHAR(50)  NULL COMMENT '品牌',
    category    VARCHAR(50)  NULL COMMENT '分类',
    description TEXT         NULL COMMENT '介绍',
    image_url   VARCHAR(500) NULL COMMENT '图片地址',
    link_url    VARCHAR(500) NULL COMMENT '详情链接',
    launch_date DATE         NULL COMMENT '发布日期，可空',
    status      VARCHAR(20)  NOT NULL DEFAULT 'published' COMMENT '状态：只有 published',
    is_featured TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '精选，前端兼作置顶展示',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_products_launch_date (launch_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '新品动态';

-- ===== 历史来源：portal-feedback-schema.sql（本文件内冻结副本） =====
-- =====================================================================
-- B4 员工反馈：Portal 的 bot.employee_feedback（员工提交 + 管理查看）搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §一「员工反馈」、§二「类型转换口径」、§四 批次 4。
--
-- 说明：Portal 原表在 bot schema（与企微机器人共表）；机器人 AI 富化（sentiment/ai_summary/
-- 状态流转/指派/解决）属 bot 模块（阻塞于 AI 基建），这里只迁「员工提交 + 管理查看」两件事。
-- AI 富化列（sentiment/ai_summary/assigned_to/resolved_note）保留为 NULL 占位，随 bot 落地后写入。
-- Postgres → MySQL：JSONB → JSON（image_urls）；Text → MEDIUMTEXT（raw_message）。
-- =====================================================================

CREATE TABLE IF NOT EXISTS portal_feedback (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL COMMENT '提交人（企微 userid = Portal 的 wecom_userid）',
    reporter_name VARCHAR(100) NOT NULL COMMENT '提交人姓名快照',
    department    VARCHAR(100) NULL COMMENT '主部门名快照（单值）',
    category      VARCHAR(50)  NOT NULL COMMENT '反馈分类（产品/内部/建议等，自由文本）',
    sub_category  VARCHAR(100) NULL,
    brand         VARCHAR(50)  NULL,
    product_name  VARCHAR(200) NULL,
    sentiment     VARCHAR(20)  NULL COMMENT 'AI 情感（bot 写入，迁移时保留占位）',
    priority      VARCHAR(20)  NOT NULL DEFAULT 'normal',
    raw_message   MEDIUMTEXT   NOT NULL COMMENT '员工原文（description）',
    image_urls    JSON         NULL COMMENT '图片地址数组；原 PG JSONB',
    ai_summary    TEXT         NULL COMMENT 'AI 摘要（bot 写入）',
    status        VARCHAR(30)  NOT NULL DEFAULT 'pending',
    assigned_to   VARCHAR(100) NULL COMMENT '指派处理人（bot/管理写入）',
    resolved_note TEXT         NULL COMMENT '解决说明',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_feedback_created (created_at),
    KEY ix_feedback_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '员工反馈：员工提交 + 管理查看；AI 富化随 bot 落地';

-- ===== 历史来源：portal-esports-schema.sql（本文件内冻结副本） =====
-- =====================================================================
-- B4 电竞室：Portal 的开放规则 / 预约单 / 审批快照 / 流水搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §三「电竞室」、§二「类型转换口径」、§四 批次 4。
-- 与其他 db/*.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
--
-- Postgres → MySQL 的等价改写：
--   1) JSONB → JSON（open_weekdays / approval_stages / participants）
--   2) boolean → TINYINT(1)
--   3) CHECK 约束保留（MySQL 8.0.16+ 生效），状态机取值不靠应用层自觉
--   4) 行 id 原样保留（迁移脚本 migrate_portal_esports.py 直接搬 id）
--   5) FK 只落在 portal_* 内部表之间（steps/logs → bookings ON DELETE CASCADE）；
--      approver_user_id / applicant_user_id 等 user_id 系列只存 sys_user.id 值，不建 FK
--      （对齐 signoff/training 口径：portal_* 表认 username/user_id 快照，不依赖 sys_user 外键）
-- =====================================================================

-- 1. 开放规则：单行表（id 恒 = 1，CHECK 锁单例）。
--    审批人只影响新单，历史预约持有审批人快照；旧单人列 approver_user_id 是第一环节第一人的投影。
CREATE TABLE IF NOT EXISTS portal_esports_settings (
    id                   BIGINT      NOT NULL,
    is_open              TINYINT(1)  NOT NULL DEFAULT 0,
    open_weekdays        JSON        NOT NULL COMMENT '开放星期（ISO 1-7），原 PG ARRAY(int)',
    open_start_time      TIME        NOT NULL DEFAULT '18:00:00',
    open_end_time        TIME        NOT NULL DEFAULT '22:00:00',
    advance_days         INT         NOT NULL DEFAULT 7,
    max_duration_minutes INT         NOT NULL DEFAULT 120,
    approver_user_id     BIGINT      NULL COMMENT '第一环节第一人投影 sys_user.id；新口径事实源是 approval_stages',
    approval_mode        VARCHAR(16) NOT NULL DEFAULT 'any',
    approval_stages      JSON        NOT NULL COMMENT '有序审批环节数组 [{approver_user_ids:[...]}]，原 PG JSONB',
    updated_by           VARCHAR(50) NULL,
    updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_esports_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_esports_settings_time CHECK (open_start_time < open_end_time),
    CONSTRAINT ck_esports_settings_advance CHECK (advance_days BETWEEN 0 AND 90),
    CONSTRAINT ck_esports_settings_duration CHECK (max_duration_minutes BETWEEN 1 AND 1440),
    CONSTRAINT ck_esports_settings_approval_mode CHECK (approval_mode IN ('any', 'sequential'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '电竞室开放规则（单行 id=1，多级审批由 approval_stages 驱动）';

-- 2. 预约单：整间独占，pending 和 approved 均占位。
--    participants 为 NULL 表示旧单只记人数、名单未知。
CREATE TABLE IF NOT EXISTS portal_esports_bookings (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    booking_date         DATE         NOT NULL,
    start_time           TIME         NOT NULL,
    end_time             TIME         NOT NULL,
    title                VARCHAR(120) NOT NULL,
    purpose              VARCHAR(1000) NULL,
    participant_count    INT          NOT NULL DEFAULT 1,
    participants         JSON         NULL COMMENT '参加人快照 [{user_id,display_name}]，原 PG JSONB；NULL=旧单无名单',
    applicant_user_id    BIGINT       NOT NULL COMMENT '申请人 sys_user.id',
    applicant_name       VARCHAR(100) NOT NULL,
    approver_user_id     BIGINT       NOT NULL COMMENT '第一环节第一人投影 sys_user.id',
    approver_name        VARCHAR(100) NOT NULL,
    approval_mode        VARCHAR(16)  NOT NULL DEFAULT 'any',
    current_stage_index  INT          NULL DEFAULT 0 COMMENT '0 起始；NULL=流程已结束',
    status               VARCHAR(16)  NOT NULL DEFAULT 'pending',
    reviewed_by_user_id  BIGINT       NULL,
    reviewed_by_name     VARCHAR(100) NULL,
    review_remark        VARCHAR(1000) NULL,
    reviewed_at          DATETIME     NULL,
    cancelled_by_user_id BIGINT       NULL,
    cancelled_at         DATETIME     NULL,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_esports_bookings_date_status (booking_date, status),
    KEY ix_esports_bookings_applicant_date (applicant_user_id, booking_date),
    KEY ix_esports_bookings_approver_date (approver_user_id, booking_date),
    CONSTRAINT ck_esports_bookings_time CHECK (start_time < end_time),
    CONSTRAINT ck_esports_bookings_participants CHECK (participant_count BETWEEN 1 AND 100),
    CONSTRAINT ck_esports_bookings_status CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled')),
    CONSTRAINT ck_esports_bookings_approval_mode CHECK (approval_mode IN ('any', 'sequential')),
    CONSTRAINT ck_esports_bookings_current_stage CHECK (current_stage_index IS NULL OR current_stage_index BETWEEN 0 AND 9)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '电竞室独占预约单（pending/approved 均占位）';

-- 3. 审批快照：每单每环节审批人；同环节任一通过即推进。
CREATE TABLE IF NOT EXISTS portal_esports_approval_steps (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    booking_id       BIGINT       NOT NULL,
    stage_index      INT          NOT NULL,
    approver_user_id BIGINT       NOT NULL COMMENT '指定审批人 sys_user.id',
    approver_name    VARCHAR(100) NOT NULL,
    status           VARCHAR(16)  NOT NULL DEFAULT 'waiting',
    acted_by_user_id BIGINT       NULL COMMENT '实际代审人 sys_user.id（同环节他人可代审）',
    acted_by_name    VARCHAR(100) NULL,
    acted_at         DATETIME     NULL,
    remark           VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_esports_approval_step_person (booking_id, stage_index, approver_user_id),
    KEY ix_esports_approval_steps_approver_status (approver_user_id, status, booking_id),
    CONSTRAINT fk_esports_approval_steps_booking FOREIGN KEY (booking_id) REFERENCES portal_esports_bookings (id) ON DELETE CASCADE,
    CONSTRAINT ck_esports_approval_steps_stage CHECK (stage_index BETWEEN 0 AND 9),
    CONSTRAINT ck_esports_approval_steps_status CHECK (status IN ('waiting', 'pending', 'approved', 'rejected', 'skipped'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '电竞预约每单每环节审批人快照';

-- 4. 流水：提交 / 审批 / 取消，与主单同事务追加，业务不提供改删入口。
CREATE TABLE IF NOT EXISTS portal_esports_booking_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    booking_id    BIGINT       NOT NULL,
    operation     VARCHAR(16)  NOT NULL,
    actor_user_id BIGINT       NOT NULL COMMENT '操作人 sys_user.id',
    actor_name    VARCHAR(100) NOT NULL,
    remark        VARCHAR(1000) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_esports_booking_logs_booking_created (booking_id, created_at, id),
    CONSTRAINT fk_esports_booking_logs_booking FOREIGN KEY (booking_id) REFERENCES portal_esports_bookings (id) ON DELETE CASCADE,
    CONSTRAINT ck_esports_booking_logs_operation CHECK (operation IN ('submit', 'approve', 'reject', 'cancel'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '电竞预约提交/审批/取消流水';

-- ===== 历史来源：portal-materials-schema.sql（本文件内冻结副本） =====
-- 迁自 CYT-Portal models.py；保留旧列和主键，历史关系不重新编号。

CREATE TABLE IF NOT EXISTS `portal_material_site_owners` (
  `site` VARCHAR(8) NOT NULL,
  `kind` VARCHAR(16) NOT NULL,
  `owner_username` VARCHAR(64) NOT NULL,
  `owner_name` VARCHAR(64) NOT NULL,
  `updated_by` VARCHAR(64) NULL,
  `updated_by_name` VARCHAR(64) NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`site`,`kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_products` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kind` VARCHAR(16) NOT NULL,
  `category_id` BIGINT NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `brand` VARCHAR(50) NULL,
  `description` LONGTEXT NULL,
  `image_urls` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `variant_dims` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `archived_at` DATETIME NULL,
  `archived_by` VARCHAR(64) NULL,
  `created_by` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_materials` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kind` VARCHAR(16) NOT NULL DEFAULT 'sample',
  `product_id` BIGINT NULL,
  `variant_values` JSON NOT NULL DEFAULT (JSON_OBJECT()),
  `site` VARCHAR(8) NOT NULL DEFAULT 'SZ',
  `category` VARCHAR(32) NOT NULL,
  `sku` VARCHAR(64) NULL,
  `name` VARCHAR(200) NOT NULL,
  `brand` VARCHAR(50) NULL,
  `spec` VARCHAR(64) NULL,
  `unit_price` DECIMAL(10,2) NULL,
  `image_urls` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `stock_qty` BIGINT NOT NULL DEFAULT '1',
  `location` VARCHAR(64) NOT NULL DEFAULT '陈列室',
  `current_borrowers` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `owner_username` VARCHAR(64) NOT NULL,
  `owner_name` VARCHAR(64) NOT NULL,
  `note` LONGTEXT NULL,
  `archived_at` DATETIME NULL,
  `archived_by` VARCHAR(64) NULL,
  `created_by` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_material_product(product_id),
  KEY idx_material_kind_site(kind,site),
  CHECK(stock_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_applies` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_no` VARCHAR(32) NOT NULL,
  `apply_type` VARCHAR(16) NOT NULL DEFAULT 'sample_borrow',
  `applicant_username` VARCHAR(64) NOT NULL,
  `applicant_name` VARCHAR(64) NOT NULL,
  `department` VARCHAR(64) NULL,
  `purpose` VARCHAR(200) NOT NULL DEFAULT '',
  `expect_return_date` DATE NULL,
  `actual_return_date` DATE NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending',
  `remark` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_material_apply_no(apply_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_apply_items` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `material_name` VARCHAR(200) NOT NULL,
  `material_sku` VARCHAR(64) NOT NULL DEFAULT '',
  `spec` VARCHAR(64) NULL,
  `quantity` BIGINT NOT NULL,
  `approved_qty` BIGINT NULL,
  `returned_quantity` BIGINT NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY idx_material_apply(apply_id),
  KEY idx_material_reference(material_id),
  CHECK(quantity > 0),
  CHECK(approved_qty IS NULL OR (approved_qty>=0 AND approved_qty<=quantity)),
  CHECK(returned_quantity>=0 AND returned_quantity<=COALESCE(approved_qty,quantity))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_apply_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_id` BIGINT NOT NULL,
  `operation` VARCHAR(32) NOT NULL,
  `operator_username` VARCHAR(64) NOT NULL,
  `operator_name` VARCHAR(64) NOT NULL,
  `before_value` LONGTEXT NULL,
  `after_value` LONGTEXT NULL,
  `remark` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 历史来源：portal-management-schema.sql（本文件内冻结副本） =====
-- 门户管理剩余域；新建表可重复执行。附件单独迁入 PORTAL_DOCUMENT_ROOT 持久卷。
CREATE TABLE IF NOT EXISTS portal_content_push_jobs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, push_trigger VARCHAR(32) NOT NULL, target_key VARCHAR(191) NOT NULL,
 content TEXT NOT NULL, recipients JSON NOT NULL, pushed_by VARCHAR(64) NOT NULL, force_send TINYINT(1) NOT NULL DEFAULT 0,
 target_count INT NOT NULL, sent_count INT NOT NULL DEFAULT 0, failed_count INT NOT NULL DEFAULT 0,
 status VARCHAR(20) NOT NULL DEFAULT 'queued', error_msg VARCHAR(500) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, finished_at DATETIME NULL,
 KEY ix_content_push_queue(status,id), KEY ix_content_push_target(push_trigger,target_key,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_content_push_guard (
 push_trigger VARCHAR(32) NOT NULL, target_key VARCHAR(191) NOT NULL, last_success_at DATETIME NULL,
 PRIMARY KEY(push_trigger,target_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_signoff_notify_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, actor_id BIGINT NULL, document_id BIGINT NULL,
 target_count INT NOT NULL, sent_count INT NOT NULL, failed_count INT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_brand_docs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, brand_code VARCHAR(32) NOT NULL, doc_type VARCHAR(32) NOT NULL,
 title VARCHAR(120) NOT NULL, file_url TEXT NOT NULL, file_size BIGINT NOT NULL DEFAULT 0,
 status VARCHAR(10) NOT NULL DEFAULT 'published', updated_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_brand_docs_brand_type(brand_code,doc_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_brand_doc_versions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, doc_id BIGINT NOT NULL, file_url TEXT NOT NULL,
 file_size BIGINT NOT NULL DEFAULT 0, uploaded_by VARCHAR(64) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_brand_doc_versions_doc(doc_id), FOREIGN KEY(doc_id) REFERENCES portal_brand_docs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_contract_entities (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, address VARCHAR(200) NOT NULL,
 legal_rep VARCHAR(50) NOT NULL, is_active TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_contract_entities_name(name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_settings (
 id BIGINT NOT NULL PRIMARY KEY, enabled TINYINT(1) NOT NULL DEFAULT 0, send_hour INT NOT NULL DEFAULT 9,
 send_minute INT NOT NULL DEFAULT 0, birthday_enabled TINYINT(1) NOT NULL DEFAULT 1, birthday_card_enabled TINYINT(1) NOT NULL DEFAULT 0,
 templates JSON NULL, updated_by VARCHAR(64) NULL, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO portal_celebration_settings(id) VALUES(1);
CREATE TABLE IF NOT EXISTS portal_celebration_preferences (
 user_id BIGINT NOT NULL PRIMARY KEY, opt_out TINYINT(1) NOT NULL DEFAULT 0,
 birthday_override TINYINT(1) NOT NULL DEFAULT 0, birthday_month_day VARCHAR(5) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_resume_interview_processes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, resume_id BIGINT NOT NULL, position VARCHAR(120) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'active', result VARCHAR(16) NULL, summary TEXT NULL,
 created_by VARCHAR(64) NOT NULL, updated_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 finished_at DATETIME NULL,
 active_resume_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='active' THEN resume_id ELSE NULL END) STORED,
 UNIQUE KEY uq_resume_interview_active(active_resume_id), KEY ix_resume_interview_resume_created(resume_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_resume_interview_rounds (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, process_id BIGINT NOT NULL, sequence INT NOT NULL,
 name VARCHAR(100) NOT NULL, interviewer VARCHAR(100) NULL, scheduled_at DATETIME NULL, location VARCHAR(500) NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'pending', result VARCHAR(16) NULL, feedback TEXT NULL, updated_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_resume_interview_round_sequence(process_id,sequence), FOREIGN KEY(process_id) REFERENCES portal_resume_interview_processes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_runs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, run_date DATE NOT NULL, kind VARCHAR(20) NOT NULL,
 status VARCHAR(10) NOT NULL, detail TEXT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uq_celebration_runs_date_kind(run_date,kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_push_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NULL, kind VARCHAR(20) NOT NULL,
 title VARCHAR(100) NOT NULL, sent_ok TINYINT(1) NOT NULL DEFAULT 0, error_msg VARCHAR(500) NULL,
 source_log_id BIGINT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uq_celebration_source_log(source_log_id), KEY ix_celebration_log_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 原推送日志完整保留（不仅庆祝模块），便于审计与迁移对账。
CREATE TABLE IF NOT EXISTS portal_wecom_push_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `trigger` VARCHAR(32) NOT NULL, target_key VARCHAR(128) NOT NULL,
 content TEXT NOT NULL, recipient_count INT NOT NULL DEFAULT 0, failed_count INT NOT NULL DEFAULT 0,
 pushed_by VARCHAR(64) NOT NULL, pushed_at DATETIME NOT NULL, error_msg TEXT NULL,
 KEY ix_wecom_push_trigger(`trigger`,target_key,pushed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_cards (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, token VARCHAR(32) NOT NULL, kind VARCHAR(20) NOT NULL,
 recipient_username VARCHAR(64) NULL, recipient_name VARCHAR(100) NOT NULL, title VARCHAR(100) NOT NULL, message VARCHAR(500) NOT NULL,
 years INT NULL, format VARCHAR(10) NOT NULL DEFAULT 'html', image_key VARCHAR(255) NULL, created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_ok TINYINT(1) NOT NULL DEFAULT 0, error_msg VARCHAR(500) NULL, sent_at DATETIME NULL,
 UNIQUE KEY uq_celebration_card_token(token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_resumes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, status VARCHAR(16) NOT NULL, source_filename VARCHAR(255) NOT NULL,
 file_ext VARCHAR(10) NOT NULL, file_size BIGINT NOT NULL, file_sha256 VARCHAR(64) NOT NULL, storage_key VARCHAR(255) NULL,
 name VARCHAR(100) NULL, gender VARCHAR(4) NULL, birth_date DATE NULL, phone VARCHAR(32) NULL, email VARCHAR(120) NULL,
 current_city VARCHAR(50) NULL, highest_degree VARCHAR(20) NULL, years_of_experience DECIMAL(4,1) NULL,
 current_company VARCHAR(120) NULL, current_title VARCHAR(120) NULL, expected_position VARCHAR(120) NULL, expected_salary VARCHAR(60) NULL,
 summary TEXT NULL, extracted JSON NOT NULL, ai_raw JSON NULL, ai_model VARCHAR(64) NULL,
 ai_tokens_in BIGINT NULL, ai_tokens_out BIGINT NULL, ai_request_id VARCHAR(64) NULL, error_msg TEXT NULL,
 duplicate_of_id BIGINT NULL, uploaded_by VARCHAR(64) NOT NULL, confirmed_by VARCHAR(64) NULL, confirmed_at DATETIME NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY ix_resumes_status_created(status,created_at), KEY ix_resumes_phone(phone), KEY ix_resumes_email(email), KEY ix_resumes_sha256(file_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 历史来源：portal-entry-rollout-schema.sql（本文件内冻结副本） =====
-- 管理入口分批上线；未配置的入口沿用开启状态。重跑不覆盖已保存的开关。
CREATE TABLE IF NOT EXISTS portal_entry_rollout (
 id TINYINT UNSIGNED NOT NULL PRIMARY KEY,
 entries JSON NOT NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 updated_by VARCHAR(100) NULL,
 updated_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户管理入口上线设置';
INSERT IGNORE INTO portal_entry_rollout(id,entries,revision) VALUES(1,JSON_OBJECT(),0);

-- ===== 历史来源：portal-management-upgrade.sql（本文件内冻结副本） =====
-- 仅适用于已经执行过早期 portal-management-schema.sql 的库；可重复执行。
-- 全新库直接执行当前 schema 即可，不需要本升级。
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_celebration_preferences ADD COLUMN birthday_override TINYINT(1) NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_celebration_preferences' AND column_name='birthday_override');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_celebration_push_log ADD COLUMN source_log_id BIGINT NULL, ADD UNIQUE KEY uq_celebration_source_log(source_log_id)','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_celebration_push_log' AND column_name='source_log_id');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_resumes ADD COLUMN ai_tokens_in BIGINT NULL, ADD COLUMN ai_tokens_out BIGINT NULL, ADD COLUMN ai_request_id VARCHAR(64) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_resumes' AND column_name='ai_tokens_in');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_celebration_preferences ADD COLUMN birthday_month_day VARCHAR(5) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_celebration_preferences' AND column_name='birthday_month_day');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;

-- ===== 历史来源：portal-materials-integrity.sql（本文件内冻结副本） =====
-- 幂等升级：归档批次防止独立下架规格误恢复；唯一约束和外键兜住并发与历史引用。
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_products ADD COLUMN archive_group CHAR(36) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_material_products' AND column_name='archive_group');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_materials ADD COLUMN archive_group CHAR(36) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_materials' AND column_name='archive_group');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_products ADD COLUMN active_name VARCHAR(200) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL THEN LOWER(TRIM(name)) ELSE NULL END) STORED','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_material_products' AND column_name='active_name');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_products ADD COLUMN active_brand VARCHAR(50) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL THEN LOWER(TRIM(COALESCE(brand,''''))) ELSE NULL END) STORED','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_material_products' AND column_name='active_brand');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_products ADD UNIQUE KEY uq_material_product_name(kind,active_name,active_brand)','SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='portal_material_products' AND index_name='uq_material_product_name');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_products ADD UNIQUE KEY uq_material_product_kind(id,kind)','SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='portal_material_products' AND index_name='uq_material_product_kind');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_materials ADD COLUMN active_variant CHAR(64) GENERATED ALWAYS AS (CASE WHEN archived_at IS NULL AND JSON_LENGTH(variant_values)>0 THEN SHA2(CAST(variant_values AS CHAR),256) ELSE NULL END) STORED','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_materials' AND column_name='active_variant');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_materials ADD UNIQUE KEY uq_material_variant(product_id,site,active_variant)','SELECT 1') FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='portal_materials' AND index_name='uq_material_variant');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_products ADD CONSTRAINT fk_oa_material_category FOREIGN KEY(category_id) REFERENCES portal_categories(id) ON DELETE RESTRICT','SELECT 1') FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='portal_material_products' AND constraint_name='fk_oa_material_category');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_materials ADD CONSTRAINT fk_oa_material_product FOREIGN KEY(product_id,kind) REFERENCES portal_material_products(id,kind) ON DELETE RESTRICT','SELECT 1') FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='portal_materials' AND constraint_name='fk_oa_material_product');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_materials ADD CONSTRAINT fk_oa_material_owner FOREIGN KEY(site,kind) REFERENCES portal_material_site_owners(site,kind) ON DELETE RESTRICT','SELECT 1') FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='portal_materials' AND constraint_name='fk_oa_material_owner');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_apply_items ADD CONSTRAINT fk_oa_material_apply FOREIGN KEY(apply_id) REFERENCES portal_material_applies(id) ON DELETE CASCADE','SELECT 1') FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='portal_material_apply_items' AND constraint_name='fk_oa_material_apply');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_apply_items ADD CONSTRAINT fk_oa_material_item FOREIGN KEY(material_id) REFERENCES portal_materials(id) ON DELETE RESTRICT','SELECT 1') FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='portal_material_apply_items' AND constraint_name='fk_oa_material_item');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_material_apply_logs ADD CONSTRAINT fk_oa_material_log FOREIGN KEY(apply_id) REFERENCES portal_material_applies(id) ON DELETE CASCADE','SELECT 1') FROM information_schema.table_constraints WHERE table_schema=DATABASE() AND table_name='portal_material_apply_logs' AND constraint_name='fk_oa_material_log');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
