-- Built-in account, roles, dictionaries and permissions only; no imported or demo business data.

SET TIME ZONE 'Asia/Shanghai';

INSERT INTO sys_role (id, code, name, built_in, remark) VALUES
    (1, 'admin',    '系统管理员', 1, '全量权限'),
    (2, 'hr',       '人事',       1, '员工档案、考勤、字典与通讯录维护'),
    (3, 'ops',      '运营',       1, '大促节奏与公告维护'),
    (4, 'manager',  '主管',       1, '有下属时的团队视角'),
    (5, 'employee', '员工',       1, '仅个人中心') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (id, parent_id, name, type, path, permission, sort_order) VALUES
    (1, 0, '通讯录同步', 'button', '', 'system:directory:sync',   1),
    (2, 0, '组织查询',   'button', '', 'system:dept:query',       2),
    (3, 0, '账号查询',   'button', '', 'system:user:query',       3),
    (4, 0, '账号授权',   'button', '', 'system:user:assign-role', 4),
    (5, 0, '角色查询',   'button', '', 'system:role:query',       5),
    (6, 0, '字典查询',   'button', '', 'system:dict:query',       6),
    (7, 0, '字典维护',   'button', '', 'system:dict:update',      7),
    (8, 0, '他人档案',   'button', '', 'hrm:employee:query',      8) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
    (2, 2), (2, 3), (2, 6), (2, 7), (2, 8),
    (3, 6) ON CONFLICT DO NOTHING;

INSERT INTO sys_user (id, wecom_userid, username, name, position, enabled, password_hash) VALUES
    (1, NULL, 'admin', '张伟', '市场部 / P6', 1,
     'pbkdf2:sha256:210000$m3g503RNYWhLM6ctXjG2dw==$UHKQeq5ibcoaOaW1xVYAJjdhzDh1IXfx9VAeG7+WjHE=') ON CONFLICT DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1) ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('sys_menu', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_menu_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_menu_id_seq))) FROM sys_menu;

SELECT setval(pg_get_serial_sequence('sys_role', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_role_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_role_id_seq))) FROM sys_role;

SELECT setval(pg_get_serial_sequence('sys_user', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_user_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_user_id_seq))) FROM sys_user;

