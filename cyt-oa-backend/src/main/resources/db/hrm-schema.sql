-- =============================================================================
-- HRM 模块建表脚本（MySQL 8 / InnoDB / utf8mb4_unicode_ci）
--
-- 来源：ruoyi-vue-pro 的 50 个 MyBatis-Plus DO 类（module.hrm.dal.dataobject.**）逐个反推。
--       上游 sql/ 目录里从来没有 hrm_* 的 CREATE TABLE，本机 ruoyi-vue-pro 库也没有建过这些表，
--       所以 HRM 是「有代码没有表」的状态，本文件是第一份可执行 DDL。
--
-- 规则（与 oa-ddl.sql 保持一致）：
--   - 表名取 @TableName，未标注的按类名 snake_case 推导；
--   - 列名取 @TableField 值，否则字段名 snake_case；
--   - @TableField(typeHandler=...) 标注的 List/Map/嵌套对象列用 text 存 JSON，
--     与上游 system_* 表的做法一致（menu_ids、template_params 等同样用 text）；
--   - 类型映射：Integer->int、Long->bigint、Boolean->bit(1)、String->varchar(按语义 64/128/255/512，正文类 text)、
--     LocalDateTime->datetime、LocalTime->time、BigDecimal->金额 decimal(12,2)/比例权重 decimal(10,4)/经纬度 decimal(10,6)、
--     业务枚举->int（yudao 以整型 code 存储）；
--   - 列 COMMENT 取该字段的中文 javadoc；
--   - 索引来自对每个 DO 在 Service/Mapper 里真实出现过的 wrapper 条件列（eq/in/ge/le/between/orderBy），未臆造。
--   - 审计列沿用上游 BaseDO：creator / create_time / updater / update_time / deleted，不含 tenant_id
--     （本系统单组织，账号与部门由企微通讯录同步提供）。
--
-- 全部为 CREATE TABLE IF NOT EXISTS，可重复执行。
-- =============================================================================

SET NAMES utf8mb4;

