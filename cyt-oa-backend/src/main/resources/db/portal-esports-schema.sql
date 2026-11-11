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