-- dict-seed.sql

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('common_status', '系统状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('common_status', '开启', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('common_status', '关闭', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_source', 'HRM 打卡来源', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_source', '手机端打卡', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_source', '手工录入', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_status', 'HRM 打卡状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '正常', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '迟到', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '早退', '2', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '缺卡', '3', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_type', 'HRM 打卡类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_type', '上班打卡', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_type', '下班打卡', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_holiday_type', 'HRM 考勤节假日类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_holiday_type', '上班', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_holiday_type', '休息', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_leave_type', 'HRM 请假类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '年假', '年假', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '事假', '事假', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '病假', '病假', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '调休', '调休', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_yes_no', 'HRM 考勤是否', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_yes_no', '否', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_yes_no', '是', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_education', 'HRM 员工最高学历', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '小学', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '初中', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '中专', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '中职', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '技校', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '高中', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '大专', '7', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '本科', '8', 8, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '硕士', '9', 9, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '博士', '10', 10, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '博士后', '11', 11, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '其他', '12', 12, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_entry_status', 'HRM 员工入职状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '在职', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待入职', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待离职', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '离职', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_id_type', 'HRM 员工证件类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '身份证', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '港澳通行证', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '台湾通行证', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '护照', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '其他', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_status', 'HRM 员工状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '正式', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '试用', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '实习', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '兼职', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '劳务', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '顾问', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '返聘', '7', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '外包', '8', 8, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_type', 'HRM 聘用形式', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_type', '正式', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_type', '非正式', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_emp_status', 'HRM 参保状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_emp_status', '停保', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_emp_status', '正常', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_month_status', 'HRM 社保表状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_month_status', '未归档', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_month_status', '已归档', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_project_type', 'HRM 社保项目类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '养老保险', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '医疗保险', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '失业保险', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '工伤保险', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '生育保险', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '补充大病医疗', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '补充养老', '7', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '残保金', '8', 8, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '社保自定义', '9', 9, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '公积金', '10', 10, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '公积金自定义', '11', 11, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_scheme_type', 'HRM 社保方案类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_scheme_type', '比例', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_scheme_type', '金额', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_appeal_status', 'HRM 绩效申诉状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '无申诉', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '待处理', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '已通过', '2', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '已驳回', '3', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '已取消', '4', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_plan_status', 'HRM 绩效计划状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '草稿', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '未开始', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '进行中', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '已归档', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '终止', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_score_calculation', 'HRM 绩效计分方式', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_score_calculation', '加权计算', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_stage_status', 'HRM 绩效阶段状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '未开始', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '员工填写', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '目标确认', '2', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '自评', '3', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '他人评分', '4', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '结果审核', '5', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '结果确认', '6', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '申诉确认', '7', 8, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '归档', '8', 9, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '结束', '10', 10, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '执行中', '9', 10, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_upper_limit_type', 'HRM 绩效上限类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_upper_limit_type', '不限制', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_upper_limit_type', '固定分', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_yes_no', 'HRM 绩效是否', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_yes_no', '否', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_yes_no', '是', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_candidate_education', 'HRM 招聘候选人学历', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '小学', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '初中', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '高中', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '大专', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '本科', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '硕士', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '博士', '7', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_candidate_status', 'HRM 招聘候选人状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '新候选人', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '初选通过', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '安排面试', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '面试通过', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '已发 offer', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '待入职', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '已淘汰', '7', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '已入职', '8', 8, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_emergency_level', 'HRM 招聘紧急程度', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_emergency_level', '紧急', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_emergency_level', '一般', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_interview_result', 'HRM 招聘面试结果', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试未完成', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试通过', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试未通过', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试取消', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_interview_type', 'HRM 招聘面试方式', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '现场面试', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '电话面试', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '视频面试', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_job_nature', 'HRM 招聘工作性质', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '全职', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '实习', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '兼职', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_post_education', 'HRM 招聘职位学历要求', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '不限', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '高中及以上', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '大专及以上', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '本科及以上', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '硕士及以上', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '博士', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_post_status', 'HRM 招聘职位状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_status', '招聘中', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_status', '停止招聘', '0', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_salary_unit', 'HRM 招聘薪资单位', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_salary_unit', '元/月', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_salary_unit', '元/年', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_work_time', 'HRM 招聘工作经验', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '不限', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '1 年以下', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '1-3 年', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '3-5 年', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '5-10 年', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '10 年以上', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_reason', 'HRM 薪资调整原因', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '入职定薪', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '入职核定', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '转正', '2', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '晋升', '3', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '调动', '4', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '年中调薪', '5', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '年度调薪', '6', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '特别调薪', '7', 8, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '其他', '8', 9, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_record_status', 'HRM 薪资调整记录状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '待生效', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '生效', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '已取消', '2', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_type', 'HRM 薪资档案状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '未定薪', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '已定薪', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '已调薪', '2', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_month_status', 'HRM 工资表状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '待核算', '5', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '已核算', '11', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '历史工资表', '10', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_slip_read_status', 'HRM 工资条阅读状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_slip_read_status', '未读', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_slip_read_status', '已读', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_tax_type', 'HRM 薪资计税类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '工资薪金所得税', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '劳务报酬所得税', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '不计税', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_yes_no', 'HRM 薪资是否', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_yes_no', '否', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_yes_no', '是', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('infra_boolean_string', 'Boolean 是否类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('infra_boolean_string', '是', 'true', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('infra_boolean_string', '否', 'false', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_apply_urgency', '申请紧急程度', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '正常', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '重要', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '紧急', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_expense_type', '费用类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '交通费', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '住宿费', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '餐饮费', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '通讯费', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '市内交通', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '其他', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_leave_type', '请假类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '年假', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '事假', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '病假', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '婚假', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '产假及哺乳假', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '陪产假', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '丧假', '7', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_overtime_type', '加班类型', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '工作日', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '休息日', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '节假日', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '其他', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_pay_status', '支付状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_pay_status', '未支付', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_pay_status', '已支付', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_reimburse_status', '报销状态', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimburse_status', '未报销', '0', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimburse_status', '已报销', '1', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_reimbursement_payment_method', 'OA 报销方式', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '银行卡', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '现金', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '其他', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_supply_category', '办公用品分类', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '文具类', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '打印耗材', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '生活用品', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '电脑办公', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '办公设备', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '电器', '7', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '财务用品', '6', 7, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_supply_pickup_method', '办公用品领取方式', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_pickup_method', '自取', '1', 0, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_transport_type', '交通方式', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '飞机', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '高铁/动车', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '火车', '3', 3, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '长途汽车', '4', 4, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '自驾', '5', 5, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '其他', '6', 6, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('system_user_sex', '用户性别', 1, '系统内置字典') ON CONFLICT (type) DO UPDATE SET name = EXCLUDED.name, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('system_user_sex', '男', '1', 1, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('system_user_sex', '女', '2', 2, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order, status = 1;

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待完善', '0', 0, 1) ON CONFLICT (dict_type,value) DO UPDATE SET label = EXCLUDED.label;

-- permission-seed.sql

INSERT INTO sys_menu (id, parent_id, name, type, path, permission, sort_order) VALUES
    (201, 0, '人事管理', 'dir', '', '', 20),
    (202, 201, 'HRM 打卡记录', 'menu', '', '', 1),
    (203, 202, '新增', 'button', '', 'hrm:attendance:clock:create', 0),
    (204, 202, '修改', 'button', '', 'hrm:attendance:clock:update', 0),
    (205, 202, '删除', 'button', '', 'hrm:attendance:clock:delete', 0),
    (206, 202, '查询', 'button', '', 'hrm:attendance:clock:query', 0),
    (207, 202, '导出', 'button', '', 'hrm:attendance:clock:export', 0),
    (208, 201, 'HRM 考勤组', 'menu', '', '', 2),
    (209, 208, '新增', 'button', '', 'hrm:attendance:group:create', 0),
    (210, 208, '修改', 'button', '', 'hrm:attendance:group:update', 0),
    (211, 208, '删除', 'button', '', 'hrm:attendance:group:delete', 0),
    (212, 208, '查询', 'button', '', 'hrm:attendance:group:query', 0),
    (213, 201, 'HRM 考勤节假日', 'menu', '', '', 3),
    (214, 213, '新增', 'button', '', 'hrm:attendance:holiday:create', 0),
    (215, 213, '修改', 'button', '', 'hrm:attendance:holiday:update', 0),
    (216, 213, '删除', 'button', '', 'hrm:attendance:holiday:delete', 0),
    (217, 213, '查询', 'button', '', 'hrm:attendance:holiday:query', 0),
    (218, 201, 'HRM 请假', 'menu', '', '', 4),
    (219, 218, '查询', 'button', '', 'hrm:attendance:leave:query', 0),
    (220, 218, '导出', 'button', '', 'hrm:attendance:leave:export', 0),
    (221, 201, 'HRM 考勤统计', 'menu', '', '', 5),
    (222, 221, '查询', 'button', '', 'hrm:attendance:clock:query', 0),
    (223, 221, '查询', 'button', '', 'hrm:attendance:statistics:query', 0),
    (224, 221, '导出', 'button', '', 'hrm:attendance:statistics:export', 0),
    (225, 221, '导出', 'button', '', 'hrm:attendance:clock:export', 0),
    (226, 201, 'HRM 员工证书', 'menu', '', '', 6),
    (227, 226, '查询', 'button', '', 'hrm:employee:query', 0),
    (228, 226, '修改', 'button', '', 'hrm:employee:update', 0),
    (229, 226, '删除', 'button', '', 'hrm:employee:delete', 0),
    (230, 201, 'HRM 员工异动记录', 'menu', '', '', 7),
    (231, 230, '查询', 'button', '', 'hrm:employee:query', 0),
    (232, 201, 'HRM 员工联系人', 'menu', '', '', 8),
    (233, 232, '查询', 'button', '', 'hrm:employee:query', 0),
    (234, 232, '修改', 'button', '', 'hrm:employee:update', 0),
    (235, 232, '删除', 'button', '', 'hrm:employee:delete', 0),
    (236, 201, 'HRM 员工合同', 'menu', '', '', 9),
    (237, 236, '查询', 'button', '', 'hrm:employee:query', 0),
    (238, 236, '修改', 'button', '', 'hrm:employee:update', 0),
    (239, 236, '删除', 'button', '', 'hrm:employee:delete', 0),
    (240, 201, 'HRM 员工档案', 'menu', '', '', 10),
    (241, 240, '新增', 'button', '', 'hrm:employee:create', 0),
    (242, 240, '修改', 'button', '', 'hrm:employee:update', 0),
    (243, 240, '删除', 'button', '', 'hrm:employee:delete', 0),
    (244, 240, '查询', 'button', '', 'hrm:employee:query', 0),
    (245, 240, '导出', 'button', '', 'hrm:employee:export', 0),
    (246, 240, '导入', 'button', '', 'hrm:employee:import', 0),
    (247, 201, 'HRM 员工教育经历', 'menu', '', '', 11),
    (248, 247, '查询', 'button', '', 'hrm:employee:query', 0),
    (249, 247, '修改', 'button', '', 'hrm:employee:update', 0),
    (250, 247, '删除', 'button', '', 'hrm:employee:delete', 0),
    (251, 201, 'HRM 员工字段配置', 'menu', '', '', 12),
    (252, 251, '查询', 'button', '', 'hrm:employee:config:query', 0),
    (253, 251, '修改', 'button', '', 'hrm:employee:config:update', 0),
    (254, 201, 'HRM 员工材料附件', 'menu', '', '', 13),
    (255, 254, '查询', 'button', '', 'hrm:employee:query', 0),
    (256, 254, '修改', 'button', '', 'hrm:employee:update', 0),
    (257, 201, 'HRM 员工个人备忘', 'menu', '', '', 14),
    (258, 257, '新增', 'button', '', 'hrm:employee:personal-note:create', 0),
    (259, 257, '删除', 'button', '', 'hrm:employee:personal-note:delete', 0),
    (260, 201, 'HRM 员工离职信息', 'menu', '', '', 15),
    (261, 260, '查询', 'button', '', 'hrm:employee:query', 0),
    (262, 201, 'HRM 员工工资卡', 'menu', '', '', 16),
    (263, 262, '查询', 'button', '', 'hrm:employee:query', 0),
    (264, 262, '修改', 'button', '', 'hrm:employee:update', 0),
    (265, 201, 'HRM 员工培训经历', 'menu', '', '', 17),
    (266, 265, '查询', 'button', '', 'hrm:employee:query', 0),
    (267, 265, '修改', 'button', '', 'hrm:employee:update', 0),
    (268, 265, '删除', 'button', '', 'hrm:employee:delete', 0),
    (269, 201, 'HRM 员工工作经历', 'menu', '', '', 18),
    (270, 269, '查询', 'button', '', 'hrm:employee:query', 0),
    (271, 269, '修改', 'button', '', 'hrm:employee:update', 0),
    (272, 269, '删除', 'button', '', 'hrm:employee:delete', 0),
    (273, 201, 'HRM 首页', 'menu', '', '', 19),
    (274, 273, 'hr-query', 'button', '', 'hrm:home:hr-query', 0),
    (275, 273, 'team-query', 'button', '', 'hrm:home:team-query', 0),
    (276, 201, 'HRM 员工参保信息', 'menu', '', '', 20),
    (277, 276, '查询', 'button', '', 'hrm:insurance:employee-info:query', 0),
    (278, 276, '修改', 'button', '', 'hrm:insurance:employee-info:update', 0),
    (279, 201, 'HRM 员工月度社保', 'menu', '', '', 21),
    (280, 279, '查询', 'button', '', 'hrm:insurance:month-record:query', 0),
    (281, 279, '修改', 'button', '', 'hrm:insurance:month-record:update', 0),
    (282, 201, 'HRM 月度社保表', 'menu', '', '', 22),
    (283, 282, '新增', 'button', '', 'hrm:insurance:month-record:create', 0),
    (284, 282, '删除', 'button', '', 'hrm:insurance:month-record:delete', 0),
    (285, 282, '查询', 'button', '', 'hrm:insurance:month-record:query', 0),
    (286, 201, 'HRM 社保方案', 'menu', '', '', 23),
    (287, 286, '新增', 'button', '', 'hrm:insurance:scheme:create', 0),
    (288, 286, '修改', 'button', '', 'hrm:insurance:scheme:update', 0),
    (289, 286, '删除', 'button', '', 'hrm:insurance:scheme:delete', 0),
    (290, 286, '查询', 'button', '', 'hrm:insurance:scheme:query', 0),
    (291, 201, 'HRM 标准参保数据', 'menu', '', '', 24),
    (292, 291, '查询', 'button', '', 'hrm:insurance:scheme:query', 0),
    (293, 201, 'HRM 操作日志', 'menu', '', '', 25),
    (294, 201, 'HRM 员工绩效考核', 'menu', '', '', 26),
    (295, 294, '修改', 'button', '', 'hrm:performance:plan:update', 0),
    (296, 294, '查询', 'button', '', 'hrm:performance:plan:query', 0),
    (297, 294, '查询', 'button', '', 'hrm:performance:archive:query', 0),
    (298, 294, '删除', 'button', '', 'hrm:performance:archive:delete', 0),
    (299, 201, 'HRM 绩效考核模板', 'menu', '', '', 27),
    (300, 299, '新增', 'button', '', 'hrm:performance:assessment-template:create', 0),
    (301, 299, '修改', 'button', '', 'hrm:performance:assessment-template:update', 0),
    (302, 299, '删除', 'button', '', 'hrm:performance:assessment-template:delete', 0),
    (303, 299, '查询', 'button', '', 'hrm:performance:assessment-template:query', 0),
    (304, 201, 'HRM 绩效计划', 'menu', '', '', 28),
    (305, 304, '新增', 'button', '', 'hrm:performance:plan:create', 0),
    (306, 304, '修改', 'button', '', 'hrm:performance:plan:update', 0),
    (307, 304, '删除', 'button', '', 'hrm:performance:plan:delete', 0),
    (308, 304, '查询', 'button', '', 'hrm:performance:plan:query', 0),
    (309, 201, 'HRM 绩效结果模板', 'menu', '', '', 29),
    (310, 309, '新增', 'button', '', 'hrm:performance:result-template:create', 0),
    (311, 309, '修改', 'button', '', 'hrm:performance:result-template:update', 0),
    (312, 309, '删除', 'button', '', 'hrm:performance:result-template:delete', 0),
    (313, 309, '查询', 'button', '', 'hrm:performance:result-template:query', 0),
    (314, 201, 'HRM 员工端考勤打卡', 'menu', '', '', 30),
    (315, 314, '查询', 'button', '', 'hrm:portal:query', 0),
    (316, 201, 'HRM 员工端请假', 'menu', '', '', 31),
    (317, 316, '查询', 'button', '', 'hrm:portal:query', 0),
    (318, 316, 'leave', 'button', '', 'hrm:portal:attendance:leave', 0),
    (319, 201, 'HRM 员工端考勤统计', 'menu', '', '', 32),
    (320, 319, '查询', 'button', '', 'hrm:portal:query', 0),
    (321, 201, 'HRM 员工端证书', 'menu', '', '', 33),
    (322, 321, '查询', 'button', '', 'hrm:portal:query', 0),
    (323, 201, 'HRM 员工端联系人', 'menu', '', '', 34),
    (324, 323, '查询', 'button', '', 'hrm:portal:query', 0),
    (325, 201, 'HRM 员工端档案', 'menu', '', '', 35),
    (326, 325, '查询', 'button', '', 'hrm:portal:query', 0),
    (327, 325, '修改', 'button', '', 'hrm:portal:employee:update', 0),
    (328, 201, 'HRM 员工端教育经历', 'menu', '', '', 36),
    (329, 328, '查询', 'button', '', 'hrm:portal:query', 0),
    (330, 201, 'HRM 员工端档案字段配置', 'menu', '', '', 37),
    (331, 330, '查询', 'button', '', 'hrm:portal:query', 0),
    (332, 201, 'HRM 员工端离职信息', 'menu', '', '', 38),
    (333, 332, '查询', 'button', '', 'hrm:portal:query', 0),
    (334, 201, 'HRM 员工端培训经历', 'menu', '', '', 39),
    (335, 334, '查询', 'button', '', 'hrm:portal:query', 0),
    (336, 201, 'HRM 员工端工作经历', 'menu', '', '', 40),
    (337, 336, '查询', 'button', '', 'hrm:portal:query', 0),
    (338, 201, 'HRM 员工端首页', 'menu', '', '', 41),
    (339, 338, '查询', 'button', '', 'hrm:portal:query', 0),
    (340, 201, 'HRM 员工端社保', 'menu', '', '', 42),
    (341, 340, '查询', 'button', '', 'hrm:portal:query', 0),
    (342, 201, 'HRM 员工端绩效考核', 'menu', '', '', 43),
    (343, 342, '查询', 'button', '', 'hrm:portal:query', 0),
    (344, 342, 'action', 'button', '', 'hrm:portal:performance:action', 0),
    (345, 201, 'HRM 员工端工资条', 'menu', '', '', 44),
    (346, 345, '查询', 'button', '', 'hrm:portal:query', 0),
    (347, 201, 'HRM 招聘候选人', 'menu', '', '', 45),
    (348, 347, '新增', 'button', '', 'hrm:recruit:candidate:create', 0),
    (349, 347, '修改', 'button', '', 'hrm:recruit:candidate:update', 0),
    (350, 347, '删除', 'button', '', 'hrm:recruit:candidate:delete', 0),
    (351, 347, '查询', 'button', '', 'hrm:recruit:candidate:query', 0),
    (352, 201, 'HRM 招聘渠道', 'menu', '', '', 46),
    (353, 352, '新增', 'button', '', 'hrm:recruit:channel:create', 0),
    (354, 352, '修改', 'button', '', 'hrm:recruit:channel:update', 0),
    (355, 352, '删除', 'button', '', 'hrm:recruit:channel:delete', 0),
    (356, 352, '查询', 'button', '', 'hrm:recruit:channel:query', 0),
    (357, 201, 'HRM 招聘设置', 'menu', '', '', 47),
    (358, 357, '修改', 'button', '', 'hrm:recruit:config:update', 0),
    (359, 201, 'HRM 招聘面试', 'menu', '', '', 48),
    (360, 359, '新增', 'button', '', 'hrm:recruit:interview:create', 0),
    (361, 359, '修改', 'button', '', 'hrm:recruit:interview:update', 0),
    (362, 359, '删除', 'button', '', 'hrm:recruit:interview:delete', 0),
    (363, 359, '查询', 'button', '', 'hrm:recruit:interview:query', 0),
    (364, 201, 'HRM 招聘职位', 'menu', '', '', 49),
    (365, 364, '新增', 'button', '', 'hrm:recruit:post:create', 0),
    (366, 364, '修改', 'button', '', 'hrm:recruit:post:update', 0),
    (367, 364, '查询', 'button', '', 'hrm:recruit:post:query', 0),
    (368, 201, 'HRM 招聘职位类型', 'menu', '', '', 50),
    (369, 368, '查询', 'button', '', 'hrm:recruit:post:query', 0),
    (370, 201, 'HRM 定薪/调薪记录', 'menu', '', '', 51),
    (371, 370, '查询', 'button', '', 'hrm:salary:employee-info:query', 0),
    (372, 370, '修改', 'button', '', 'hrm:salary:employee-info:update', 0),
    (373, 370, '删除', 'button', '', 'hrm:salary:change-record:delete', 0),
    (374, 201, 'HRM 调薪模板', 'menu', '', '', 52),
    (375, 374, '新增', 'button', '', 'hrm:salary:change-template:create', 0),
    (376, 374, '修改', 'button', '', 'hrm:salary:change-template:update', 0),
    (377, 374, '删除', 'button', '', 'hrm:salary:change-template:delete', 0),
    (378, 374, '查询', 'button', '', 'hrm:salary:change-template:query', 0),
    (379, 201, 'HRM 薪资配置', 'menu', '', '', 53),
    (380, 379, '修改', 'button', '', 'hrm:salary:config:update', 0),
    (381, 379, '查询', 'button', '', 'hrm:salary:config:query', 0),
    (382, 201, 'HRM 员工薪资信息', 'menu', '', '', 54),
    (383, 382, '查询', 'button', '', 'hrm:salary:employee-info:query', 0),
    (384, 382, '修改', 'button', '', 'hrm:salary:employee-info:update', 0),
    (385, 382, '导入', 'button', '', 'hrm:salary:employee-info:import', 0),
    (386, 201, 'HRM 薪资组', 'menu', '', '', 55),
    (387, 386, '新增', 'button', '', 'hrm:salary:group:create', 0),
    (388, 386, '修改', 'button', '', 'hrm:salary:group:update', 0),
    (389, 386, '删除', 'button', '', 'hrm:salary:group:delete', 0),
    (390, 386, '查询', 'button', '', 'hrm:salary:group:query', 0),
    (391, 201, 'HRM 员工月度工资', 'menu', '', '', 56),
    (392, 391, '修改', 'button', '', 'hrm:salary:month-record:update', 0),
    (393, 391, '查询', 'button', '', 'hrm:salary:month-record:query', 0),
    (394, 201, 'HRM 月度工资表', 'menu', '', '', 57),
    (395, 394, '新增', 'button', '', 'hrm:salary:month-record:create', 0),
    (396, 394, 'compute', 'button', '', 'hrm:salary:month-record:compute', 0),
    (397, 394, '删除', 'button', '', 'hrm:salary:month-record:delete', 0),
    (398, 394, '查询', 'button', '', 'hrm:salary:month-record:query', 0),
    (399, 201, 'HRM 薪资项', 'menu', '', '', 58),
    (400, 399, '新增', 'button', '', 'hrm:salary:option:create', 0),
    (401, 399, '修改', 'button', '', 'hrm:salary:option:update', 0),
    (402, 399, '删除', 'button', '', 'hrm:salary:option:delete', 0),
    (403, 399, '查询', 'button', '', 'hrm:salary:option:query', 0),
    (404, 201, 'HRM 工资条', 'menu', '', '', 59),
    (405, 404, '查询', 'button', '', 'hrm:salary:slip:query', 0),
    (406, 404, '修改', 'button', '', 'hrm:salary:slip:update', 0),
    (407, 201, 'HRM 工资条发放记录', 'menu', '', '', 60),
    (408, 407, '新增', 'button', '', 'hrm:salary:slip:create', 0),
    (409, 407, '查询', 'button', '', 'hrm:salary:slip:query', 0),
    (410, 407, '删除', 'button', '', 'hrm:salary:slip:delete', 0),
    (411, 201, 'HRM 工资条模板', 'menu', '', '', 61),
    (412, 411, '修改', 'button', '', 'hrm:salary:slip:update', 0),
    (413, 411, '删除', 'button', '', 'hrm:salary:slip:delete', 0),
    (414, 411, '查询', 'button', '', 'hrm:salary:slip:query', 0),
    (415, 201, 'HRM 计税规则', 'menu', '', '', 62),
    (416, 415, '新增', 'button', '', 'hrm:salary:tax-rule:create', 0),
    (417, 415, '修改', 'button', '', 'hrm:salary:tax-rule:update', 0),
    (418, 415, '删除', 'button', '', 'hrm:salary:tax-rule:delete', 0),
    (419, 415, '查询', 'button', '', 'hrm:salary:tax-rule:query', 0),
    (420, 0, '协同办公', 'dir', '', '', 30),
    (421, 420, 'OA 公告', 'menu', '', '', 1),
    (422, 421, '新增', 'button', '', 'oa:announcement:create', 0),
    (423, 421, '修改', 'button', '', 'oa:announcement:update', 0),
    (424, 421, '删除', 'button', '', 'oa:announcement:delete', 0),
    (425, 421, '查询', 'button', '', 'oa:announcement:query', 0),
    (426, 420, 'OA 考勤', 'menu', '', '', 2),
    (427, 426, '修改', 'button', '', 'oa:attendance:update', 0),
    (428, 426, '删除', 'button', '', 'oa:attendance:delete', 0),
    (429, 426, '查询', 'button', '', 'oa:attendance:query', 0),
    (430, 420, '请假申请', 'menu', '', '', 3),
    (431, 430, '新增', 'button', '', 'oa:leave-apply:create', 0),
    (432, 430, '查询', 'button', '', 'oa:leave-apply:query', 0),
    (433, 420, '加班申请', 'menu', '', '', 4),
    (434, 433, '新增', 'button', '', 'oa:overtime-apply:create', 0),
    (435, 433, '查询', 'button', '', 'oa:overtime-apply:query', 0),
    (436, 420, '费用报销', 'menu', '', '', 5),
    (437, 436, '新增', 'button', '', 'oa:reimbursement:create', 0),
    (438, 436, '查询', 'button', '', 'oa:reimbursement:query', 0),
    (439, 420, 'OA 日程', 'menu', '', '', 6),
    (440, 439, '新增', 'button', '', 'oa:schedule:create', 0),
    (441, 439, '修改', 'button', '', 'oa:schedule:update', 0),
    (442, 439, '删除', 'button', '', 'oa:schedule:delete', 0),
    (443, 439, '查询', 'button', '', 'oa:schedule:query', 0),
    (444, 420, '用印申请', 'menu', '', '', 7),
    (445, 444, '新增', 'button', '', 'oa:seal-apply:create', 0),
    (446, 444, '修改', 'button', '', 'oa:seal-apply:update', 0),
    (447, 444, '删除', 'button', '', 'oa:seal-apply:delete', 0),
    (448, 444, '查询', 'button', '', 'oa:seal-apply:query', 0),
    (449, 420, '印章', 'menu', '', '', 8),
    (450, 449, '新增', 'button', '', 'oa:seal:create', 0),
    (451, 449, '修改', 'button', '', 'oa:seal:update', 0),
    (452, 449, '删除', 'button', '', 'oa:seal:delete', 0),
    (453, 449, '查询', 'button', '', 'oa:seal:query', 0),
    (454, 420, '用品领用申请', 'menu', '', '', 9),
    (455, 454, '新增', 'button', '', 'oa:supply-apply:create', 0),
    (456, 454, '修改', 'button', '', 'oa:supply-apply:update', 0),
    (457, 454, '删除', 'button', '', 'oa:supply-apply:delete', 0),
    (458, 454, '查询', 'button', '', 'oa:supply-apply:query', 0),
    (459, 420, '领用发放', 'menu', '', '', 10),
    (460, 459, '查询', 'button', '', 'oa:supply-issue:query', 0),
    (461, 459, 'issue', 'button', '', 'oa:supply-issue:issue', 0),
    (462, 459, 'return', 'button', '', 'oa:supply-issue:return', 0),
    (463, 420, '办公用品', 'menu', '', '', 11),
    (464, 463, '新增', 'button', '', 'oa:supply-item:create', 0),
    (465, 463, '修改', 'button', '', 'oa:supply-item:update', 0),
    (466, 463, '删除', 'button', '', 'oa:supply-item:delete', 0),
    (467, 463, '查询', 'button', '', 'oa:supply-item:query', 0),
    (468, 463, 'stock-in', 'button', '', 'oa:supply-item:stock-in', 0),
    (469, 420, '出差申请', 'menu', '', '', 12),
    (470, 469, 'save', 'button', '', 'oa:travel-apply:save', 0),
    (471, 469, '删除', 'button', '', 'oa:travel-apply:delete', 0),
    (472, 469, '查询', 'button', '', 'oa:travel-apply:query', 0),
    (473, 420, '出差报销', 'menu', '', '', 13),
    (474, 473, 'save', 'button', '', 'oa:travel-reimbursement:save', 0),
    (475, 473, '删除', 'button', '', 'oa:travel-reimbursement:delete', 0),
    (476, 473, '查询', 'button', '', 'oa:travel-reimbursement:query', 0),
    (477, 420, 'OA 工作汇报', 'menu', '', '', 14),
    (478, 477, '新增', 'button', '', 'oa:work-report:create', 0),
    (479, 477, '修改', 'button', '', 'oa:work-report:update', 0),
    (480, 477, '删除', 'button', '', 'oa:work-report:delete', 0),
    (481, 477, '查询', 'button', '', 'oa:work-report:query', 0),
    (482, 477, 'statistics', 'button', '', 'oa:work-report:statistics', 0) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 202),
    (1, 203),
    (1, 204),
    (1, 205),
    (1, 206),
    (1, 207),
    (1, 208),
    (1, 209),
    (1, 210),
    (1, 211),
    (1, 212),
    (1, 213) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 214),
    (1, 215),
    (1, 216),
    (1, 217),
    (1, 218),
    (1, 219),
    (1, 220),
    (1, 221),
    (1, 222),
    (1, 223),
    (1, 224),
    (1, 225) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 226),
    (1, 227),
    (1, 228),
    (1, 229),
    (1, 230),
    (1, 231),
    (1, 232),
    (1, 233),
    (1, 234),
    (1, 235),
    (1, 236),
    (1, 237) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 238),
    (1, 239),
    (1, 240),
    (1, 241),
    (1, 242),
    (1, 243),
    (1, 244),
    (1, 245),
    (1, 246),
    (1, 247),
    (1, 248),
    (1, 249) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 250),
    (1, 251),
    (1, 252),
    (1, 253),
    (1, 254),
    (1, 255),
    (1, 256),
    (1, 257),
    (1, 258),
    (1, 259),
    (1, 260),
    (1, 261) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 262),
    (1, 263),
    (1, 264),
    (1, 265),
    (1, 266),
    (1, 267),
    (1, 268),
    (1, 269),
    (1, 270),
    (1, 271),
    (1, 272),
    (1, 273) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 274),
    (1, 275),
    (1, 276),
    (1, 277),
    (1, 278),
    (1, 279),
    (1, 280),
    (1, 281),
    (1, 282),
    (1, 283),
    (1, 284),
    (1, 285) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 286),
    (1, 287),
    (1, 288),
    (1, 289),
    (1, 290),
    (1, 291),
    (1, 292),
    (1, 293),
    (1, 294),
    (1, 295),
    (1, 296),
    (1, 297) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 298),
    (1, 299),
    (1, 300),
    (1, 301),
    (1, 302),
    (1, 303),
    (1, 304),
    (1, 305),
    (1, 306),
    (1, 307),
    (1, 308),
    (1, 309) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 310),
    (1, 311),
    (1, 312),
    (1, 313),
    (1, 314),
    (1, 315),
    (1, 316),
    (1, 317),
    (1, 318),
    (1, 319),
    (1, 320),
    (1, 321) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 322),
    (1, 323),
    (1, 324),
    (1, 325),
    (1, 326),
    (1, 327),
    (1, 328),
    (1, 329),
    (1, 330),
    (1, 331),
    (1, 332),
    (1, 333) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 334),
    (1, 335),
    (1, 336),
    (1, 337),
    (1, 338),
    (1, 339),
    (1, 340),
    (1, 341),
    (1, 342),
    (1, 343),
    (1, 344),
    (1, 345) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 346),
    (1, 347),
    (1, 348),
    (1, 349),
    (1, 350),
    (1, 351),
    (1, 352),
    (1, 353),
    (1, 354),
    (1, 355),
    (1, 356),
    (1, 357) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 358),
    (1, 359),
    (1, 360),
    (1, 361),
    (1, 362),
    (1, 363),
    (1, 364),
    (1, 365),
    (1, 366),
    (1, 367),
    (1, 368),
    (1, 369) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 370),
    (1, 371),
    (1, 372),
    (1, 373),
    (1, 374),
    (1, 375),
    (1, 376),
    (1, 377),
    (1, 378),
    (1, 379),
    (1, 380),
    (1, 381) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 382),
    (1, 383),
    (1, 384),
    (1, 385),
    (1, 386),
    (1, 387),
    (1, 388),
    (1, 389),
    (1, 390),
    (1, 391),
    (1, 392),
    (1, 393) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 394),
    (1, 395),
    (1, 396),
    (1, 397),
    (1, 398),
    (1, 399),
    (1, 400),
    (1, 401),
    (1, 402),
    (1, 403),
    (1, 404),
    (1, 405) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 406),
    (1, 407),
    (1, 408),
    (1, 409),
    (1, 410),
    (1, 411),
    (1, 412),
    (1, 413),
    (1, 414),
    (1, 415),
    (1, 416),
    (1, 417) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 418),
    (1, 419),
    (1, 421),
    (1, 422),
    (1, 423),
    (1, 424),
    (1, 425),
    (1, 426),
    (1, 427),
    (1, 428),
    (1, 429),
    (1, 430) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 431),
    (1, 432),
    (1, 433),
    (1, 434),
    (1, 435),
    (1, 436),
    (1, 437),
    (1, 438),
    (1, 439),
    (1, 440),
    (1, 441),
    (1, 442) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 443),
    (1, 444),
    (1, 445),
    (1, 446),
    (1, 447),
    (1, 448),
    (1, 449),
    (1, 450),
    (1, 451),
    (1, 452),
    (1, 453),
    (1, 454) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 455),
    (1, 456),
    (1, 457),
    (1, 458),
    (1, 459),
    (1, 460),
    (1, 461),
    (1, 462),
    (1, 463),
    (1, 464),
    (1, 465),
    (1, 466) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 467),
    (1, 468),
    (1, 469),
    (1, 470),
    (1, 471),
    (1, 472),
    (1, 473),
    (1, 474),
    (1, 475),
    (1, 476),
    (1, 477),
    (1, 478) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 479),
    (1, 480),
    (1, 481),
    (1, 482) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 202),
    (2, 203),
    (2, 204),
    (2, 205),
    (2, 206),
    (2, 207),
    (2, 208),
    (2, 209),
    (2, 210),
    (2, 211),
    (2, 212),
    (2, 213) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 214),
    (2, 215),
    (2, 216),
    (2, 217),
    (2, 218),
    (2, 219),
    (2, 220),
    (2, 221),
    (2, 222),
    (2, 223),
    (2, 224),
    (2, 225) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 226),
    (2, 227),
    (2, 228),
    (2, 229),
    (2, 230),
    (2, 231),
    (2, 232),
    (2, 233),
    (2, 234),
    (2, 235),
    (2, 236),
    (2, 237) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 238),
    (2, 239),
    (2, 240),
    (2, 241),
    (2, 242),
    (2, 243),
    (2, 244),
    (2, 245),
    (2, 246),
    (2, 247),
    (2, 248),
    (2, 249) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 250),
    (2, 251),
    (2, 252),
    (2, 253),
    (2, 254),
    (2, 255),
    (2, 256),
    (2, 257),
    (2, 258),
    (2, 259),
    (2, 260),
    (2, 261) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 262),
    (2, 263),
    (2, 264),
    (2, 265),
    (2, 266),
    (2, 267),
    (2, 268),
    (2, 269),
    (2, 270),
    (2, 271),
    (2, 272),
    (2, 273) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 274),
    (2, 275),
    (2, 276),
    (2, 277),
    (2, 278),
    (2, 279),
    (2, 280),
    (2, 281),
    (2, 282),
    (2, 283),
    (2, 284),
    (2, 285) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 286),
    (2, 287),
    (2, 288),
    (2, 289),
    (2, 290),
    (2, 291),
    (2, 292),
    (2, 293),
    (2, 294),
    (2, 295),
    (2, 296),
    (2, 297) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 298),
    (2, 299),
    (2, 300),
    (2, 301),
    (2, 302),
    (2, 303),
    (2, 304),
    (2, 305),
    (2, 306),
    (2, 307),
    (2, 308),
    (2, 309) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 310),
    (2, 311),
    (2, 312),
    (2, 313),
    (2, 314),
    (2, 315),
    (2, 316),
    (2, 317),
    (2, 318),
    (2, 319),
    (2, 320),
    (2, 321) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 322),
    (2, 323),
    (2, 324),
    (2, 325),
    (2, 326),
    (2, 327),
    (2, 328),
    (2, 329),
    (2, 330),
    (2, 331),
    (2, 332),
    (2, 333) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 334),
    (2, 335),
    (2, 336),
    (2, 337),
    (2, 338),
    (2, 339),
    (2, 340),
    (2, 341),
    (2, 342),
    (2, 343),
    (2, 344),
    (2, 345) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 346),
    (2, 347),
    (2, 348),
    (2, 349),
    (2, 350),
    (2, 351),
    (2, 352),
    (2, 353),
    (2, 354),
    (2, 355),
    (2, 356),
    (2, 357) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 358),
    (2, 359),
    (2, 360),
    (2, 361),
    (2, 362),
    (2, 363),
    (2, 364),
    (2, 365),
    (2, 366),
    (2, 367),
    (2, 368),
    (2, 369) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 370),
    (2, 371),
    (2, 372),
    (2, 373),
    (2, 374),
    (2, 375),
    (2, 376),
    (2, 377),
    (2, 378),
    (2, 379),
    (2, 380),
    (2, 381) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 382),
    (2, 383),
    (2, 384),
    (2, 385),
    (2, 386),
    (2, 387),
    (2, 388),
    (2, 389),
    (2, 390),
    (2, 391),
    (2, 392),
    (2, 393) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 394),
    (2, 395),
    (2, 396),
    (2, 397),
    (2, 398),
    (2, 399),
    (2, 400),
    (2, 401),
    (2, 402),
    (2, 403),
    (2, 404),
    (2, 405) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 406),
    (2, 407),
    (2, 408),
    (2, 409),
    (2, 410),
    (2, 411),
    (2, 412),
    (2, 413),
    (2, 414),
    (2, 415),
    (2, 416),
    (2, 417) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 418),
    (2, 419) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 421),
    (3, 422),
    (3, 423),
    (3, 424),
    (3, 425),
    (3, 426),
    (3, 427),
    (3, 428),
    (3, 429),
    (3, 430),
    (3, 431),
    (3, 432) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 433),
    (3, 434),
    (3, 435),
    (3, 436),
    (3, 437),
    (3, 438),
    (3, 439),
    (3, 440),
    (3, 441),
    (3, 442),
    (3, 443),
    (3, 444) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 445),
    (3, 446),
    (3, 447),
    (3, 448),
    (3, 449),
    (3, 450),
    (3, 451),
    (3, 452),
    (3, 453),
    (3, 454),
    (3, 455),
    (3, 456) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 457),
    (3, 458),
    (3, 459),
    (3, 460),
    (3, 461),
    (3, 462),
    (3, 463),
    (3, 464),
    (3, 465),
    (3, 466),
    (3, 467),
    (3, 468) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 469),
    (3, 470),
    (3, 471),
    (3, 472),
    (3, 473),
    (3, 474),
    (3, 475),
    (3, 476),
    (3, 477),
    (3, 478),
    (3, 479),
    (3, 480) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 481),
    (3, 482) ON CONFLICT DO NOTHING;

