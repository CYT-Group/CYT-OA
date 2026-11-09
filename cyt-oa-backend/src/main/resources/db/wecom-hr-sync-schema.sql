-- 存储每个字段最近成功采用的企微值，用于区分上游变化和 HR 本地修改。
CREATE TABLE IF NOT EXISTS hrm_employee_wecom_sync (
    employee_id BIGINT NOT NULL PRIMARY KEY,
    birthday DATETIME NULL,
    entry_time DATETIME NULL,
    synced_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微花名册日期同步基线';
