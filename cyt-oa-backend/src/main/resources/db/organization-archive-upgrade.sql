-- 已有数据库的增量升级；MySQL 8。重复执行安全，有冲突时停止，禁止自动合并员工历史。
DROP PROCEDURE IF EXISTS upgrade_organization_archive;
DELIMITER $$
CREATE PROCEDURE upgrade_organization_archive()
BEGIN
    IF EXISTS (SELECT user_id FROM hrm_employee WHERE deleted = 0 AND user_id IS NOT NULL
               GROUP BY user_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate active employee links; resolve manually before upgrade';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'hrm_employee' AND column_name = 'active_user_id') THEN
        ALTER TABLE hrm_employee ADD COLUMN active_user_id BIGINT
            GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN user_id ELSE NULL END) STORED;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
                   AND table_name = 'hrm_employee' AND index_name = 'uk_hrm_employee_active_user') THEN
        ALTER TABLE hrm_employee ADD UNIQUE KEY uk_hrm_employee_active_user (active_user_id);
    END IF;
END$$
DELIMITER ;
CALL upgrade_organization_archive();
DROP PROCEDURE upgrade_organization_archive;
ALTER TABLE sys_dept MODIFY COLUMN sort_order BIGINT NOT NULL DEFAULT 0;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status)
VALUES ('hrm_employee_entry_status', '待完善', '0', 0, 1)
ON DUPLICATE KEY UPDATE label = VALUES(label);