-- portal-admin-permission-seed.sql

INSERT INTO sys_menu(name,type,permission) SELECT '提交内容','button','can_create_content' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_create_content') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '查看自己的草稿','button','can_view_own_draft' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_own_draft') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '查看任意状态内容','button','can_view_any_status' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_any_status') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '内容审批','button','can_review' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_review') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '编辑他人稿件','button','can_edit_any' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_edit_any') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '删除内容','button','can_delete' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_delete') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '批量导入','button','can_import' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_import') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '评论治理','button','can_moderate_comments' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_moderate_comments') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '用户管理','button','can_manage_users' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_users') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '角色与授权管理','button','can_manage_roles' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_roles') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '进入后台','button','can_enter_admin' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_enter_admin') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '数据面板','button','can_view_dashboards' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_dashboards') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT 'API 成本看板','button','can_view_api_costs' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_api_costs') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '通讯录管理','button','can_manage_directory' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_directory') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT 'AI 模型与提示词','button','can_manage_ai_settings' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_ai_settings') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT 'AI 厂商 API key','button','can_manage_ai_keys' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_ai_keys') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '查看 HR SOP 库','button','can_view_hr_sop' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_hr_sop') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT 'HR 文档编辑','button','can_edit_hr_documents' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_edit_hr_documents') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '签收统计','button','can_view_signoff_stats' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_signoff_stats') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '查看全员汇报','button','can_view_all_reports' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_all_reports') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '简历识别与候选人库','button','can_manage_resumes' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_resumes') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '物资档案管理','button','can_manage_materials' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_materials') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '物资全站审批','button','can_approve_any_material_apply' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_approve_any_material_apply') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '站点负责人配置','button','can_configure_site_owners' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_configure_site_owners') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '电竞室预约管理','button','can_manage_esports' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_esports') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '查看全部电竞室预约','button','can_view_all_esports' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_view_all_esports') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '新品动态管理','button','can_manage_products' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_products') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '展会活动管理','button','can_manage_events' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_events') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '品牌手册库管理','button','can_manage_brand_docs' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_brand_docs') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '站内信发送','button','can_send_messages' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_send_messages') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '劳动合同生成','button','can_generate_contracts' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_generate_contracts') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '员工庆祝管理','button','can_manage_celebrations' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_celebrations') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu(name,type,permission) SELECT '培训中心管理','button','can_manage_training' WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE permission='can_manage_training') ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu(role_id,menu_id) SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m WHERE r.code='admin' AND LEFT(m.permission,4)='can_' ON CONFLICT DO NOTHING;

