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
