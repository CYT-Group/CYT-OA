-- 仅适用于已经执行过早期 portal-management-schema.sql 的库；可重复执行。
-- 全新库直接执行当前 schema 即可，不需要本升级。
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_celebration_preferences ADD COLUMN birthday_override TINYINT(1) NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_celebration_preferences' AND column_name='birthday_override');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_celebration_push_log ADD COLUMN source_log_id BIGINT NULL, ADD UNIQUE KEY uq_celebration_source_log(source_log_id)','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_celebration_push_log' AND column_name='source_log_id');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_resumes ADD COLUMN ai_tokens_in BIGINT NULL, ADD COLUMN ai_tokens_out BIGINT NULL, ADD COLUMN ai_request_id VARCHAR(64) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_resumes' AND column_name='ai_tokens_in');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
SET @portal_ddl=(SELECT IF(COUNT(*)=0,'ALTER TABLE portal_celebration_preferences ADD COLUMN birthday_month_day VARCHAR(5) NULL','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='portal_celebration_preferences' AND column_name='birthday_month_day');
PREPARE portal_stmt FROM @portal_ddl;
EXECUTE portal_stmt;
DEALLOCATE PREPARE portal_stmt;