-- V20261111_001__portal_schema_baseline.sql

INSERT INTO portal_celebration_settings(id) VALUES(1) ON CONFLICT DO NOTHING;

INSERT INTO portal_entry_rollout(id,entries,revision) VALUES(1,'{}',0) ON CONFLICT DO NOTHING;

-- V20261111_002__hrm_workspace.sql

INSERT INTO sys_menu(name,type,permission)
SELECT p.name,'button',p.permission FROM (
 SELECT '人事工作台' name,'hrm:home:hr-query' permission UNION ALL
 SELECT '维护员工','hrm:employee:update' UNION ALL
 SELECT '新增员工','hrm:employee:create' UNION ALL
 SELECT '查询员工','hrm:employee:query' UNION ALL
 SELECT '招聘查询','hrm:recruit:candidate:query' UNION ALL
 SELECT '新增候选人','hrm:recruit:candidate:create' UNION ALL
 SELECT '候选人流转','hrm:recruit:candidate:update' UNION ALL
 SELECT '招聘职位查询','hrm:recruit:post:query' UNION ALL
 SELECT '招聘职位维护','hrm:recruit:post:create' UNION ALL
 SELECT '招聘渠道查询','hrm:recruit:channel:query' UNION ALL
 SELECT '面试查询','hrm:recruit:interview:query' UNION ALL
 SELECT '安排面试','hrm:recruit:interview:create' UNION ALL
 SELECT '面试反馈','hrm:recruit:interview:update' UNION ALL
 SELECT '合同模板查询','hrm:contract-template:query' UNION ALL
 SELECT '提交合同模板','hrm:contract-template:create' UNION ALL
 SELECT '审核合同模板','hrm:contract-template:review' UNION ALL
 SELECT '人事后台入口','can_enter_admin'
) p WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.permission=p.permission) ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code IN ('hr','admin') AND (
 m.permission IN ('hrm:home:hr-query','hrm:employee:query','hrm:employee:create','hrm:employee:update',
 'hrm:recruit:candidate:query','hrm:recruit:candidate:create','hrm:recruit:candidate:update',
 'hrm:recruit:post:query','hrm:recruit:post:create','hrm:recruit:channel:query',
 'hrm:recruit:interview:query','hrm:recruit:interview:create','hrm:recruit:interview:update',
 'hrm:contract-template:query','hrm:contract-template:create','can_enter_admin')
 OR (r.code='admin' AND m.permission='hrm:contract-template:review')) ON CONFLICT DO NOTHING;

