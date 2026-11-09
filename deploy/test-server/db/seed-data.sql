-- CYT-OA 测试服：只含项目自带脚本里的 INSERT/UPDATE/DELETE 数据语句（不含建表、不含本机库业务数据）。
-- 由 deploy/test-server 部署流程生成；表结构来自开发库的 --no-data 快照。
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
-- ===== dict-seed.sql =====
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('common_status', '系统状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('common_status', '开启', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('common_status', '关闭', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_source', 'HRM 打卡来源', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_source', '手机端打卡', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_source', '手工录入', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_status', 'HRM 打卡状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '正常', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '迟到', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '早退', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_status', '缺卡', '3', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_type', 'HRM 打卡类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_type', '上班打卡', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_type', '下班打卡', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_holiday_type', 'HRM 考勤节假日类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_holiday_type', '上班', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_holiday_type', '休息', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_leave_type', 'HRM 请假类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '年假', '年假', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '事假', '事假', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '病假', '病假', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_leave_type', '调休', '调休', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_yes_no', 'HRM 考勤是否', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_yes_no', '否', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_yes_no', '是', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_education', 'HRM 员工最高学历', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '小学', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '初中', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '中专', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '中职', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '技校', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '高中', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '大专', '7', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '本科', '8', 8, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '硕士', '9', 9, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '博士', '10', 10, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '博士后', '11', 11, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_education', '其他', '12', 12, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_entry_status', 'HRM 员工入职状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '在职', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待入职', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待离职', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '离职', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_id_type', 'HRM 员工证件类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '身份证', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '港澳通行证', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '台湾通行证', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '护照', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_id_type', '其他', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_status', 'HRM 员工状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '正式', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '试用', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '实习', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '兼职', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '劳务', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '顾问', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '返聘', '7', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_status', '外包', '8', 8, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_type', 'HRM 聘用形式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_type', '正式', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_type', '非正式', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_emp_status', 'HRM 参保状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_emp_status', '停保', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_emp_status', '正常', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_month_status', 'HRM 社保表状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_month_status', '未归档', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_month_status', '已归档', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_project_type', 'HRM 社保项目类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '养老保险', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '医疗保险', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '失业保险', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '工伤保险', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '生育保险', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '补充大病医疗', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '补充养老', '7', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '残保金', '8', 8, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '社保自定义', '9', 9, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '公积金', '10', 10, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_project_type', '公积金自定义', '11', 11, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_scheme_type', 'HRM 社保方案类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_scheme_type', '比例', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_scheme_type', '金额', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_appeal_status', 'HRM 绩效申诉状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '无申诉', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '待处理', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '已通过', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '已驳回', '3', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_appeal_status', '已取消', '4', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_plan_status', 'HRM 绩效计划状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '草稿', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '未开始', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '进行中', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '已归档', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_plan_status', '终止', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_score_calculation', 'HRM 绩效计分方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_score_calculation', '加权计算', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_stage_status', 'HRM 绩效阶段状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '未开始', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '员工填写', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '目标确认', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '自评', '3', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '他人评分', '4', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '结果审核', '5', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '结果确认', '6', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '申诉确认', '7', 8, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '归档', '8', 9, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '结束', '10', 10, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_stage_status', '执行中', '9', 10, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_upper_limit_type', 'HRM 绩效上限类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_upper_limit_type', '不限制', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_upper_limit_type', '固定分', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_yes_no', 'HRM 绩效是否', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_yes_no', '否', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_yes_no', '是', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_candidate_education', 'HRM 招聘候选人学历', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '小学', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '初中', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '高中', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '大专', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '本科', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '硕士', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_education', '博士', '7', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_candidate_status', 'HRM 招聘候选人状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '新候选人', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '初选通过', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '安排面试', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '面试通过', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '已发 offer', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '待入职', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '已淘汰', '7', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_candidate_status', '已入职', '8', 8, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_emergency_level', 'HRM 招聘紧急程度', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_emergency_level', '紧急', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_emergency_level', '一般', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_interview_result', 'HRM 招聘面试结果', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试未完成', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试通过', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试未通过', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_result', '面试取消', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_interview_type', 'HRM 招聘面试方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '现场面试', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '电话面试', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '视频面试', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_job_nature', 'HRM 招聘工作性质', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '全职', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '实习', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '兼职', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_post_education', 'HRM 招聘职位学历要求', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '不限', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '高中及以上', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '大专及以上', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '本科及以上', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '硕士及以上', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_education', '博士', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_post_status', 'HRM 招聘职位状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_status', '招聘中', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_status', '停止招聘', '0', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_salary_unit', 'HRM 招聘薪资单位', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_salary_unit', '元/月', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_salary_unit', '元/年', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_work_time', 'HRM 招聘工作经验', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '不限', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '1 年以下', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '1-3 年', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '3-5 年', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '5-10 年', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_work_time', '10 年以上', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_reason', 'HRM 薪资调整原因', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '入职定薪', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '入职核定', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '转正', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '晋升', '3', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '调动', '4', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '年中调薪', '5', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '年度调薪', '6', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '特别调薪', '7', 8, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_reason', '其他', '8', 9, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_record_status', 'HRM 薪资调整记录状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '待生效', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '生效', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '已取消', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_type', 'HRM 薪资档案状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '未定薪', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '已定薪', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '已调薪', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_month_status', 'HRM 工资表状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '待核算', '5', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '已核算', '11', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '历史工资表', '10', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_slip_read_status', 'HRM 工资条阅读状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_slip_read_status', '未读', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_slip_read_status', '已读', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_tax_type', 'HRM 薪资计税类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '工资薪金所得税', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '劳务报酬所得税', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '不计税', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_yes_no', 'HRM 薪资是否', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_yes_no', '否', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_yes_no', '是', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('infra_boolean_string', 'Boolean 是否类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('infra_boolean_string', '是', 'true', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('infra_boolean_string', '否', 'false', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_apply_urgency', '申请紧急程度', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '正常', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '重要', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '紧急', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_expense_type', '费用类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '交通费', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '住宿费', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '餐饮费', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '通讯费', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '市内交通', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_expense_type', '其他', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_leave_type', '请假类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '年假', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '事假', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '病假', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '婚假', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '产假及哺乳假', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '陪产假', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_leave_type', '丧假', '7', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_overtime_type', '加班类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '工作日', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '休息日', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '节假日', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_overtime_type', '其他', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_pay_status', '支付状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_pay_status', '未支付', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_pay_status', '已支付', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_reimburse_status', '报销状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimburse_status', '未报销', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimburse_status', '已报销', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_reimbursement_payment_method', 'OA 报销方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '银行卡', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '现金', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '其他', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_apply_type', '用印类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_apply_type', '合同用印', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_apply_type', '协议用印', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_apply_type', '证明用印', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_apply_type', '授权用印', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_apply_type', '其他用印', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_category', '印章分类', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_category', '行政类', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_category', '业务类', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_category', '财务类', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_category', '其他类', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_status', '印章状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_status', '在库', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_status', '停用', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_status', '使用中', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_type', '印章类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_type', '公章', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_type', '合同章', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_type', '财务章', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_type', '法人章', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_type', '发票章', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_type', '部门章', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_use_mode', '用印方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_mode', '现场用印', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_mode', '外借', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_use_status', '用印状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_status', '待处理', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_status', '已完成', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_status', '外借中', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_status', '已归还', '3', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_status', '已逾期', '4', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_supply_category', '办公用品分类', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '文具类', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '打印耗材', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '生活用品', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '电脑办公', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '办公设备', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '电器', '7', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_category', '财务用品', '6', 7, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_supply_pickup_method', '办公用品领取方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_pickup_method', '自取', '1', 0, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_transport_type', '交通方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '飞机', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '高铁/动车', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '火车', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '长途汽车', '4', 4, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '自驾', '5', 5, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_transport_type', '其他', '6', 6, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('system_user_sex', '用户性别', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('system_user_sex', '男', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('system_user_sex', '女', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待完善', '0', 0, 1) ON DUPLICATE KEY UPDATE label = VALUES(label);
-- ===== permission-seed.sql =====
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
    (482, 477, 'statistics', 'button', '', 'oa:work-report:statistics', 0);
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
    (1, 213);
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
    (1, 225);
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
    (1, 237);
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
    (1, 249);
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
    (1, 261);
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
    (1, 273);
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
    (1, 285);
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
    (1, 297);
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
    (1, 309);
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
    (1, 321);
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
    (1, 333);
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
    (1, 345);
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
    (1, 357);
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
    (1, 369);
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
    (1, 381);
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
    (1, 393);
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
    (1, 405);
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
    (1, 417);
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
    (1, 430);
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
    (1, 442);
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
    (1, 454);
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
    (1, 466);
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
    (1, 478);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 479),
    (1, 480),
    (1, 481),
    (1, 482);
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
    (2, 213);
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
    (2, 225);
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
    (2, 237);
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
    (2, 249);
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
    (2, 261);
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
    (2, 273);
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
    (2, 285);
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
    (2, 297);
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
    (2, 309);
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
    (2, 321);
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
    (2, 333);
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
    (2, 345);
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
    (2, 357);
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
    (2, 369);
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
    (2, 381);
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
    (2, 393);
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
    (2, 405);
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
    (2, 417);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (2, 418),
    (2, 419);
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
    (3, 432);
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
    (3, 444);
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
    (3, 456);
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
    (3, 468);
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
    (3, 480);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (3, 481),
    (3, 482);
-- ===== system-seed.sql =====
INSERT INTO sys_role (id, code, name, built_in, remark) VALUES
    (1, 'admin',    '系统管理员', 1, '全量权限'),
    (2, 'hr',       '人事',       1, '员工档案、考勤、字典与通讯录维护'),
    (3, 'ops',      '运营',       1, '大促节奏与公告维护'),
    (4, 'manager',  '主管',       1, '有下属时的团队视角'),
    (5, 'employee', '员工',       1, '仅个人中心');
INSERT INTO sys_menu (id, parent_id, name, type, path, permission, sort_order) VALUES
    (1, 0, '通讯录同步', 'button', '', 'system:directory:sync',   1),
    (2, 0, '组织查询',   'button', '', 'system:dept:query',       2),
    (3, 0, '账号查询',   'button', '', 'system:user:query',       3),
    (4, 0, '账号授权',   'button', '', 'system:user:assign-role', 4),
    (5, 0, '角色查询',   'button', '', 'system:role:query',       5),
    (6, 0, '字典查询',   'button', '', 'system:dict:query',       6),
    (7, 0, '字典维护',   'button', '', 'system:dict:update',      7),
    (8, 0, '他人档案',   'button', '', 'hrm:employee:query',      8);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
    (2, 2), (2, 3), (2, 6), (2, 7), (2, 8),
    (3, 6);
INSERT INTO sys_user (id, wecom_userid, username, name, position, enabled, password_hash) VALUES
    (1, NULL, 'admin', '张伟', '市场部 / P6', 1,
     'pbkdf2:sha256:210000$m3g503RNYWhLM6ctXjG2dw==$UHKQeq5ibcoaOaW1xVYAJjdhzDh1IXfx9VAeG7+WjHE=');
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
-- ===== me-schema.sql =====
INSERT INTO `rhythm_nodes`
  (`id`, `name`, `kind`, `start_date`, `end_date`, `dept_actions`, `note`) VALUES
  (1, '双十一大促', 'sale', '2026-11-01', '2026-11-11',
   '[{"dept":"市场","items":["投放加码"]},{"dept":"运营","items":["各店冲刺"]},{"dept":"客服","items":["排班已出"]},{"dept":"仓储","items":["备货到位"]}]',
   '全年峰值大促，目标 GMV 冲历史新高。客服排班表已出、仓储提前备货、市场投放加码，各店同步冲刺。'),
  (2, '新旗舰键盘 全球首发', 'new', '2026-11-15', '2026-11-15',
   '[{"dept":"市场","items":["落地页 + KOL 寄测"]},{"dept":"设计","items":["主图 / 视频"]},{"dept":"运营","items":["首发折扣"]}]',
   '全球同步首发。市场出落地页并安排 KOL 寄测，设计产出主图与视频物料，运营配置首发折扣。'),
  (3, '公司 8 周年司庆', 'event', '2026-11-20', '2026-11-20',
   '[{"dept":"全员","items":["司庆活动"]},{"dept":"行政","items":["筹备"]}]',
   '公司 8 周年，全员活动。行政统筹筹备，鼓励各团队参与。'),
  (4, '黑五 Black Friday', 'sale', '2026-11-28', '2026-11-28',
   '[{"dept":"海外站","items":["主战场"]},{"dept":"客服","items":["海外时区排班"]},{"dept":"市场","items":["海外投放"]}]',
   '海外站主战场。客服按海外时区排班，市场加大海外投放，承接全年最大海外流量。'),
  (5, '网一 Cyber Monday', 'sale', '2026-12-01', '2026-12-01',
   '[{"dept":"运营","items":["承接黑五流量"]}]',
   '承接黑五流量的第二波。运营做好流量承接与转化优化。'),
  (6, '双十二', 'sale', '2026-12-12', '2026-12-12',
   '[{"dept":"国内","items":["收尾冲量"]}]',
   '国内收尾冲量，全年大促收官。'),
  (7, '冬季限定新色 首发', 'new', '2026-12-20', '2026-12-20',
   '[{"dept":"市场","items":["预热"]},{"dept":"设计","items":["视觉"]}]',
   '冬季限定新色首发。市场提前预热，设计产出视觉物料。')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `kind` = VALUES(`kind`),
  `start_date` = VALUES(`start_date`), `end_date` = VALUES(`end_date`),
  `dept_actions` = VALUES(`dept_actions`), `note` = VALUES(`note`);
UPDATE `notifications` SET `link` = CASE `type`
    WHEN 'new_product'       THEN '/me/rhythm'
    WHEN 'approval_pending'  THEN '/me/apply'
    WHEN 'anniversary'       THEN '/me/profile'
    WHEN 'report_due'        THEN '/me/report?tab=draft'
    ELSE '/me/reminder'
  END
WHERE `link` LIKE '/personal?view=%';
-- ===== local-demo-seed.sql =====
REPLACE INTO sys_dept (id, parent_id, name, path, sort_order, leader_userid, active, created_at, updated_at) VALUES
  (1, 0, '创源通科技', '/1/', 1, 'zhangwei', 1, NOW(), NOW()),
  (100, 1, '市场部', '/1/100/', 10, 'zhangwei', 1, NOW(), NOW());
INSERT INTO sys_user (id, wecom_userid, username, name, position, main_dept_id, enabled, created_at, updated_at) VALUES
  (2, 'lina', 'lina', '李娜', '内容运营', 100, 1, NOW(), NOW()),
  (3, 'wangqiang', 'wangqiang', '王强', '投放', 100, 1, NOW(), NOW()),
  (4, 'zhaomin', 'zhaomin', '赵敏', '设计', 100, 1, NOW(), NOW()),
  (5, 'sunyang', 'sunyang', '孙杨', 'KOL 运营', 100, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name), position = VALUES(position), main_dept_id = VALUES(main_dept_id);
UPDATE sys_user SET wecom_userid = 'zhangwei', name = '张伟', position = '市场部负责人', main_dept_id = 100 WHERE id = 1;
REPLACE INTO hrm_employee
  (id, name, job_number, user_id, mobile, sex, email, birthday, dept_id, leader_employee_id,
   entry_status, status, type, entry_time, probation, regular_time, post_name, post_level, work_city, company_age,
   creator, create_time, updater, update_time, deleted) VALUES
  (1, '张伟', 'CYT0412', 1, '13800000412', 1, 'zhangwei@cyt.test', '1990-11-06', 100, NULL,
   1, 1, 1, '2022-11-20', 1, '2023-02-20', '市场部负责人', 'P6', '上海', 3,
   '1', NOW(), '1', NOW(), 0),
  (2, '李娜', 'CYT0518', 2, '13800000518', 2, 'lina@cyt.test', '1994-03-12', 100, 1,
   1, 1, 1, '2023-05-08', 1, '2023-08-08', '内容运营', 'P5', '上海', 2,
   '1', NOW(), '1', NOW(), 0),
  (3, '王强', 'CYT0623', 3, '13800000623', 1, 'wangqiang@cyt.test', '1992-08-21', 100, 1,
   1, 1, 1, '2023-06-26', 1, '2023-09-26', '投放', 'P5', '深圳', 2,
   '1', NOW(), '1', NOW(), 0),
  (4, '赵敏', 'CYT0744', 4, '13800000744', 2, 'zhaomin@cyt.test', '1996-01-30', 100, 1,
   1, 1, 1, '2024-02-19', 1, '2024-05-19', '设计', 'P4', '上海', 1,
   '1', NOW(), '1', NOW(), 0),
  (5, '孙杨', 'CYT0861', 5, '13800000861', 1, 'sunyang@cyt.test', '1997-06-15', 100, 1,
   1, 1, 1, '2024-09-02', 0, NULL, 'KOL 运营', 'P4', '杭州', 1,
   '1', NOW(), '1', NOW(), 0);
REPLACE INTO oa_work_report
  (id, no, type, status, dept_id, start_time, end_time, title, summary, plan, problem,
   work_items, plan_items, file_urls, remark, creator, create_time, updater, update_time, deleted) VALUES
(9001, 'WR-02-00', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), INTERVAL 6 DAY), '李娜的本周周报', '完成 本周的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9002, 'WR-02-01', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), INTERVAL 6 DAY), '李娜的1 周前周报', '完成 1 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9003, 'WR-02-02', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), INTERVAL 6 DAY), '李娜的2 周前周报', '完成 2 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9004, 'WR-02-03', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), INTERVAL 6 DAY), '李娜的3 周前周报', '完成 3 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9005, 'WR-02-04', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), INTERVAL 6 DAY), '李娜的4 周前周报', '完成 4 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9006, 'WR-02-05', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), INTERVAL 6 DAY), '李娜的5 周前周报', '完成 5 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9007, 'WR-02-06', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), INTERVAL 6 DAY), '李娜的6 周前周报', '完成 6 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9008, 'WR-02-07', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), INTERVAL 6 DAY), '李娜的7 周前周报', '完成 7 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '2', NOW(), '2', NOW(), 0),
(9009, 'WR-03-00', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), INTERVAL 6 DAY), '王强的本周周报', '完成 本周的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9010, 'WR-03-01', 2, 1, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), INTERVAL 6 DAY), '王强的1 周前周报', '完成 1 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9011, 'WR-03-02', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), INTERVAL 6 DAY), '王强的2 周前周报', '完成 2 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9012, 'WR-03-03', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), INTERVAL 6 DAY), '王强的3 周前周报', '完成 3 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9013, 'WR-03-05', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), INTERVAL 6 DAY), '王强的5 周前周报', '完成 5 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9014, 'WR-03-06', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), INTERVAL 6 DAY), '王强的6 周前周报', '完成 6 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9015, 'WR-03-07', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), INTERVAL 6 DAY), '王强的7 周前周报', '完成 7 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '3', NOW(), '3', NOW(), 0),
(9016, 'WR-04-00', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), INTERVAL 6 DAY), '赵敏的本周周报', '完成 本周的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9017, 'WR-04-01', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), INTERVAL 6 DAY), '赵敏的1 周前周报', '完成 1 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9018, 'WR-04-02', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), INTERVAL 6 DAY), '赵敏的2 周前周报', '完成 2 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9019, 'WR-04-03', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), INTERVAL 6 DAY), '赵敏的3 周前周报', '完成 3 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9020, 'WR-04-04', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), INTERVAL 6 DAY), '赵敏的4 周前周报', '完成 4 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9021, 'WR-04-05', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), INTERVAL 6 DAY), '赵敏的5 周前周报', '完成 5 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9022, 'WR-04-06', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), INTERVAL 6 DAY), '赵敏的6 周前周报', '完成 6 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9023, 'WR-04-07', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), INTERVAL 6 DAY), '赵敏的7 周前周报', '完成 7 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '4', NOW(), '4', NOW(), 0),
(9024, 'WR-05-00', 2, 1, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), INTERVAL 6 DAY), '孙杨的本周周报', '完成 本周的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9025, 'WR-05-01', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), INTERVAL 6 DAY), '孙杨的1 周前周报', '完成 1 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9026, 'WR-05-02', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), INTERVAL 6 DAY), '孙杨的2 周前周报', '完成 2 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9027, 'WR-05-04', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), INTERVAL 6 DAY), '孙杨的4 周前周报', '完成 4 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9028, 'WR-05-05', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 35) DAY), INTERVAL 6 DAY), '孙杨的5 周前周报', '完成 5 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9029, 'WR-05-06', 2, 1, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 42) DAY), INTERVAL 6 DAY), '孙杨的6 周前周报', '完成 6 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9030, 'WR-05-07', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 49) DAY), INTERVAL 6 DAY), '孙杨的7 周前周报', '完成 7 周前的投放与落地页迭代，数据达标。', '下周推进新客渠道测试。', '设计排期偏紧，需要支援', '[{"content":"落地页 A/B 上线","progress":100},{"content":"KOL 寄测跟进","progress":60}]', '[{"content":"新客渠道测试"},{"content":"月度投放复盘"}]', NULL, NULL, '5', NOW(), '5', NOW(), 0),
(9031, 'WR-01-00', 2, 1, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 0) DAY), INTERVAL 6 DAY), '张伟的本周周报', '统筹大促物料与渠道排期。', '收口双十一点位，复盘转化。', NULL, '[{"content":"大促落地页","progress":80}]', '[{"content":"大促复盘"}]', NULL, NULL, '1', NOW(), '1', NOW(), 0),
(9032, 'WR-01-01', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 7) DAY), INTERVAL 6 DAY), '张伟的1 周前周报', '统筹大促物料与渠道排期。', '收口双十一点位，复盘转化。', NULL, '[{"content":"大促落地页","progress":80}]', '[{"content":"大促复盘"}]', NULL, NULL, '1', NOW(), '1', NOW(), 0),
(9033, 'WR-01-02', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 14) DAY), INTERVAL 6 DAY), '张伟的2 周前周报', '统筹大促物料与渠道排期。', '收口双十一点位，复盘转化。', NULL, '[{"content":"大促落地页","progress":80}]', '[{"content":"大促复盘"}]', NULL, NULL, '1', NOW(), '1', NOW(), 0),
(9034, 'WR-01-03', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 21) DAY), INTERVAL 6 DAY), '张伟的3 周前周报', '统筹大促物料与渠道排期。', '收口双十一点位，复盘转化。', NULL, '[{"content":"大促落地页","progress":80}]', '[{"content":"大促复盘"}]', NULL, NULL, '1', NOW(), '1', NOW(), 0),
(9035, 'WR-01-04', 2, 2, 100, DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL (WEEKDAY(CURDATE()) + 28) DAY), INTERVAL 6 DAY), '张伟的4 周前周报', '统筹大促物料与渠道排期。', '收口双十一点位，复盘转化。', NULL, '[{"content":"大促落地页","progress":80}]', '[{"content":"大促复盘"}]', NULL, NULL, '1', NOW(), '1', NOW(), 0),
(9036, 'WR-01-M', 3, 1, 100, DATE_FORMAT(CURDATE(), '%Y-%m-01'), LAST_DAY(CURDATE()), '本月部门总结', '市场部整体投放 ROI 提升，新品落地页全部上线。', '下月重点做转化率。', '多语言翻译排期紧', NULL, NULL, NULL, NULL, '1', NOW(), '1', NOW(), 0);
SET FOREIGN_KEY_CHECKS=1;
