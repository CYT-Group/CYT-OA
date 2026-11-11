-- 管理入口分批上线；未配置的入口沿用开启状态。重跑不覆盖已保存的开关。
CREATE TABLE IF NOT EXISTS portal_entry_rollout (
 id TINYINT UNSIGNED NOT NULL PRIMARY KEY,
 entries JSON NOT NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 updated_by VARCHAR(100) NULL,
 updated_at DATETIME(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户管理入口上线设置';
INSERT IGNORE INTO portal_entry_rollout(id,entries,revision) VALUES(1,JSON_OBJECT(),0);