-- V20261111_003__portal_workspace_features.sql

INSERT INTO sys_menu (name,type,permission)
SELECT '发声与调研管理','button','can_manage_voice'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission='can_manage_voice') ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (name,type,permission)
SELECT '审批流程与制度规则','button','can_manage_workspace_rules'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission='can_manage_workspace_rules') ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission IN ('can_manage_voice','can_manage_workspace_rules')
WHERE r.code IN ('admin','management') ON CONFLICT DO NOTHING;

INSERT INTO sys_role_menu (role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission='can_manage_voice'
WHERE r.code='ops' ON CONFLICT DO NOTHING;

-- V20261112_001__wecom_roster_sync.sql

SELECT setval(pg_get_serial_sequence('hrm_attendance_clock', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_attendance_clock_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_attendance_clock_id_seq))) FROM hrm_attendance_clock;

SELECT setval(pg_get_serial_sequence('hrm_attendance_group', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_attendance_group_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_attendance_group_id_seq))) FROM hrm_attendance_group;

SELECT setval(pg_get_serial_sequence('hrm_attendance_holiday', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_attendance_holiday_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_attendance_holiday_id_seq))) FROM hrm_attendance_holiday;

SELECT setval(pg_get_serial_sequence('hrm_attendance_leave', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_attendance_leave_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_attendance_leave_id_seq))) FROM hrm_attendance_leave;

