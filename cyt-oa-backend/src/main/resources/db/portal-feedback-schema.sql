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
