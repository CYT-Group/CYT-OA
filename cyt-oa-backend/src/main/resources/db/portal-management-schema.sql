-- 门户管理剩余域；新建表可重复执行。附件单独迁入 PORTAL_DOCUMENT_ROOT 持久卷。
CREATE TABLE IF NOT EXISTS portal_content_push_jobs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, push_trigger VARCHAR(32) NOT NULL, target_key VARCHAR(191) NOT NULL,
 content TEXT NOT NULL, recipients JSON NOT NULL, pushed_by VARCHAR(64) NOT NULL, force_send TINYINT(1) NOT NULL DEFAULT 0,
 target_count INT NOT NULL, sent_count INT NOT NULL DEFAULT 0, failed_count INT NOT NULL DEFAULT 0,
 status VARCHAR(20) NOT NULL DEFAULT 'queued', error_msg VARCHAR(500) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, finished_at DATETIME NULL,
 KEY ix_content_push_queue(status,id), KEY ix_content_push_target(push_trigger,target_key,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_content_push_guard (
 push_trigger VARCHAR(32) NOT NULL, target_key VARCHAR(191) NOT NULL, last_success_at DATETIME NULL,
 PRIMARY KEY(push_trigger,target_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_signoff_notify_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, actor_id BIGINT NULL, document_id BIGINT NULL,
 target_count INT NOT NULL, sent_count INT NOT NULL, failed_count INT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_brand_docs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, brand_code VARCHAR(32) NOT NULL, doc_type VARCHAR(32) NOT NULL,
 title VARCHAR(120) NOT NULL, file_url TEXT NOT NULL, file_size BIGINT NOT NULL DEFAULT 0,
 status VARCHAR(10) NOT NULL DEFAULT 'published', updated_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_brand_docs_brand_type(brand_code,doc_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_brand_doc_versions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, doc_id BIGINT NOT NULL, file_url TEXT NOT NULL,
 file_size BIGINT NOT NULL DEFAULT 0, uploaded_by VARCHAR(64) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_brand_doc_versions_doc(doc_id), FOREIGN KEY(doc_id) REFERENCES portal_brand_docs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_contract_entities (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, address VARCHAR(200) NOT NULL,
 legal_rep VARCHAR(50) NOT NULL, is_active TINYINT(1) NOT NULL DEFAULT 1, created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_contract_entities_name(name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_settings (
 id BIGINT NOT NULL PRIMARY KEY, enabled TINYINT(1) NOT NULL DEFAULT 0, send_hour INT NOT NULL DEFAULT 9,
 send_minute INT NOT NULL DEFAULT 0, birthday_enabled TINYINT(1) NOT NULL DEFAULT 1, birthday_card_enabled TINYINT(1) NOT NULL DEFAULT 0,
 templates JSON NULL, updated_by VARCHAR(64) NULL, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO portal_celebration_settings(id) VALUES(1);
CREATE TABLE IF NOT EXISTS portal_celebration_preferences (
 user_id BIGINT NOT NULL PRIMARY KEY, opt_out TINYINT(1) NOT NULL DEFAULT 0,
 birthday_override TINYINT(1) NOT NULL DEFAULT 0, birthday_month_day VARCHAR(5) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_resume_interview_processes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, resume_id BIGINT NOT NULL, position VARCHAR(120) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'active', result VARCHAR(16) NULL, summary TEXT NULL,
 created_by VARCHAR(64) NOT NULL, updated_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 finished_at DATETIME NULL,
 active_resume_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='active' THEN resume_id ELSE NULL END) STORED,
 UNIQUE KEY uq_resume_interview_active(active_resume_id), KEY ix_resume_interview_resume_created(resume_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_resume_interview_rounds (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, process_id BIGINT NOT NULL, sequence INT NOT NULL,
 name VARCHAR(100) NOT NULL, interviewer VARCHAR(100) NULL, scheduled_at DATETIME NULL, location VARCHAR(500) NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'pending', result VARCHAR(16) NULL, feedback TEXT NULL, updated_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_resume_interview_round_sequence(process_id,sequence), FOREIGN KEY(process_id) REFERENCES portal_resume_interview_processes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_runs (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, run_date DATE NOT NULL, kind VARCHAR(20) NOT NULL,
 status VARCHAR(10) NOT NULL, detail TEXT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uq_celebration_runs_date_kind(run_date,kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_push_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NULL, kind VARCHAR(20) NOT NULL,
 title VARCHAR(100) NOT NULL, sent_ok TINYINT(1) NOT NULL DEFAULT 0, error_msg VARCHAR(500) NULL,
 source_log_id BIGINT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uq_celebration_source_log(source_log_id), KEY ix_celebration_log_created(created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 原推送日志完整保留（不仅庆祝模块），便于审计与迁移对账。
CREATE TABLE IF NOT EXISTS portal_wecom_push_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `trigger` VARCHAR(32) NOT NULL, target_key VARCHAR(128) NOT NULL,
 content TEXT NOT NULL, recipient_count INT NOT NULL DEFAULT 0, failed_count INT NOT NULL DEFAULT 0,
 pushed_by VARCHAR(64) NOT NULL, pushed_at DATETIME NOT NULL, error_msg TEXT NULL,
 KEY ix_wecom_push_trigger(`trigger`,target_key,pushed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_celebration_cards (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, token VARCHAR(32) NOT NULL, kind VARCHAR(20) NOT NULL,
 recipient_username VARCHAR(64) NULL, recipient_name VARCHAR(100) NOT NULL, title VARCHAR(100) NOT NULL, message VARCHAR(500) NOT NULL,
 years INT NULL, format VARCHAR(10) NOT NULL DEFAULT 'html', image_key VARCHAR(255) NULL, created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, sent_ok TINYINT(1) NOT NULL DEFAULT 0, error_msg VARCHAR(500) NULL, sent_at DATETIME NULL,
 UNIQUE KEY uq_celebration_card_token(token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS portal_resumes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, status VARCHAR(16) NOT NULL, source_filename VARCHAR(255) NOT NULL,
 file_ext VARCHAR(10) NOT NULL, file_size BIGINT NOT NULL, file_sha256 VARCHAR(64) NOT NULL, storage_key VARCHAR(255) NULL,
 name VARCHAR(100) NULL, gender VARCHAR(4) NULL, birth_date DATE NULL, phone VARCHAR(32) NULL, email VARCHAR(120) NULL,
 current_city VARCHAR(50) NULL, highest_degree VARCHAR(20) NULL, years_of_experience DECIMAL(4,1) NULL,
 current_company VARCHAR(120) NULL, current_title VARCHAR(120) NULL, expected_position VARCHAR(120) NULL, expected_salary VARCHAR(60) NULL,
 summary TEXT NULL, extracted JSON NOT NULL, ai_raw JSON NULL, ai_model VARCHAR(64) NULL,
 ai_tokens_in BIGINT NULL, ai_tokens_out BIGINT NULL, ai_request_id VARCHAR(64) NULL, error_msg TEXT NULL,
 duplicate_of_id BIGINT NULL, uploaded_by VARCHAR(64) NOT NULL, confirmed_by VARCHAR(64) NULL, confirmed_at DATETIME NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY ix_resumes_status_created(status,created_at), KEY ix_resumes_phone(phone), KEY ix_resumes_email(email), KEY ix_resumes_sha256(file_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