SELECT setval(pg_get_serial_sequence('hrm_config', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_config_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_config_id_seq))) FROM hrm_config;

SELECT setval(pg_get_serial_sequence('hrm_employee', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_id_seq))) FROM hrm_employee;

SELECT setval(pg_get_serial_sequence('hrm_employee_certificate', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_certificate_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_certificate_id_seq))) FROM hrm_employee_certificate;

SELECT setval(pg_get_serial_sequence('hrm_employee_change_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_change_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_change_record_id_seq))) FROM hrm_employee_change_record;

SELECT setval(pg_get_serial_sequence('hrm_employee_contact', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_contact_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_contact_id_seq))) FROM hrm_employee_contact;

SELECT setval(pg_get_serial_sequence('hrm_employee_contract', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_contract_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_contract_id_seq))) FROM hrm_employee_contract;

SELECT setval(pg_get_serial_sequence('hrm_employee_education_experience', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_education_experience_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_education_experience_id_seq))) FROM hrm_employee_education_experience;

SELECT setval(pg_get_serial_sequence('hrm_employee_file', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_file_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_file_id_seq))) FROM hrm_employee_file;

SELECT setval(pg_get_serial_sequence('hrm_employee_personal_note', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_personal_note_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_personal_note_id_seq))) FROM hrm_employee_personal_note;

SELECT setval(pg_get_serial_sequence('hrm_employee_quit_info', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_quit_info_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_quit_info_id_seq))) FROM hrm_employee_quit_info;

SELECT setval(pg_get_serial_sequence('hrm_employee_salary_card', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_salary_card_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_salary_card_id_seq))) FROM hrm_employee_salary_card;

SELECT setval(pg_get_serial_sequence('hrm_employee_training_experience', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_training_experience_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_training_experience_id_seq))) FROM hrm_employee_training_experience;

SELECT setval(pg_get_serial_sequence('hrm_employee_work_experience', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employee_work_experience_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employee_work_experience_id_seq))) FROM hrm_employee_work_experience;

SELECT setval(pg_get_serial_sequence('hrm_insurance_employee_info', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_insurance_employee_info_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_insurance_employee_info_id_seq))) FROM hrm_insurance_employee_info;

SELECT setval(pg_get_serial_sequence('hrm_insurance_month_employee_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_insurance_month_employee_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_insurance_month_employee_record_id_seq))) FROM hrm_insurance_month_employee_record;

SELECT setval(pg_get_serial_sequence('hrm_insurance_month_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_insurance_month_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_insurance_month_record_id_seq))) FROM hrm_insurance_month_record;

SELECT setval(pg_get_serial_sequence('hrm_insurance_scheme', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_insurance_scheme_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_insurance_scheme_id_seq))) FROM hrm_insurance_scheme;

SELECT setval(pg_get_serial_sequence('hrm_insurance_scheme_project', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_insurance_scheme_project_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_insurance_scheme_project_id_seq))) FROM hrm_insurance_scheme_project;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_id_seq))) FROM hrm_performance_assessment;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_action_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_action_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_action_record_id_seq))) FROM hrm_performance_assessment_action_record;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_appeal_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_appeal_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_appeal_record_id_seq))) FROM hrm_performance_assessment_appeal_record;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_dimension', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_dimension_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_dimension_id_seq))) FROM hrm_performance_assessment_dimension;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_quota', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_quota_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_quota_id_seq))) FROM hrm_performance_assessment_quota;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_quota_score', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_quota_score_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_quota_score_id_seq))) FROM hrm_performance_assessment_quota_score;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_stage', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_stage_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_stage_id_seq))) FROM hrm_performance_assessment_stage;

SELECT setval(pg_get_serial_sequence('hrm_performance_assessment_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_assessment_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_assessment_template_id_seq))) FROM hrm_performance_assessment_template;

SELECT setval(pg_get_serial_sequence('hrm_performance_plan', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_plan_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_plan_id_seq))) FROM hrm_performance_plan;

SELECT setval(pg_get_serial_sequence('hrm_performance_result_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_performance_result_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_performance_result_template_id_seq))) FROM hrm_performance_result_template;

SELECT setval(pg_get_serial_sequence('hrm_recruit_candidate', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_recruit_candidate_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_recruit_candidate_id_seq))) FROM hrm_recruit_candidate;

SELECT setval(pg_get_serial_sequence('hrm_recruit_channel', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_recruit_channel_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_recruit_channel_id_seq))) FROM hrm_recruit_channel;

SELECT setval(pg_get_serial_sequence('hrm_recruit_interview', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_recruit_interview_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_recruit_interview_id_seq))) FROM hrm_recruit_interview;

