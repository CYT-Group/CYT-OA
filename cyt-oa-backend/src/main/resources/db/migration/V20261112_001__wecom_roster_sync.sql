-- 花名册同步分阶段运行，页面刷新和服务重启后均可查看结果。
CREATE TABLE IF NOT EXISTS hrm_wecom_roster_sync_run (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    status VARCHAR(16) NOT NULL,
    stage VARCHAR(16) NOT NULL,
    actor VARCHAR(160) NOT NULL,
    message VARCHAR(500) NOT NULL DEFAULT '',
    directory_json MEDIUMTEXT NULL,
    result_json MEDIUMTEXT NULL,
    active_slot TINYINT NULL,
    created_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    UNIQUE KEY uk_roster_sync_active (active_slot),
    KEY idx_roster_sync_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微通讯录与人事花名册同步记录';
