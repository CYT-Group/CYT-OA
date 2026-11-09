-- 通讯录同步改成异步任务后的增量升级；MySQL 8。重复执行安全。
-- 运行中的那次同步没有结果，所以 ok 允许 NULL（NULL=运行中），耗时同理，并补一个结束时间。
DROP PROCEDURE IF EXISTS upgrade_directory_sync_run;
DELIMITER $$
CREATE PROCEDURE upgrade_directory_sync_run()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'sys_directory_sync_log' AND column_name = 'ok'
               AND is_nullable = 'NO') THEN
        ALTER TABLE sys_directory_sync_log MODIFY COLUMN ok TINYINT(1) NULL DEFAULT NULL
            COMMENT 'NULL=运行中，1=成功，0=失败';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
               AND table_name = 'sys_directory_sync_log' AND column_name = 'duration_ms'
               AND is_nullable = 'NO') THEN
        ALTER TABLE sys_directory_sync_log MODIFY COLUMN duration_ms BIGINT NULL
            COMMENT '运行中还没有耗时，不能拿 0 冒充';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'sys_directory_sync_log' AND column_name = 'finished_at') THEN
        ALTER TABLE sys_directory_sync_log ADD COLUMN finished_at DATETIME NULL AFTER created_at;
    END IF;
END$$
DELIMITER ;
CALL upgrade_directory_sync_run();
DROP PROCEDURE upgrade_directory_sync_run;
