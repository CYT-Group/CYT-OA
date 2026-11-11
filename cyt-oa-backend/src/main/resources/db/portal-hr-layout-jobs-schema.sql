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
