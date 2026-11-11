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
