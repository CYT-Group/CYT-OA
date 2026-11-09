-- =============================================================================
-- 字典种子数据：从本机 ruoyi-vue-pro 库的 system_dict_type / system_dict_data 导出，
-- 只保留移植过来的 OA / HR 代码引用到的 57 个类型
-- （代码里的 DictTypeConstants 常量与 @DictFormat 注解就是这份清单的依据）。
-- 可重复执行：同 (type) / (dict_type, value) 的行会被覆盖成上游的最新值。
-- =============================================================================

-- common_status（系统状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('common_status', '系统状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('common_status', '开启', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('common_status', '关闭', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_attendance_clock_source（HRM 打卡来源）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_source', 'HRM 打卡来源', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_source', '手机端打卡', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_source', '手工录入', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_attendance_clock_status（HRM 打卡状态）
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

-- hrm_attendance_clock_type（HRM 打卡类型）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_clock_type', 'HRM 打卡类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_type', '上班打卡', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_clock_type', '下班打卡', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_attendance_holiday_type（HRM 考勤节假日类型）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_holiday_type', 'HRM 考勤节假日类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_holiday_type', '上班', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_holiday_type', '休息', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_attendance_leave_type（HRM 请假类型）
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

-- hrm_attendance_yes_no（HRM 考勤是否）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_attendance_yes_no', 'HRM 考勤是否', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_yes_no', '否', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_attendance_yes_no', '是', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_employee_education（HRM 员工最高学历）
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

-- hrm_employee_entry_status（HRM 员工入职状态）
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

-- hrm_employee_id_type（HRM 员工证件类型）
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

-- hrm_employee_status（HRM 员工状态）
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

-- hrm_employee_type（HRM 聘用形式）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_employee_type', 'HRM 聘用形式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_type', '正式', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_type', '非正式', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_insurance_emp_status（HRM 参保状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_emp_status', 'HRM 参保状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_emp_status', '停保', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_emp_status', '正常', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_insurance_month_status（HRM 社保表状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_month_status', 'HRM 社保表状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_month_status', '未归档', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_month_status', '已归档', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_insurance_project_type（HRM 社保项目类型）
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

-- hrm_insurance_scheme_type（HRM 社保方案类型）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_insurance_scheme_type', 'HRM 社保方案类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_scheme_type', '比例', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_insurance_scheme_type', '金额', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_performance_appeal_status（HRM 绩效申诉状态）
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

-- hrm_performance_plan_status（HRM 绩效计划状态）
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

-- hrm_performance_score_calculation（HRM 绩效计分方式）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_score_calculation', 'HRM 绩效计分方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_score_calculation', '加权计算', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_performance_stage_status（HRM 绩效阶段状态）
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

-- hrm_performance_upper_limit_type（HRM 绩效上限类型）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_upper_limit_type', 'HRM 绩效上限类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_upper_limit_type', '不限制', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_upper_limit_type', '固定分', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_performance_yes_no（HRM 绩效是否）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_performance_yes_no', 'HRM 绩效是否', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_yes_no', '否', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_performance_yes_no', '是', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_recruit_candidate_education（HRM 招聘候选人学历）
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

-- hrm_recruit_candidate_status（HRM 招聘候选人状态）
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

-- hrm_recruit_emergency_level（HRM 招聘紧急程度）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_emergency_level', 'HRM 招聘紧急程度', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_emergency_level', '紧急', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_emergency_level', '一般', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_recruit_interview_result（HRM 招聘面试结果）
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

-- hrm_recruit_interview_type（HRM 招聘面试方式）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_interview_type', 'HRM 招聘面试方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '现场面试', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '电话面试', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_interview_type', '视频面试', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_recruit_job_nature（HRM 招聘工作性质）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_job_nature', 'HRM 招聘工作性质', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '全职', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '实习', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_job_nature', '兼职', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_recruit_post_education（HRM 招聘职位学历要求）
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

-- hrm_recruit_post_status（HRM 招聘职位状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_post_status', 'HRM 招聘职位状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_status', '招聘中', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_post_status', '停止招聘', '0', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_recruit_salary_unit（HRM 招聘薪资单位）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_recruit_salary_unit', 'HRM 招聘薪资单位', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_salary_unit', '元/月', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_recruit_salary_unit', '元/年', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_recruit_work_time（HRM 招聘工作经验）
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

-- hrm_salary_change_reason（HRM 薪资调整原因）
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

-- hrm_salary_change_record_status（HRM 薪资调整记录状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_record_status', 'HRM 薪资调整记录状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '待生效', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '生效', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_record_status', '已取消', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_salary_change_type（HRM 薪资档案状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_change_type', 'HRM 薪资档案状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '未定薪', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '已定薪', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_change_type', '已调薪', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_salary_month_status（HRM 工资表状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_month_status', 'HRM 工资表状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '待核算', '5', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '已核算', '11', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_month_status', '历史工资表', '10', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_salary_slip_read_status（HRM 工资条阅读状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_slip_read_status', 'HRM 工资条阅读状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_slip_read_status', '未读', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_slip_read_status', '已读', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_salary_tax_type（HRM 薪资计税类型）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_tax_type', 'HRM 薪资计税类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '工资薪金所得税', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '劳务报酬所得税', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_tax_type', '不计税', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- hrm_salary_yes_no（HRM 薪资是否）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('hrm_salary_yes_no', 'HRM 薪资是否', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_yes_no', '否', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_salary_yes_no', '是', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- infra_boolean_string（Boolean 是否类型）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('infra_boolean_string', 'Boolean 是否类型', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('infra_boolean_string', '是', 'true', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('infra_boolean_string', '否', 'false', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_apply_urgency（申请紧急程度）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_apply_urgency', '申请紧急程度', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '正常', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '重要', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_apply_urgency', '紧急', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_expense_type（费用类型）
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

-- oa_leave_type（请假类型）
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

-- oa_overtime_type（加班类型）
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

-- oa_pay_status（支付状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_pay_status', '支付状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_pay_status', '未支付', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_pay_status', '已支付', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_reimburse_status（报销状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_reimburse_status', '报销状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimburse_status', '未报销', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimburse_status', '已报销', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_reimbursement_payment_method（OA 报销方式）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_reimbursement_payment_method', 'OA 报销方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '银行卡', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '现金', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_reimbursement_payment_method', '其他', '3', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_seal_apply_type（用印类型）
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

-- oa_seal_category（印章分类）
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

-- oa_seal_status（印章状态）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_status', '印章状态', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_status', '在库', '0', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_status', '停用', '1', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_status', '使用中', '2', 3, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_seal_type（印章类型）
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

-- oa_seal_use_mode（用印方式）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_seal_use_mode', '用印方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_mode', '现场用印', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_seal_use_mode', '外借', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_seal_use_status（用印状态）
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

-- oa_supply_category（办公用品分类）
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

-- oa_supply_pickup_method（办公用品领取方式）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('oa_supply_pickup_method', '办公用品领取方式', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('oa_supply_pickup_method', '自取', '1', 0, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- oa_transport_type（交通方式）
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

-- system_user_sex（用户性别）
INSERT INTO sys_dict_type (type, name, status, remark) VALUES ('system_user_sex', '用户性别', 1, '系统内置字典')
  ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('system_user_sex', '男', '1', 1, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;
INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('system_user_sex', '女', '2', 2, 1)
  ON DUPLICATE KEY UPDATE label = VALUES(label), sort_order = VALUES(sort_order), status = 1;

-- 上游没有这些字典类型，需要人工补录：hrm_salary_option_type

INSERT INTO sys_dict_data (dict_type, label, value, sort_order, status) VALUES ('hrm_employee_entry_status', '待完善', '0', 0, 1) ON DUPLICATE KEY UPDATE label = VALUES(label);