SELECT setval(pg_get_serial_sequence('hrm_recruit_post', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_recruit_post_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_recruit_post_id_seq))) FROM hrm_recruit_post;

SELECT setval(pg_get_serial_sequence('hrm_recruit_post_type', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_recruit_post_type_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_recruit_post_type_id_seq))) FROM hrm_recruit_post_type;

SELECT setval(pg_get_serial_sequence('hrm_salary_change_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_change_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_change_record_id_seq))) FROM hrm_salary_change_record;

SELECT setval(pg_get_serial_sequence('hrm_salary_change_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_change_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_change_template_id_seq))) FROM hrm_salary_change_template;

SELECT setval(pg_get_serial_sequence('hrm_salary_config', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_config_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_config_id_seq))) FROM hrm_salary_config;

SELECT setval(pg_get_serial_sequence('hrm_salary_employee_info', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_employee_info_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_employee_info_id_seq))) FROM hrm_salary_employee_info;

SELECT setval(pg_get_serial_sequence('hrm_salary_group', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_group_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_group_id_seq))) FROM hrm_salary_group;

SELECT setval(pg_get_serial_sequence('hrm_salary_month_employee_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_month_employee_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_month_employee_record_id_seq))) FROM hrm_salary_month_employee_record;

SELECT setval(pg_get_serial_sequence('hrm_salary_month_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_month_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_month_record_id_seq))) FROM hrm_salary_month_record;

SELECT setval(pg_get_serial_sequence('hrm_salary_option', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_option_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_option_id_seq))) FROM hrm_salary_option;

SELECT setval(pg_get_serial_sequence('hrm_salary_option_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_option_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_option_template_id_seq))) FROM hrm_salary_option_template;

SELECT setval(pg_get_serial_sequence('hrm_salary_slip', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_slip_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_slip_id_seq))) FROM hrm_salary_slip;

SELECT setval(pg_get_serial_sequence('hrm_salary_slip_send_record', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_slip_send_record_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_slip_send_record_id_seq))) FROM hrm_salary_slip_send_record;

SELECT setval(pg_get_serial_sequence('hrm_salary_slip_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_slip_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_slip_template_id_seq))) FROM hrm_salary_slip_template;

SELECT setval(pg_get_serial_sequence('hrm_salary_tax_rule', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_salary_tax_rule_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_salary_tax_rule_id_seq))) FROM hrm_salary_tax_rule;

SELECT setval(pg_get_serial_sequence('milestones', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM milestones_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM milestones_id_seq))) FROM milestones;

SELECT setval(pg_get_serial_sequence('notifications', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM notifications_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM notifications_id_seq))) FROM notifications;

SELECT setval(pg_get_serial_sequence('oa_announcement', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_announcement_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_announcement_id_seq))) FROM oa_announcement;

SELECT setval(pg_get_serial_sequence('oa_announcement_receiver', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_announcement_receiver_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_announcement_receiver_id_seq))) FROM oa_announcement_receiver;

SELECT setval(pg_get_serial_sequence('oa_attendance', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_attendance_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_attendance_id_seq))) FROM oa_attendance;

SELECT setval(pg_get_serial_sequence('oa_leave_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_leave_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_leave_apply_id_seq))) FROM oa_leave_apply;

SELECT setval(pg_get_serial_sequence('oa_meeting_room', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_meeting_room_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_meeting_room_id_seq))) FROM oa_meeting_room;

SELECT setval(pg_get_serial_sequence('oa_meeting_room_booking', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_meeting_room_booking_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_meeting_room_booking_id_seq))) FROM oa_meeting_room_booking;

SELECT setval(pg_get_serial_sequence('oa_official_doc_receive', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_official_doc_receive_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_official_doc_receive_id_seq))) FROM oa_official_doc_receive;

SELECT setval(pg_get_serial_sequence('oa_official_doc_send', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_official_doc_send_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_official_doc_send_id_seq))) FROM oa_official_doc_send;

SELECT setval(pg_get_serial_sequence('oa_official_doc_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_official_doc_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_official_doc_template_id_seq))) FROM oa_official_doc_template;

SELECT setval(pg_get_serial_sequence('oa_overtime_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_overtime_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_overtime_apply_id_seq))) FROM oa_overtime_apply;

SELECT setval(pg_get_serial_sequence('oa_regular_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_regular_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_regular_apply_id_seq))) FROM oa_regular_apply;

SELECT setval(pg_get_serial_sequence('oa_reimbursement', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_reimbursement_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_reimbursement_id_seq))) FROM oa_reimbursement;

SELECT setval(pg_get_serial_sequence('oa_resign_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_resign_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_resign_apply_id_seq))) FROM oa_resign_apply;

SELECT setval(pg_get_serial_sequence('oa_schedule', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_schedule_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_schedule_id_seq))) FROM oa_schedule;

SELECT setval(pg_get_serial_sequence('oa_schedule_participant', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_schedule_participant_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_schedule_participant_id_seq))) FROM oa_schedule_participant;

SELECT setval(pg_get_serial_sequence('oa_seal', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_seal_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_seal_id_seq))) FROM oa_seal;

SELECT setval(pg_get_serial_sequence('oa_seal_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_seal_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_seal_apply_id_seq))) FROM oa_seal_apply;

SELECT setval(pg_get_serial_sequence('oa_supply_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_supply_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_supply_apply_id_seq))) FROM oa_supply_apply;

SELECT setval(pg_get_serial_sequence('oa_supply_apply_item', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_supply_apply_item_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_supply_apply_item_id_seq))) FROM oa_supply_apply_item;

SELECT setval(pg_get_serial_sequence('oa_supply_item', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_supply_item_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_supply_item_id_seq))) FROM oa_supply_item;

SELECT setval(pg_get_serial_sequence('oa_task', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_task_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_task_id_seq))) FROM oa_task;

SELECT setval(pg_get_serial_sequence('oa_task_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_task_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_task_log_id_seq))) FROM oa_task_log;

SELECT setval(pg_get_serial_sequence('oa_task_receiver', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_task_receiver_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_task_receiver_id_seq))) FROM oa_task_receiver;

SELECT setval(pg_get_serial_sequence('oa_travel_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_travel_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_travel_apply_id_seq))) FROM oa_travel_apply;

SELECT setval(pg_get_serial_sequence('oa_travel_reimbursement', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_travel_reimbursement_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_travel_reimbursement_id_seq))) FROM oa_travel_reimbursement;

SELECT setval(pg_get_serial_sequence('oa_vehicle', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_vehicle_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_vehicle_id_seq))) FROM oa_vehicle;

SELECT setval(pg_get_serial_sequence('oa_vehicle_apply', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_vehicle_apply_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_vehicle_apply_id_seq))) FROM oa_vehicle_apply;

SELECT setval(pg_get_serial_sequence('oa_vehicle_return', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_vehicle_return_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_vehicle_return_id_seq))) FROM oa_vehicle_return;

SELECT setval(pg_get_serial_sequence('oa_work_report', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_work_report_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_work_report_id_seq))) FROM oa_work_report;

SELECT setval(pg_get_serial_sequence('oa_work_report_comment', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_work_report_comment_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_work_report_comment_id_seq))) FROM oa_work_report_comment;

SELECT setval(pg_get_serial_sequence('portal_categories', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_categories_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_categories_id_seq))) FROM portal_categories;

SELECT setval(pg_get_serial_sequence('portal_news', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_news_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_news_id_seq))) FROM portal_news;

SELECT setval(pg_get_serial_sequence('portal_news_comment', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_news_comment_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_news_comment_id_seq))) FROM portal_news_comment;

SELECT setval(pg_get_serial_sequence('portal_news_like', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_news_like_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_news_like_id_seq))) FROM portal_news_like;

SELECT setval(pg_get_serial_sequence('portal_page_views', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_page_views_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_page_views_id_seq))) FROM portal_page_views;

SELECT setval(pg_get_serial_sequence('portal_wiki', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_wiki_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_wiki_id_seq))) FROM portal_wiki;

SELECT setval(pg_get_serial_sequence('recognitions', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM recognitions_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM recognitions_id_seq))) FROM recognitions;

SELECT setval(pg_get_serial_sequence('rhythm_node_follow', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM rhythm_node_follow_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM rhythm_node_follow_id_seq))) FROM rhythm_node_follow;

SELECT setval(pg_get_serial_sequence('rhythm_nodes', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM rhythm_nodes_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM rhythm_nodes_id_seq))) FROM rhythm_nodes;

SELECT setval(pg_get_serial_sequence('sys_dict_data', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_dict_data_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_dict_data_id_seq))) FROM sys_dict_data;

SELECT setval(pg_get_serial_sequence('sys_dict_type', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_dict_type_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_dict_type_id_seq))) FROM sys_dict_type;

