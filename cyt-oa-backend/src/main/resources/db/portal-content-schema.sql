-- =====================================================================
-- B2 内容域：Portal 的分类树 / 公司动态 / 知识库 / 页面埋点搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §一「内容管理」、§二「类型转换口径」、§四 批次 3。
-- 与其他 db/*.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
--
-- 建表口径（契约 §二 末）：本项目自建表用 created_at/updated_at，不继承芋道 BaseDO。
-- Postgres → MySQL 的三处等价改写：
--   1) ARRAY(String) / JSONB → JSON（blocks / attrs / tags / pending_notify_dept_scope）
--   2) PG 的部分唯一索引 archived_at IS NULL → 生成列 active_name + 普通唯一索引
--      （MySQL 唯一索引天然忽略 NULL 值，归档行的 active_name 是 NULL 所以不占名）
--   3) CHECK 约束保留（MySQL 8.0.16+ 生效），状态机取值不靠应用层自觉
-- =====================================================================

-- 1. 通用分类树：一张表服务多个业务，靠 scope 认归属（scope 刻意无 DB CHECK，
--    合法值只由 module/portal/domain/PortalCategoryScopes 注册表判——加业务 = 加一行注册表，
--    不走迁移）。被 B4 培训、B5 物资的 category_id 引用，所以它必须先建。
CREATE TABLE IF NOT EXISTS portal_categories (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    scope       VARCHAR(32) NOT NULL COMMENT '业务归属，点号分层：material.sample / material.office / training.course',
    name        VARCHAR(100) NOT NULL COMMENT '展示名；物资侧另由注册表限 32（请求体冻结面）',
    code        VARCHAR(32)  NULL COMMENT '程序锚点（路由 slug / seed 幂等键），普通分类留空',
    parent_id   BIGINT       NULL,
    depth       SMALLINT    NOT NULL DEFAULT 1 COMMENT '1 = 顶层（物资品类、培训大类），2 = 子级（培训小类）',
    sort_order  INT         NOT NULL DEFAULT 0 COMMENT '同层展示序，再按 name',
    attrs       JSON        NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '各业务自己的开关与展示字段，键名按 scope 白名单校验',
    archived_at DATETIME     NULL COMMENT 'NULL = 在役；时间戳本身即审计信息，不用 boolean',
    archived_by VARCHAR(64)  NULL,
    created_by  VARCHAR(64)  NOT NULL COMMENT '创建人 username；迁移 seed 行写 migration',
    active_name VARCHAR(100) GENERATED ALWAYS AS (IF(archived_at IS NULL, name, NULL)) STORED
        COMMENT '在役行才非 NULL，给下面的唯一索引当锚：等价于 PG 的部分唯一索引',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_scope_name_active (scope, active_name) COMMENT '同 scope 在役不重名，归档行不占名',
    UNIQUE KEY uq_categories_scope_code (scope, code) COMMENT 'code 含归档行一起唯一（NULL 不碰撞，等价 PG 的 WHERE code IS NOT NULL）',
    KEY ix_categories_scope_archived_sort (scope, archived_at, sort_order),
    KEY ix_categories_parent (parent_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES portal_categories (id) ON DELETE RESTRICT,
    CONSTRAINT ck_categories_depth_parent CHECK (parent_id IS NULL OR depth > 1)
    -- Portal 的 ck_categories_not_self_parent（parent_id <> id）在 MySQL 建不了：
    -- CHECK 不许引用自增列（3818）。同一件事由服务层 validateReparent 挡（"不能把自己设为父分类"）。
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '全门户共用分类字典：scope + depth + attrs + 归档';

-- 2. 公司动态：带 draft→pending→published→rejected 审批状态机的内容，
--    与 OA 系统公告 oa_announcement（type/priority/top，无状态机）是两套东西，不合并（契约 §一）。
--    身份列保留 username（企微 userid）原值：Portal 的作者/点赞人/评论人都是按名认人，
--    账号注销后历史行仍要能显示是谁写的，改成 sys_user.id 外键反而丢数据。
CREATE TABLE IF NOT EXISTS portal_news (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL,
    summary         TEXT         NULL,
    content         MEDIUMTEXT   NOT NULL COMMENT '富文本正文（Portal 侧是 Text，给到 MEDIUMTEXT 防长稿截断）',
    category        VARCHAR(50)  NULL COMMENT '文本分类，与 portal_categories 无外键：动态的分类口径独立于物资/培训',
    cover_image     VARCHAR(255) NULL,
    template_type   VARCHAR(32)  NOT NULL DEFAULT 'simple' COMMENT 'simple / 模板化稿型，模板特有字段在 blocks',
    blocks          JSON         NULL COMMENT '模板数据块（含 dept 等前端要读的字段）',
    author          VARCHAR(100) NULL COMMENT '作者展示名快照',
    author_username VARCHAR(50)  NULL COMMENT '作者 username（= 企微 userid）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',
    is_featured     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '精选，前端兼作置顶展示',
    published_at    DATETIME     NULL,
    pending_notify_wecom      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '作者提交的企微推送意图，审批通过时由审核人二次确认并一次性消费',
    pending_notify_dept_scope JSON       NULL COMMENT '推送部门范围，NULL = 全员',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_news_status_published (status, published_at),
    KEY ix_news_author (author_username),
    CONSTRAINT ck_news_status CHECK (status IN ('draft', 'pending', 'published', 'rejected'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '公司动态：审批状态机 + 模板 blocks + 企微推送意图';

-- 3. 动态点赞：幂等靠唯一索引，不靠先查后插（并发下先查后插会重复计数）
CREATE TABLE IF NOT EXISTS portal_news_like (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    news_id      BIGINT       NOT NULL,
    username     VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_news_likes_news_user (news_id, username),
    KEY ix_news_likes_news_id (news_id),
    CONSTRAINT fk_news_like_news FOREIGN KEY (news_id) REFERENCES portal_news (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '动态点赞：每人每动态最多一次';

-- 4. 动态评论：楼中楼用 parent_id 表达，服务端返回平铺数组（前端按 parent_id 组装）
CREATE TABLE IF NOT EXISTS portal_news_comment (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    news_id      BIGINT       NOT NULL,
    username     VARCHAR(50)  NOT NULL,
    display_name VARCHAR(100) NULL,
    avatar_url   VARCHAR(255) NULL,
    content      TEXT         NOT NULL COMMENT '软删除后写占位文案 [该评论已删除]，列复用不另开 flag 文案',
    parent_id    BIGINT       NULL,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_news_comments_news_id (news_id),
    KEY ix_news_comments_created_at (created_at),
    KEY ix_news_comments_parent (parent_id),
    CONSTRAINT fk_news_comment_news FOREIGN KEY (news_id) REFERENCES portal_news (id) ON DELETE CASCADE,
    CONSTRAINT fk_news_comment_parent FOREIGN KEY (parent_id) REFERENCES portal_news_comment (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '动态评论（含楼中楼），删除是软删';

-- 5. 知识库文档：状态机与动态一致，但没有 published_at / 推送意图列；详情自增 view_count
CREATE TABLE IF NOT EXISTS portal_wiki (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL,
    content         MEDIUMTEXT   NULL,
    category        VARCHAR(50)  NULL,
    department      VARCHAR(50)  NULL COMMENT '单值归属部门（可见范围口径），不是 portal_categories 的行',
    doc_url         VARCHAR(500) NULL COMMENT '外链文档地址，正文可为空只给链接',
    tags            JSON         NULL COMMENT '标签数组（原 PG ARRAY(String)），接口层进出都是数组',
    author_username VARCHAR(50)  NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',
    view_count      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_wiki_status (status),
    KEY ix_wiki_author (author_username),
    CONSTRAINT ck_wiki_status CHECK (status IN ('draft', 'pending', 'published', 'rejected'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '知识库文档';

-- 6. 页面埋点：写接口跟着搬到 OA（Portal 展示页继续打点），7140 行历史一起迁。
--    只存 username/display_name/department 三个快照列，不建外键：账号变动后历史统计不能变形。
CREATE TABLE IF NOT EXISTS portal_page_views (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    page_url     VARCHAR(500) NOT NULL,
    page_title   VARCHAR(300) NULL,
    username     VARCHAR(50)  NULL COMMENT 'NULL = 匿名访问',
    display_name VARCHAR(100) NULL COMMENT '打点时的名字快照',
    department   VARCHAR(50)  NULL COMMENT '打点时的主部门名快照',
    referrer     VARCHAR(500) NULL,
    user_agent   VARCHAR(500) NULL,
    viewed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_page_views_page_url (page_url),
    KEY ix_page_views_username (username),
    KEY ix_page_views_viewed_at (viewed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '页面浏览记录：谁看了什么、哪些页面热门';