-- 企微花名册日期同步基线；已有库也可单独执行 wecom-hr-sync-schema.sql。
CREATE TABLE IF NOT EXISTS hrm_employee_wecom_sync (
    employee_id BIGINT NOT NULL PRIMARY KEY,
    birthday DATETIME NULL,
    entry_time DATETIME NULL,
    synced_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微花名册日期同步基线';

-- ----------------------------
-- 1. HRM 考勤组 hrm_attendance_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_attendance_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤组编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '考勤组名称',
  `dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '适用部门编号',
  `employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '适用员工编号',
  `open_wifi_card` bit(1) NULL DEFAULT NULL COMMENT '是否启用 WiFi 打卡',
  `open_point_card` bit(1) NULL DEFAULT NULL COMMENT '是否启用定位打卡',
  `shifts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '班次配置',
  `rest` bit(1) NULL DEFAULT NULL COMMENT '是否法定节假日休息',
  `special_dates` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '特殊日期设置数组',
  `points` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '打卡地点配置',
  `wifis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'WiFi 打卡配置',
  `deduct_rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扣款规则',
  `default_status` bit(1) NULL DEFAULT NULL COMMENT '是否默认考勤组',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_default_status`(`default_status` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 考勤组';

-- ----------------------------
-- 2. HRM 考勤节假日 hrm_attendance_holiday
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_attendance_holiday` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节假日编号',
  `date` datetime NULL DEFAULT NULL COMMENT '日期',
  `type` int NULL DEFAULT NULL COMMENT '日期类型 枚举  字典',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_date`(`date` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 考勤节假日';

-- ----------------------------
-- 3. HRM 考勤打卡记录 hrm_attendance_clock
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_attendance_clock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '打卡记录编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `clock_time` datetime NULL DEFAULT NULL COMMENT '打卡时间',
  `type` int NULL DEFAULT NULL COMMENT '打卡类型 枚举  字典',
  `attendance_time` datetime NULL DEFAULT NULL COMMENT '应打卡时间',
  `source_type` int NULL DEFAULT NULL COMMENT '打卡来源 枚举  字典',
  `status` int NULL DEFAULT NULL COMMENT '打卡状态 枚举  字典',
  `stage` int NULL DEFAULT NULL COMMENT '打卡阶段 枚举',
  `address` varchar(512) NULL DEFAULT NULL COMMENT '打卡地址',
  `longitude` decimal(10,6) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) NULL DEFAULT NULL COMMENT '纬度',
  `ssid` varchar(128) NULL DEFAULT NULL COMMENT 'WiFi 名称',
  `mac` varchar(512) NULL DEFAULT NULL COMMENT 'WiFi MAC 地址',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_clock_time`(`clock_time` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 考勤打卡记录';

-- ----------------------------
-- 4. HRM 考勤请假 hrm_attendance_leave
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_attendance_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '请假记录编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `type` varchar(255) NULL DEFAULT NULL COMMENT '请假类型 字典',
  `start_time` datetime NULL DEFAULT NULL COMMENT '请假开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '请假结束时间',
  `day` decimal(10,4) NULL DEFAULT NULL COMMENT '请假时长',
  `reason` varchar(255) NULL DEFAULT NULL COMMENT '请假理由',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `approval_status` int NULL DEFAULT NULL COMMENT '审批状态 枚举',
  `process_instance_id` varchar(64) NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `approval_time` datetime NULL DEFAULT NULL COMMENT '审批完成时间',
  `approval_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批或取消原因',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_approval_status`(`approval_status` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 考勤请假';

-- ----------------------------
-- 5. HRM 通用业务配置 hrm_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `type` int NULL DEFAULT NULL COMMENT '配置类型 枚举',
  `value` varchar(255) NULL DEFAULT NULL COMMENT '配置值',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 通用业务配置';

-- ----------------------------
-- 6. HRM 员工异动记录 hrm_employee_change_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '异动记录编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `type` int NULL DEFAULT NULL COMMENT '异动类型 枚举',
  `reason` int NULL DEFAULT NULL COMMENT '异动原因 枚举',
  `old_dept_id` bigint NULL DEFAULT NULL COMMENT '原部门编号',
  `new_dept_id` bigint NULL DEFAULT NULL COMMENT '新部门编号',
  `old_post_name` varchar(255) NULL DEFAULT NULL COMMENT '原岗位',
  `new_post_name` varchar(255) NULL DEFAULT NULL COMMENT '新岗位',
  `old_post_level` varchar(255) NULL DEFAULT NULL COMMENT '原职级',
  `new_post_level` varchar(255) NULL DEFAULT NULL COMMENT '新职级',
  `old_work_address` varchar(255) NULL DEFAULT NULL COMMENT '原工作地点',
  `new_work_address` varchar(255) NULL DEFAULT NULL COMMENT '新工作地点',
  `old_leader_employee_id` bigint NULL DEFAULT NULL COMMENT '原直属上级员工编号',
  `new_leader_employee_id` bigint NULL DEFAULT NULL COMMENT '新直属上级员工编号',
  `probation` int NULL DEFAULT NULL COMMENT '试用期，单位：月',
  `effect_time` datetime NULL DEFAULT NULL COMMENT '生效日期',
  `applied_time` datetime NULL DEFAULT NULL COMMENT '实际生效时间',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_effect_time`(`effect_time` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工异动记录';

-- ----------------------------
-- 7. HRM 员工合同 hrm_employee_contract
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `no` varchar(64) NULL DEFAULT NULL COMMENT '合同编号',
  `type` int NULL DEFAULT NULL COMMENT '合同类型 枚举',
  `start_time` datetime NULL DEFAULT NULL COMMENT '合同开始日期',
  `end_time` datetime NULL DEFAULT NULL COMMENT '合同结束日期',
  `term` int NULL DEFAULT NULL COMMENT '合同期限，单位：年',
  `status` int NULL DEFAULT NULL COMMENT '合同状态 枚举',
  `sign_company` varchar(255) NULL DEFAULT NULL COMMENT '签约公司',
  `sign_time` datetime NULL DEFAULT NULL COMMENT '合同签订日期',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `expire_remind` bit(1) NULL DEFAULT NULL COMMENT '是否到期提醒',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址数组',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_end_time`(`end_time` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_expire_remind`(`expire_remind` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工合同';

-- ----------------------------
-- 8. HRM 员工离职信息 hrm_employee_quit_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_quit_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '离职信息编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `plan_quit_time` datetime NULL DEFAULT NULL COMMENT '计划离职时间',
  `apply_quit_time` datetime NULL DEFAULT NULL COMMENT '申请离职时间',
  `salary_settlement_time` datetime NULL DEFAULT NULL COMMENT '薪资结算时间',
  `type` int NULL DEFAULT NULL COMMENT '离职类型 枚举',
  `reason` int NULL DEFAULT NULL COMMENT '离职原因 枚举',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `old_employee_status` int NULL DEFAULT NULL COMMENT '离职前员工状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plan_quit_time`(`plan_quit_time` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工离职信息';

-- ----------------------------
-- 9. HRM 员工工资卡 hrm_employee_salary_card
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_salary_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `bank_card_number` varchar(64) NULL DEFAULT NULL COMMENT '银行卡号',
  `bank_area_id` int NULL DEFAULT NULL COMMENT '开户地区编号',
  `bank_name` varchar(128) NULL DEFAULT NULL COMMENT '银行名称',
  `bank_branch_name` varchar(128) NULL DEFAULT NULL COMMENT '开户支行名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工工资卡';

-- ----------------------------
-- 10. HRM 员工证书 hrm_employee_certificate
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '证书编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '证书名称',
  `level` varchar(255) NULL DEFAULT NULL COMMENT '证书级别',
  `no` varchar(64) NULL DEFAULT NULL COMMENT '证书编号',
  `start_time` datetime NULL DEFAULT NULL COMMENT '有效开始日期',
  `end_time` datetime NULL DEFAULT NULL COMMENT '有效结束日期',
  `issuing_authority` varchar(255) NULL DEFAULT NULL COMMENT '发证机构',
  `issuing_time` datetime NULL DEFAULT NULL COMMENT '发证日期',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工证书';

-- ----------------------------
-- 11. HRM 员工教育经历 hrm_employee_education_experience
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_education_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教育经历编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `education` int NULL DEFAULT NULL COMMENT '学历 枚举  字典',
  `graduate_school` varchar(255) NULL DEFAULT NULL COMMENT '毕业院校',
  `major` varchar(255) NULL DEFAULT NULL COMMENT '专业',
  `admission_time` datetime NULL DEFAULT NULL COMMENT '入学日期',
  `graduation_time` datetime NULL DEFAULT NULL COMMENT '毕业日期',
  `teaching_methods` int NULL DEFAULT NULL COMMENT '教学方式 枚举',
  `first_degree` bit(1) NULL DEFAULT NULL COMMENT '是否第一学历',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工教育经历';

-- ----------------------------
-- 12. HRM 员工培训经历 hrm_employee_training_experience
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_training_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '培训经历编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `course` varchar(255) NULL DEFAULT NULL COMMENT '培训课程',
  `organization_name` varchar(128) NULL DEFAULT NULL COMMENT '培训机构名称',
  `start_time` datetime NULL DEFAULT NULL COMMENT '培训开始日期',
  `end_time` datetime NULL DEFAULT NULL COMMENT '培训结束日期',
  `duration` varchar(255) NULL DEFAULT NULL COMMENT '培训时长',
  `result` varchar(255) NULL DEFAULT NULL COMMENT '培训成绩',
  `certificate_name` varchar(128) NULL DEFAULT NULL COMMENT '培训证书名称',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工培训经历';

-- ----------------------------
-- 13. HRM 员工工作经历 hrm_employee_work_experience
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_work_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作经历编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `work_unit` varchar(255) NULL DEFAULT NULL COMMENT '工作单位',
  `post_name` varchar(255) NULL DEFAULT NULL COMMENT '职务',
  `start_time` datetime NULL DEFAULT NULL COMMENT '工作开始日期',
  `end_time` datetime NULL DEFAULT NULL COMMENT '工作结束日期',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '离职原因',
  `witness_name` varchar(255) NULL DEFAULT NULL COMMENT '证明人',
  `witness_phone` varchar(32) NULL DEFAULT NULL COMMENT '证明人手机号',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作备注',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工工作经历';

-- ----------------------------
-- 14. HRM 员工联系人 hrm_employee_contact
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '联系人名称',
  `relation` varchar(255) NULL DEFAULT NULL COMMENT '与员工的关系',
  `phone` varchar(32) NULL DEFAULT NULL COMMENT '联系人电话',
  `work_unit` varchar(255) NULL DEFAULT NULL COMMENT '联系人工作单位',
  `post_name` varchar(255) NULL DEFAULT NULL COMMENT '联系人职务',
  `address` varchar(512) NULL DEFAULT NULL COMMENT '联系人地址',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工联系人';

-- ----------------------------
-- 15. HRM 员工档案 hrm_employee
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '员工姓名',
  `job_number` varchar(64) NULL DEFAULT NULL COMMENT '工号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '后台用户编号 允许为空，用于待入职、离职历史等没有登录账号的员工档案',
  `mobile` varchar(32) NULL DEFAULT NULL COMMENT '手机号码',
  `country` varchar(255) NULL DEFAULT NULL COMMENT '国家或地区',
  `nation` varchar(255) NULL DEFAULT NULL COMMENT '民族',
  `id_type` int NULL DEFAULT NULL COMMENT '证件类型 枚举',
  `id_number` varchar(64) NULL DEFAULT NULL COMMENT '证件号码',
  `sex` int NULL DEFAULT NULL COMMENT '性别 枚举',
  `email` varchar(128) NULL DEFAULT NULL COMMENT '邮箱',
  `native_place` varchar(255) NULL DEFAULT NULL COMMENT '籍贯',
  `birthday` datetime NULL DEFAULT NULL COMMENT '出生时间',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `address` varchar(512) NULL DEFAULT NULL COMMENT '户籍地址',
  `highest_education` int NULL DEFAULT NULL COMMENT '最高学历 枚举  字典',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门编号',
  `leader_employee_id` bigint NULL DEFAULT NULL COMMENT '直属上级员工编号',
  `entry_status` int NULL DEFAULT NULL COMMENT '入职状态 枚举  字典',
  `status` int NULL DEFAULT NULL COMMENT '员工状态 枚举  字典',
  `type` int NULL DEFAULT NULL COMMENT '聘用形式 枚举  字典',
  `entry_time` datetime NULL DEFAULT NULL COMMENT '入职时间',
  `probation` int NULL DEFAULT NULL COMMENT '试用期，单位：月 0 表示无试用期',
  `regular_time` datetime NULL DEFAULT NULL COMMENT '转正时间',
  `leave_time` datetime NULL DEFAULT NULL COMMENT '离职时间',
  `post_name` varchar(128) NULL DEFAULT NULL COMMENT '职位名称',
  `post_level` varchar(255) NULL DEFAULT NULL COMMENT '岗位职级',
  `work_city` varchar(255) NULL DEFAULT NULL COMMENT '工作城市',
  `work_address` varchar(512) NULL DEFAULT NULL COMMENT '工作地址',
  `work_detail_address` varchar(512) NULL DEFAULT NULL COMMENT '工作详细地址',
  `channel_id` bigint NULL DEFAULT NULL COMMENT '招聘渠道编号',
  `company_age_start_time` datetime NULL DEFAULT NULL COMMENT '司龄开始时间',
  `company_age` int NULL DEFAULT NULL COMMENT '司龄，单位：年',
  `candidate_id` bigint NULL DEFAULT NULL COMMENT '招聘候选人编号',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `active_user_id` bigint GENERATED ALWAYS AS (CASE WHEN `deleted` = 0 THEN `user_id` ELSE NULL END) STORED,
  UNIQUE KEY `uk_hrm_employee_active_user` (`active_user_id`),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_entry_status`(`entry_status` ASC) USING BTREE,
  INDEX `idx_job_number`(`job_number` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_entry_time`(`entry_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工档案';

-- ----------------------------
-- 16. HRM 员工材料附件 hrm_employee_file
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `type` int NULL DEFAULT NULL COMMENT '附件类型 枚举',
  `url` varchar(512) NULL DEFAULT NULL COMMENT '附件地址',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工材料附件';

-- ----------------------------
-- 17. HRM 员工个人备忘 hrm_employee_personal_note
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_employee_personal_note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NULL DEFAULT NULL COMMENT '所属员工编号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备忘内容',
  `reminder_time` datetime NULL DEFAULT NULL COMMENT '提醒时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_reminder_time`(`reminder_time` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工个人备忘';

-- ----------------------------
-- 18. HRM 社保方案 hrm_insurance_scheme
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_insurance_scheme` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '方案名称',
  `area_id` int NULL DEFAULT NULL COMMENT '参保地区编号',
  `household_type` varchar(255) NULL DEFAULT NULL COMMENT '户籍类型',
  `type` int NULL DEFAULT NULL COMMENT '方案类型 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_area_id`(`area_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 社保方案';

-- ----------------------------
-- 19. HRM 社保方案项目 hrm_insurance_scheme_project
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_insurance_scheme_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `scheme_id` bigint NULL DEFAULT NULL COMMENT '社保方案编号',
  `type` int NULL DEFAULT NULL COMMENT '项目类型 枚举',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '项目名称',
  `base_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '缴纳基数',
  `corporate_rate` decimal(10,4) NULL DEFAULT NULL COMMENT '公司缴纳比例',
  `personal_rate` decimal(10,4) NULL DEFAULT NULL COMMENT '个人缴纳比例',
  `corporate_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司缴纳金额',
  `personal_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人缴纳金额',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scheme_id`(`scheme_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 社保方案项目';

-- ----------------------------
-- 20. HRM 员工参保信息 hrm_insurance_employee_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_insurance_employee_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `first_social_security` bit(1) NULL DEFAULT NULL COMMENT '是否首次参保',
  `first_accumulation_fund` bit(1) NULL DEFAULT NULL COMMENT '是否首次缴纳公积金',
  `social_security_number` varchar(64) NULL DEFAULT NULL COMMENT '社保账号',
  `accumulation_fund_number` varchar(64) NULL DEFAULT NULL COMMENT '公积金账号',
  `social_security_start_month` datetime NULL DEFAULT NULL COMMENT '社保起缴月份',
  `scheme_id` bigint NULL DEFAULT NULL COMMENT '社保方案编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_scheme_id`(`scheme_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工参保信息';

-- ----------------------------
-- 21. HRM 员工月度社保记录 hrm_insurance_month_employee_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_insurance_month_employee_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `month_record_id` bigint NULL DEFAULT NULL COMMENT '月度社保表编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `scheme_id` bigint NULL DEFAULT NULL COMMENT '社保方案编号',
  `year` int NULL DEFAULT NULL COMMENT '年份',
  `month` int NULL DEFAULT NULL COMMENT '月份',
  `personal_insurance_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司公积金金额',
  `status` int NULL DEFAULT NULL COMMENT '参保状态 枚举',
  `projects` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参保项目快照',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_month_record_id`(`month_record_id` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_scheme_id`(`scheme_id` ASC) USING BTREE,
  INDEX `idx_year`(`year` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工月度社保记录';

-- ----------------------------
-- 22. HRM 月度社保表 hrm_insurance_month_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_insurance_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) NULL DEFAULT NULL COMMENT '标题',
  `year` int NULL DEFAULT NULL COMMENT '年份',
  `month` int NULL DEFAULT NULL COMMENT '月份',
  `insured_employee_count` int NULL DEFAULT NULL COMMENT '参保人数',
  `stopped_employee_count` int NULL DEFAULT NULL COMMENT '停保人数',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `personal_insurance_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司公积金金额',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_year`(`year` ASC) USING BTREE,
  INDEX `idx_month`(`month` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 月度社保表';

-- ----------------------------
-- 23. HRM 员工绩效考核动作记录 hrm_performance_assessment_action_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动作记录编号',
  `assessment_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核编号',
  `stage_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核阶段编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '操作员工编号',
  `type` int NULL DEFAULT NULL COMMENT '动作类型 枚举',
  `title` varchar(255) NULL DEFAULT NULL COMMENT '动作标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '动作内容',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `status` int NULL DEFAULT NULL COMMENT '动作完成后的阶段状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_id`(`assessment_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核动作记录';

-- ----------------------------
-- 24. HRM 员工绩效考核申诉记录 hrm_performance_assessment_appeal_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_appeal_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申诉记录编号',
  `assessment_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核编号',
  `stage_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核阶段编号',
  `status` int NULL DEFAULT NULL COMMENT '处理状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_id`(`assessment_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核申诉记录';

-- ----------------------------
-- 25. HRM 员工绩效考核 hrm_performance_assessment
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工绩效考核编号',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '绩效计划编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `status` int NULL DEFAULT NULL COMMENT '考核状态 枚举',
  `process_status` int NULL DEFAULT NULL COMMENT '流程处理状态 枚举',
  `stage_type` int NULL DEFAULT NULL COMMENT '当前业务阶段类型 枚举',
  `stage_sort` int NULL DEFAULT NULL COMMENT '当前阶段排序',
  `score` decimal(12,2) NULL DEFAULT NULL COMMENT '综合得分',
  `result_level` varchar(255) NULL DEFAULT NULL COMMENT '结果等级',
  `coefficient` decimal(10,4) NULL DEFAULT NULL COMMENT '绩效系数',
  `target_confirmation_result` int NULL DEFAULT NULL COMMENT '目标确认结果 枚举',
  `target_confirmation_comment` varchar(255) NULL DEFAULT NULL COMMENT '目标确认意见',
  `target_confirmation_time` datetime NULL DEFAULT NULL COMMENT '目标确认时间',
  `self_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自评说明',
  `reviewer_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '他评说明',
  `result_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结果说明',
  `result_confirmation_time` datetime NULL DEFAULT NULL COMMENT '结果确认时间',
  `result_audit_status` int NULL DEFAULT NULL COMMENT '结果审核状态 枚举',
  `result_audit_time` datetime NULL DEFAULT NULL COMMENT '结果审核时间',
  `result_audit_reason` varchar(255) NULL DEFAULT NULL COMMENT '结果审核意见',
  `appeal_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申诉原因',
  `appeal_file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申诉附件地址列表',
  `appeal_submit_time` datetime NULL DEFAULT NULL COMMENT '申诉提交时间',
  `appeal_status` int NULL DEFAULT NULL COMMENT '申诉状态 枚举',
  `appeal_time` datetime NULL DEFAULT NULL COMMENT '申诉完成时间',
  `appeal_comment` varchar(255) NULL DEFAULT NULL COMMENT '申诉处理意见',
  `archive_time` datetime NULL DEFAULT NULL COMMENT '归档时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_archive_time`(`archive_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核';

-- ----------------------------
-- 26. HRM 员工绩效考核维度 hrm_performance_assessment_dimension
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_dimension` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核维度编号',
  `assessment_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '维度名称',
  `quota_type` int NULL DEFAULT NULL COMMENT '指标类型 枚举',
  `weight` decimal(10,4) NULL DEFAULT NULL COMMENT '维度权重',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `allow_edit` bit(1) NULL DEFAULT NULL COMMENT '是否允许员工编辑指标',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_id`(`assessment_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核维度';

-- ----------------------------
-- 27. HRM 员工绩效考核指标 hrm_performance_assessment_quota
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核指标编号',
  `assessment_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核编号',
  `dimension_id` bigint NULL DEFAULT NULL COMMENT '考核维度编号',
  `preset` bit(1) NULL DEFAULT NULL COMMENT '是否预置指标',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '指标名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '指标说明',
  `standard` varchar(255) NULL DEFAULT NULL COMMENT '评分标准',
  `weight` decimal(10,4) NULL DEFAULT NULL COMMENT '指标权重',
  `score_type` int NULL DEFAULT NULL COMMENT '评分类型 枚举',
  `target_value` varchar(255) NULL DEFAULT NULL COMMENT '目标值',
  `actual_value` varchar(255) NULL DEFAULT NULL COMMENT '实际值',
  `self_score` decimal(12,2) NULL DEFAULT NULL COMMENT '自评汇总分',
  `reviewer_score` decimal(12,2) NULL DEFAULT NULL COMMENT '他评汇总分',
  `final_score` decimal(12,2) NULL DEFAULT NULL COMMENT '最终指标得分',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_id`(`assessment_id` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核指标';

-- ----------------------------
-- 28. HRM 员工绩效考核指标评分 hrm_performance_assessment_quota_score
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_quota_score` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核指标评分编号',
  `assessment_stage_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核阶段编号',
  `assessment_quota_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核指标编号',
  `score` decimal(12,2) NULL DEFAULT NULL COMMENT '指标得分',
  `comment` varchar(255) NULL DEFAULT NULL COMMENT '指标评语',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_stage_id`(`assessment_stage_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核指标评分';

-- ----------------------------
-- 29. HRM 员工绩效考核阶段 hrm_performance_assessment_stage
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核阶段编号',
  `assessment_id` bigint NULL DEFAULT NULL COMMENT '员工绩效考核编号',
  `type` int NULL DEFAULT NULL COMMENT '业务阶段类型 枚举',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '阶段名称',
  `handler_employee_id` bigint NULL DEFAULT NULL COMMENT '实际处理员工编号',
  `rater_type` int NULL DEFAULT NULL COMMENT '评分人类型 枚举',
  `weight` decimal(10,4) NULL DEFAULT NULL COMMENT '评分权重快照',
  `scoring_type` int NULL DEFAULT NULL COMMENT '评分方式快照 枚举',
  `visible_content` int NULL DEFAULT NULL COMMENT '评分内容可见范围快照 枚举',
  `required_setting` bit(1) NULL DEFAULT NULL COMMENT '评语是否必填',
  `reject_authority` bit(1) NULL DEFAULT NULL COMMENT '是否允许驳回',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `status` int NULL DEFAULT NULL COMMENT '阶段处理状态 枚举',
  `score` decimal(12,2) NULL DEFAULT NULL COMMENT '阶段汇总得分',
  `result_level` varchar(255) NULL DEFAULT NULL COMMENT '阶段结果等级',
  `comment` varchar(255) NULL DEFAULT NULL COMMENT '阶段意见',
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最近一次驳回原因',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  `deadline_time` datetime NULL DEFAULT NULL COMMENT '截止时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assessment_id`(`assessment_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工绩效考核阶段';

-- ----------------------------
-- 30. HRM 绩效考核模板 hrm_performance_assessment_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_assessment_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绩效考核模板编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '模板名称',
  `illustrate` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模板说明',
  `score_calculation` int NULL DEFAULT NULL COMMENT '计分方式 枚举',
  `upper_limit_type` int NULL DEFAULT NULL COMMENT '分数上限类型 枚举',
  `upper_limit_score` decimal(12,2) NULL DEFAULT NULL COMMENT '分数上限',
  `dimension_count` int NULL DEFAULT NULL COMMENT '维度数量',
  `quota_count` int NULL DEFAULT NULL COMMENT '指标数量',
  `dimensions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '考核维度配置',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 绩效考核模板';

-- ----------------------------
-- 31. HRM 绩效结果模板 hrm_performance_result_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_result_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绩效结果模板编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '模板名称',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `levels` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结果等级配置',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 绩效结果模板';

-- ----------------------------
-- 32. HRM 绩效考核计划 hrm_performance_plan
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_performance_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划编号',
  `status` int NULL DEFAULT NULL COMMENT '计划状态 枚举',
  `stage_type` int NULL DEFAULT NULL COMMENT '当前业务阶段类型 枚举',
  `operation_type` int NULL DEFAULT NULL COMMENT '当前可执行操作类型 枚举',
  `terminate_time` datetime NULL DEFAULT NULL COMMENT '终止时间',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '计划名称',
  `cycle_type` int NULL DEFAULT NULL COMMENT '考核周期类型 枚举',
  `cycle` varchar(255) NULL DEFAULT NULL COMMENT '考核周期',
  `quarter` int NULL DEFAULT NULL COMMENT '季度 枚举',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '考核说明',
  `scopes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '考评范围列表',
  `assessment_template_id` bigint NULL DEFAULT NULL COMMENT '考核模板编号',
  `assessment_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '考核配置快照',
  `quota_setting_type` int NULL DEFAULT NULL COMMENT '指标制定类型 枚举',
  `target_confirmation` bit(1) NULL DEFAULT NULL COMMENT '是否开启目标确认',
  `target_confirmation_stage` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '目标确认节点',
  `review_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评分阶段列表',
  `result_audit` bit(1) NULL DEFAULT NULL COMMENT '是否开启结果审核',
  `result_audit_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结果审核节点列表',
  `result_confirmation` bit(1) NULL DEFAULT NULL COMMENT '是否开启结果确认',
  `appeal_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申诉处理节点列表',
  `appeal_timeout_days` int NULL DEFAULT NULL COMMENT '申诉超期天数',
  `appeal_timeout_action` int NULL DEFAULT NULL COMMENT '申诉超期处理动作 枚举',
  `result_template_id` bigint NULL DEFAULT NULL COMMENT '结果模板编号',
  `result_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结果配置快照',
  `sync_to_salary` bit(1) NULL DEFAULT NULL COMMENT '是否同步到薪资',
  `paid_for_month` varchar(255) NULL DEFAULT NULL COMMENT '计薪月份',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_paid_for_month`(`paid_for_month` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 绩效考核计划';

-- ----------------------------
-- 33. HRM 招聘候选人 hrm_recruit_candidate
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_recruit_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '候选人编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '候选人姓名',
  `mobile` varchar(32) NULL DEFAULT NULL COMMENT '手机号码',
  `sex` int NULL DEFAULT NULL COMMENT '性别 枚举',
  `age` int NULL DEFAULT NULL COMMENT '年龄',
  `email` varchar(128) NULL DEFAULT NULL COMMENT '邮箱',
  `post_id` bigint NULL DEFAULT NULL COMMENT '应聘职位编号',
  `stage_number` int NULL DEFAULT NULL COMMENT '面试轮次',
  `work_time` int NULL DEFAULT NULL COMMENT '工作年限，单位：年',
  `education` int NULL DEFAULT NULL COMMENT '学历 字典',
  `graduate_school` varchar(255) NULL DEFAULT NULL COMMENT '毕业院校',
  `latest_work_place` varchar(255) NULL DEFAULT NULL COMMENT '最近工作单位',
  `channel_id` bigint NULL DEFAULT NULL COMMENT '招聘渠道编号',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `status` int NULL DEFAULT NULL COMMENT '候选人状态 枚举  字典',
  `eliminate` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '淘汰原因',
  `status_update_time` datetime NULL DEFAULT NULL COMMENT '状态更新时间',
  `entry_time` datetime NULL DEFAULT NULL COMMENT '入职时间',
  `resume_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '简历附件地址数组',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_status_update_time`(`status_update_time` ASC) USING BTREE,
  INDEX `idx_channel_id`(`channel_id` ASC) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 招聘候选人';

-- ----------------------------
-- 34. HRM 招聘面试记录 hrm_recruit_interview
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_recruit_interview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '面试编号',
  `candidate_id` bigint NULL DEFAULT NULL COMMENT '候选人编号',
  `type` int NULL DEFAULT NULL COMMENT '面试方式 字典',
  `stage_number` int NULL DEFAULT NULL COMMENT '面试轮次',
  `interview_employee_id` bigint NULL DEFAULT NULL COMMENT '主面试官员工编号',
  `other_interview_employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '其他面试官员工编号数组',
  `interview_time` datetime NULL DEFAULT NULL COMMENT '面试时间',
  `address` varchar(512) NULL DEFAULT NULL COMMENT '面试地址',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `result` int NULL DEFAULT NULL COMMENT '面试结果 枚举  字典',
  `evaluate` varchar(255) NULL DEFAULT NULL COMMENT '面试评价',
  `cancel_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '取消原因',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_candidate_id`(`candidate_id` ASC) USING BTREE,
  INDEX `idx_stage_number`(`stage_number` ASC) USING BTREE,
  INDEX `idx_interview_time`(`interview_time` ASC) USING BTREE,
  INDEX `idx_result`(`result` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 招聘面试记录';

-- ----------------------------
-- 35. HRM 招聘渠道 hrm_recruit_channel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_recruit_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘渠道编号',
  `system_flag` bit(1) NULL DEFAULT NULL COMMENT '是否系统内置',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '渠道名称',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 招聘渠道';

-- ----------------------------
-- 36. HRM 招聘职位 hrm_recruit_post
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_recruit_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘职位编号',
  `post_name` varchar(128) NULL DEFAULT NULL COMMENT '职位名称',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门编号',
  `job_nature` int NULL DEFAULT NULL COMMENT '工作性质 字典',
  `area_id` int NULL DEFAULT NULL COMMENT '工作城市地区编号',
  `recruit_num` int NULL DEFAULT NULL COMMENT '招聘人数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '招聘原因',
  `work_time` int NULL DEFAULT NULL COMMENT '工作经验要求 字典',
  `education_require` int NULL DEFAULT NULL COMMENT '学历要求 字典',
  `min_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '最低薪资 -1 表示面议',
  `max_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '最高薪资 -1 表示面议',
  `salary_unit` int NULL DEFAULT NULL COMMENT '薪资单位 字典',
  `min_age` int NULL DEFAULT NULL COMMENT '最小年龄 -1 表示不限',
  `max_age` int NULL DEFAULT NULL COMMENT '最大年龄 -1 表示不限',
  `latest_entry_time` datetime NULL DEFAULT NULL COMMENT '最迟到岗时间',
  `owner_employee_id` bigint NULL DEFAULT NULL COMMENT '招聘负责人员工编号',
  `interview_employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '面试官员工编号数组',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '职位描述',
  `emergency_level` int NULL DEFAULT NULL COMMENT '紧急程度 字典',
  `post_type_id` bigint NULL DEFAULT NULL COMMENT '职位类型编号',
  `status` int NULL DEFAULT NULL COMMENT '职位状态 枚举  字典',
  `stop_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '停止原因',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_owner_employee_id`(`owner_employee_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 招聘职位';

-- ----------------------------
-- 37. HRM 招聘职位类型 hrm_recruit_post_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_recruit_post_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位类型编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '类型名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父类型编号',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 招聘职位类型';

-- ----------------------------
-- 38. HRM 调薪模板 hrm_salary_change_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_change_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调薪模板编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '模板名称',
  `default_status` bit(1) NULL DEFAULT NULL COMMENT '是否默认模板',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '调薪项配置',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 调薪模板';

-- ----------------------------
-- 39. HRM 计薪配置 hrm_salary_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cycle_start_day` int NULL DEFAULT NULL COMMENT '计薪周期开始日',
  `cycle_end_day` int NULL DEFAULT NULL COMMENT '计薪周期结束日',
  `social_security_month_type` int NULL DEFAULT NULL COMMENT '社保对应月份类型 枚举',
  `start_year` int NULL DEFAULT NULL COMMENT '工资开始年份',
  `start_month` int NULL DEFAULT NULL COMMENT '工资开始月份',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 计薪配置';

-- ----------------------------
-- 40. HRM 薪资组 hrm_salary_group
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NULL DEFAULT NULL COMMENT '薪资组名称',
  `salary_standard` decimal(12,2) NULL DEFAULT NULL COMMENT '月计薪标准',
  `change_rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '转正、调薪月规则',
  `dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '适用部门编号列表',
  `employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '适用员工编号列表',
  `tax_rule_id` bigint NULL DEFAULT NULL COMMENT '计税规则编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tax_rule_id`(`tax_rule_id` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 薪资组';

-- ----------------------------
-- 41. HRM 工资表薪资项 hrm_salary_option
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '薪资项编号',
  `template_id` bigint NULL DEFAULT NULL COMMENT '标准薪资项目录编号（可选）',
  `code` int NULL DEFAULT NULL COMMENT '薪资项编码',
  `parent_code` int NULL DEFAULT NULL COMMENT '父薪资项编码',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '薪资项名称',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `system_flag` bit(1) NULL DEFAULT NULL COMMENT '是否系统默认项',
  `type` int NULL DEFAULT NULL COMMENT '薪资项类型 枚举',
  `visible` bit(1) NULL DEFAULT NULL COMMENT '是否显示',
  `enabled` bit(1) NULL DEFAULT NULL COMMENT '是否启用',
  `tax_enabled` bit(1) NULL DEFAULT NULL COMMENT '是否计税',
  `calculate_enabled` bit(1) NULL DEFAULT NULL COMMENT '是否参与计算',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_code`(`code` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_visible`(`visible` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 工资表薪资项';

-- ----------------------------
-- 42. HRM 标准薪资项目录 hrm_salary_option_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_option_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标准薪资项编号',
  `code` int NULL DEFAULT NULL COMMENT '薪资项编码',
  `parent_code` int NULL DEFAULT NULL COMMENT '父薪资项编码',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '薪资项名称',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `system_flag` bit(1) NULL DEFAULT NULL COMMENT '是否系统默认项',
  `type` int NULL DEFAULT NULL COMMENT '薪资项类型 枚举',
  `visible` bit(1) NULL DEFAULT NULL COMMENT '是否显示',
  `tax_enabled` bit(1) NULL DEFAULT NULL COMMENT '是否计税',
  `calculate_enabled` bit(1) NULL DEFAULT NULL COMMENT '是否参与计算',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_code`(`code` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 标准薪资项目录';

-- ----------------------------
-- 43. HRM 计税规则 hrm_salary_tax_rule
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_tax_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '计税规则名称',
  `type` int NULL DEFAULT NULL COMMENT '计税类型 枚举',
  `tax_enabled` bit(1) NULL DEFAULT NULL COMMENT '是否计税',
  `threshold` decimal(12,2) NULL DEFAULT NULL COMMENT '起征阈值',
  `decimal_scale` int NULL DEFAULT NULL COMMENT '小数位数',
  `cycle_type` int NULL DEFAULT NULL COMMENT '计税周期类型 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 计税规则';

-- ----------------------------
-- 44. HRM 定薪/调薪记录 hrm_salary_change_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `type` int NULL DEFAULT NULL COMMENT '记录类型 枚举',
  `reason` int NULL DEFAULT NULL COMMENT '调整原因 枚举',
  `effect_time` datetime NULL DEFAULT NULL COMMENT '生效时间',
  `before_total` decimal(12,2) NULL DEFAULT NULL COMMENT '调整前正式薪资',
  `after_total` decimal(12,2) NULL DEFAULT NULL COMMENT '调整后正式薪资',
  `probation_before_total` decimal(12,2) NULL DEFAULT NULL COMMENT '调整前试用期薪资',
  `probation_after_total` decimal(12,2) NULL DEFAULT NULL COMMENT '调整后试用期薪资',
  `salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '正式薪资项后态快照',
  `probation_salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '试用期薪资项后态快照',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_effect_time`(`effect_time` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 定薪/调薪记录';

-- ----------------------------
-- 45. HRM 员工薪资信息 hrm_salary_employee_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_employee_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `change_reason` int NULL DEFAULT NULL COMMENT '定薪、调薪原因 枚举',
  `effect_time` datetime NULL DEFAULT NULL COMMENT '生效时间',
  `change_type` int NULL DEFAULT NULL COMMENT '薪资信息变更类型 枚举',
  `probation_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '试用期工资',
  `regular_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '正式工资',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '正式薪资项 薪资项随员工薪资信息整体维护',
  `probation_salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '试用期薪资项 薪资项随员工薪资信息整体维护',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_change_type`(`change_type` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工薪资信息';

-- ----------------------------
-- 46. HRM 员工月度工资记录 hrm_salary_month_employee_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_month_employee_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `month_record_id` bigint NULL DEFAULT NULL COMMENT '月度工资表编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `actual_work_day` decimal(10,4) NULL DEFAULT NULL COMMENT '实际工作天数',
  `need_work_day` decimal(10,4) NULL DEFAULT NULL COMMENT '应工作天数',
  `year` int NULL DEFAULT NULL COMMENT '年份',
  `month` int NULL DEFAULT NULL COMMENT '月份',
  `expected_pay_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '应发工资',
  `taxable_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '应税工资',
  `personal_tax` decimal(12,2) NULL DEFAULT NULL COMMENT '个人所得税',
  `real_pay_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '实发工资',
  `option_values` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '薪资项值快照 薪资项值随员工月度工资记录整体维护',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_month_record_id`(`month_record_id` ASC) USING BTREE,
  INDEX `idx_year`(`year` ASC) USING BTREE,
  INDEX `idx_month`(`month` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 员工月度工资记录';

-- ----------------------------
-- 47. HRM 月度工资表 hrm_salary_month_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) NULL DEFAULT NULL COMMENT '工资表标题',
  `year` int NULL DEFAULT NULL COMMENT '年份',
  `month` int NULL DEFAULT NULL COMMENT '月份',
  `employee_count` int NULL DEFAULT NULL COMMENT '员工人数',
  `start_time` datetime NULL DEFAULT NULL COMMENT '计薪开始日期',
  `end_time` datetime NULL DEFAULT NULL COMMENT '计薪结束日期',
  `expected_pay_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '应发工资',
  `personal_tax` decimal(12,2) NULL DEFAULT NULL COMMENT '个人所得税',
  `real_pay_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '实发工资',
  `personal_insurance_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(12,2) NULL DEFAULT NULL COMMENT '公司公积金金额',
  `option_headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '薪资项表头快照',
  `status` int NULL DEFAULT NULL COMMENT '状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_year`(`year` ASC) USING BTREE,
  INDEX `idx_month`(`month` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 月度工资表';

-- ----------------------------
-- 48. HRM 工资条 hrm_salary_slip
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_slip` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `send_record_id` bigint NULL DEFAULT NULL COMMENT '工资条发放记录编号',
  `month_employee_record_id` bigint NULL DEFAULT NULL COMMENT '员工月度工资记录编号',
  `employee_id` bigint NULL DEFAULT NULL COMMENT '员工编号',
  `year` int NULL DEFAULT NULL COMMENT '年份',
  `month` int NULL DEFAULT NULL COMMENT '月份',
  `read_status` int NULL DEFAULT NULL COMMENT '阅读状态 枚举',
  `real_pay_salary` decimal(12,2) NULL DEFAULT NULL COMMENT '实发工资',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工资条项，随员工工资条整体维护',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_employee_id`(`employee_id` ASC) USING BTREE,
  INDEX `idx_send_record_id`(`send_record_id` ASC) USING BTREE,
  INDEX `idx_read_status`(`read_status` ASC) USING BTREE,
  INDEX `idx_month_employee_record_id`(`month_employee_record_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 工资条';

-- ----------------------------
-- 49. HRM 工资条发放记录 DO 同一月度工资表可以分多批次发放工资条，因此发放记录保持独立实体。 hrm_salary_slip_send_record
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_slip_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `month_record_id` bigint NULL DEFAULT NULL COMMENT '月度工资表编号',
  `employee_count` int NULL DEFAULT NULL COMMENT '工资表总人数',
  `send_employee_count` int NULL DEFAULT NULL COMMENT '本批次发放人数',
  `year` int NULL DEFAULT NULL COMMENT '年份',
  `month` int NULL DEFAULT NULL COMMENT '月份',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_year`(`year` ASC) USING BTREE,
  INDEX `idx_month`(`month` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 工资条发放记录 DO 同一月度工资表可以分多批次发放工资条，因此发放记录保持独立实体。';

-- ----------------------------
-- 50. HRM 工资条模板 hrm_salary_slip_template（平台级默认模板全员只读共享）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `hrm_salary_slip_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) NULL DEFAULT NULL COMMENT '模板名称',
  `hide_empty` bit(1) NULL DEFAULT NULL COMMENT '是否隐藏空项',
  `default_status` bit(1) NULL DEFAULT NULL COMMENT '是否默认模板',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工资条模板项，随模板整体维护',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_default_status`(`default_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'HRM 工资条模板';

-- 共 50 张表