SELECT setval(pg_get_serial_sequence('sys_directory_sync_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_directory_sync_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_directory_sync_log_id_seq))) FROM sys_directory_sync_log;

SELECT setval(pg_get_serial_sequence('sys_employee_sync_job', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_employee_sync_job_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_employee_sync_job_id_seq))) FROM sys_employee_sync_job;

SELECT setval(pg_get_serial_sequence('sys_menu', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_menu_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_menu_id_seq))) FROM sys_menu;

SELECT setval(pg_get_serial_sequence('sys_operate_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_operate_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_operate_log_id_seq))) FROM sys_operate_log;

SELECT setval(pg_get_serial_sequence('sys_role', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_role_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_role_id_seq))) FROM sys_role;

SELECT setval(pg_get_serial_sequence('sys_role_menu', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_role_menu_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_role_menu_id_seq))) FROM sys_role_menu;

SELECT setval(pg_get_serial_sequence('sys_user', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_user_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_user_id_seq))) FROM sys_user;

SELECT setval(pg_get_serial_sequence('sys_user_dept', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_user_dept_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_user_dept_id_seq))) FROM sys_user_dept;

SELECT setval(pg_get_serial_sequence('sys_user_role', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM sys_user_role_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM sys_user_role_id_seq))) FROM sys_user_role;

SELECT setval(pg_get_serial_sequence('voices', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM voices_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM voices_id_seq))) FROM voices;

SELECT setval(pg_get_serial_sequence('portal_ai_models', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_ai_models_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_ai_models_id_seq))) FROM portal_ai_models;

SELECT setval(pg_get_serial_sequence('portal_ai_task_setting_versions', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_ai_task_setting_versions_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_ai_task_setting_versions_id_seq))) FROM portal_ai_task_setting_versions;

SELECT setval(pg_get_serial_sequence('portal_api_call_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_api_call_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_api_call_log_id_seq))) FROM portal_api_call_log;

SELECT setval(pg_get_serial_sequence('portal_esports_bookings', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_esports_bookings_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_esports_bookings_id_seq))) FROM portal_esports_bookings;

SELECT setval(pg_get_serial_sequence('portal_esports_approval_steps', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_esports_approval_steps_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_esports_approval_steps_id_seq))) FROM portal_esports_approval_steps;

SELECT setval(pg_get_serial_sequence('portal_esports_booking_logs', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_esports_booking_logs_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_esports_booking_logs_id_seq))) FROM portal_esports_booking_logs;

SELECT setval(pg_get_serial_sequence('portal_events', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_events_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_events_id_seq))) FROM portal_events;

SELECT setval(pg_get_serial_sequence('portal_products', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_products_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_products_id_seq))) FROM portal_products;

SELECT setval(pg_get_serial_sequence('portal_feedback', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_feedback_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_feedback_id_seq))) FROM portal_feedback;

SELECT setval(pg_get_serial_sequence('portal_hr_layout_jobs', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_layout_jobs_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_layout_jobs_id_seq))) FROM portal_hr_layout_jobs;

SELECT setval(pg_get_serial_sequence('portal_hr_layout_attempts', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_layout_attempts_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_layout_attempts_id_seq))) FROM portal_hr_layout_attempts;

SELECT setval(pg_get_serial_sequence('portal_hr_documents', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_documents_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_documents_id_seq))) FROM portal_hr_documents;

SELECT setval(pg_get_serial_sequence('portal_hr_document_aliases', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_document_aliases_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_document_aliases_id_seq))) FROM portal_hr_document_aliases;

SELECT setval(pg_get_serial_sequence('portal_hr_document_entry_bindings', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_document_entry_bindings_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_document_entry_bindings_id_seq))) FROM portal_hr_document_entry_bindings;

SELECT setval(pg_get_serial_sequence('portal_hr_document_versions', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_document_versions_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_document_versions_id_seq))) FROM portal_hr_document_versions;

SELECT setval(pg_get_serial_sequence('portal_hr_document_draft_versions', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_hr_document_draft_versions_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_hr_document_draft_versions_id_seq))) FROM portal_hr_document_draft_versions;

SELECT setval(pg_get_serial_sequence('portal_content_push_jobs', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_content_push_jobs_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_content_push_jobs_id_seq))) FROM portal_content_push_jobs;

SELECT setval(pg_get_serial_sequence('portal_signoff_notify_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_signoff_notify_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_signoff_notify_log_id_seq))) FROM portal_signoff_notify_log;

SELECT setval(pg_get_serial_sequence('portal_brand_docs', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_brand_docs_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_brand_docs_id_seq))) FROM portal_brand_docs;

SELECT setval(pg_get_serial_sequence('portal_brand_doc_versions', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_brand_doc_versions_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_brand_doc_versions_id_seq))) FROM portal_brand_doc_versions;

SELECT setval(pg_get_serial_sequence('portal_contract_entities', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_contract_entities_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_contract_entities_id_seq))) FROM portal_contract_entities;

SELECT setval(pg_get_serial_sequence('portal_resume_interview_processes', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_resume_interview_processes_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_resume_interview_processes_id_seq))) FROM portal_resume_interview_processes;

SELECT setval(pg_get_serial_sequence('portal_resume_interview_rounds', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_resume_interview_rounds_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_resume_interview_rounds_id_seq))) FROM portal_resume_interview_rounds;

SELECT setval(pg_get_serial_sequence('portal_celebration_runs', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_celebration_runs_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_celebration_runs_id_seq))) FROM portal_celebration_runs;

SELECT setval(pg_get_serial_sequence('portal_celebration_push_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_celebration_push_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_celebration_push_log_id_seq))) FROM portal_celebration_push_log;

SELECT setval(pg_get_serial_sequence('portal_wecom_push_log', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_wecom_push_log_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_wecom_push_log_id_seq))) FROM portal_wecom_push_log;

SELECT setval(pg_get_serial_sequence('portal_celebration_cards', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_celebration_cards_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_celebration_cards_id_seq))) FROM portal_celebration_cards;

SELECT setval(pg_get_serial_sequence('portal_resumes', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_resumes_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_resumes_id_seq))) FROM portal_resumes;

SELECT setval(pg_get_serial_sequence('portal_material_products', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_material_products_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_material_products_id_seq))) FROM portal_material_products;

SELECT setval(pg_get_serial_sequence('portal_materials', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_materials_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_materials_id_seq))) FROM portal_materials;

SELECT setval(pg_get_serial_sequence('portal_material_applies', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_material_applies_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_material_applies_id_seq))) FROM portal_material_applies;

SELECT setval(pg_get_serial_sequence('portal_material_apply_items', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_material_apply_items_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_material_apply_items_id_seq))) FROM portal_material_apply_items;

SELECT setval(pg_get_serial_sequence('portal_material_apply_logs', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_material_apply_logs_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_material_apply_logs_id_seq))) FROM portal_material_apply_logs;

SELECT setval(pg_get_serial_sequence('portal_handbook_signoff', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_handbook_signoff_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_handbook_signoff_id_seq))) FROM portal_handbook_signoff;

SELECT setval(pg_get_serial_sequence('portal_training', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_training_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_training_id_seq))) FROM portal_training;

SELECT setval(pg_get_serial_sequence('portal_training_lessons', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_training_lessons_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_training_lessons_id_seq))) FROM portal_training_lessons;

SELECT setval(pg_get_serial_sequence('portal_training_lesson_progress', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_training_lesson_progress_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_training_lesson_progress_id_seq))) FROM portal_training_lesson_progress;

SELECT setval(pg_get_serial_sequence('portal_training_progress', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_training_progress_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_training_progress_id_seq))) FROM portal_training_progress;

SELECT setval(pg_get_serial_sequence('hrm_contract_template', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_contract_template_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_contract_template_id_seq))) FROM hrm_contract_template;

SELECT setval(pg_get_serial_sequence('hrm_employment_document', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_employment_document_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_employment_document_id_seq))) FROM hrm_employment_document;

SELECT setval(pg_get_serial_sequence('hrm_candidate_attachment', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM hrm_candidate_attachment_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM hrm_candidate_attachment_id_seq))) FROM hrm_candidate_attachment;

SELECT setval(pg_get_serial_sequence('portal_workspace_records', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_workspace_records_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_workspace_records_id_seq))) FROM portal_workspace_records;

SELECT setval(pg_get_serial_sequence('portal_workspace_answers', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_workspace_answers_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_workspace_answers_id_seq))) FROM portal_workspace_answers;

SELECT setval(pg_get_serial_sequence('portal_workspace_approval_steps', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM portal_workspace_approval_steps_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM portal_workspace_approval_steps_id_seq))) FROM portal_workspace_approval_steps;

SELECT setval(pg_get_serial_sequence('oa_wecom_approval_link', 'id'), GREATEST(COALESCE(MAX(id), 1), (SELECT last_value FROM oa_wecom_approval_link_id_seq)), (MAX(id) IS NOT NULL OR (SELECT is_called FROM oa_wecom_approval_link_id_seq))) FROM oa_wecom_approval_link;
