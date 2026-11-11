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
