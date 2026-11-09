-- =============================================================================
-- OA 模块建表脚本（MySQL 8 / InnoDB / utf8mb4_unicode_ci）
--
-- 从 ruoyi-vue-pro 的 sql/mysql/oa-ddl.sql 里按「本次真的会移植的域」裁剪而来，
-- 表结构、审计列（creator/create_time/updater/update_time/deleted）与索引保持原样，
-- 这样 ruoyi 的 OA 代码只改包名即可落库。
--
-- 未纳入的域：云盘（oa_file_*）、邮件（oa_mail_*）、讨论（oa_discussion_*）、
-- 便签（oa_note_*）、投票（oa_vote_*）、个人通讯录（oa_contact_*）——
-- 企业微信本身已经提供这些能力，个人中心 demo 里也没有对应入口。
-- =============================================================================

SET NAMES utf8mb4;

-- 1. oa_announcement
CREATE TABLE IF NOT EXISTS `oa_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `type` int NULL DEFAULT NULL COMMENT '公告类型',
  `priority` int NULL DEFAULT NULL COMMENT '优先级',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '相关链接',
  `top` bit(1) NULL DEFAULT NULL COMMENT '是否置顶',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 公告表';

-- ----------------------------
-- 2. OA 公告接收关系 oa_announcement_receiver
-- ----------------------------

-- 2. oa_announcement_receiver
CREATE TABLE IF NOT EXISTS `oa_announcement_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接收关系编号',
  `announcement_id` bigint NULL DEFAULT NULL COMMENT '公告编号',
  `receiver_user_id` bigint NULL DEFAULT NULL COMMENT '接收人用户编号',
  `read_status` bit(1) NULL DEFAULT NULL COMMENT '是否已读',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_announcement_id`(`announcement_id` ASC) USING BTREE,
  INDEX `idx_receiver_user_id`(`receiver_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 公告接收关系表';

-- ----------------------------
-- 3. OA 考勤 oa_attendance
-- ----------------------------

-- 3. oa_attendance
CREATE TABLE IF NOT EXISTS `oa_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤编号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户编号',
  `type` int NULL DEFAULT NULL COMMENT '考勤类型',
  `status` int NULL DEFAULT NULL COMMENT '考勤状态',
  `attendance_time` datetime NULL DEFAULT NULL COMMENT '考勤时间',
  `attendance_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '考勤 IP',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '考勤备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_attendance_time`(`attendance_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 考勤表';

-- ----------------------------
-- 4. OA 联系人分类 oa_contact_category
-- ----------------------------

-- 4. oa_leave_apply
CREATE TABLE IF NOT EXISTS `oa_leave_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请标题',
  `urgency` int NULL DEFAULT NULL COMMENT '紧急程度',
  `type` int NULL DEFAULT NULL COMMENT '请假类型',
  `start_time` datetime NULL DEFAULT NULL COMMENT '请假开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '请假结束时间',
  `days` int NULL DEFAULT NULL COMMENT '请假天数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请原因',
  `approvers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自选审批人的员工编号 JSON 数组',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `wecom_approval_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '企微审批单号 sp_no',
  `wecom_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提交企微的幂等请求号',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_process_instance_id`(`process_instance_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 请假申请表';

-- ----------------------------
-- 16. OA 企业邮箱账号 oa_mail_account
-- ----------------------------

-- 5. oa_overtime_apply
CREATE TABLE IF NOT EXISTS `oa_overtime_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请标题',
  `urgency` int NULL DEFAULT NULL COMMENT '紧急程度',
  `type` int NULL DEFAULT NULL COMMENT '加班类型',
  `start_time` datetime NULL DEFAULT NULL COMMENT '加班开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '加班结束时间',
  `days` decimal(10, 2) NULL DEFAULT NULL COMMENT '加班天数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请原因',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 加班申请表';

-- ----------------------------
-- 29. OA 工作计划 oa_plan
-- ----------------------------

-- 6. oa_reimbursement
CREATE TABLE IF NOT EXISTS `oa_reimbursement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请标题',
  `urgency` int NULL DEFAULT NULL COMMENT '紧急程度',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请原因',
  `witness_user_id` bigint NULL DEFAULT NULL COMMENT '证明人用户编号',
  `customer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '相关客户名称',
  `payment_method` int NULL DEFAULT NULL COMMENT '报销方式',
  `invoice_count` int NULL DEFAULT NULL COMMENT '票据总数',
  `total_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '报销总金额，单位：元',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '报销明细',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_process_instance_id`(`process_instance_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 费用报销表';

-- ----------------------------
-- 32. OA 离职申请 oa_resign_apply
-- ----------------------------

-- 7. oa_schedule
CREATE TABLE IF NOT EXISTS `oa_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日程编号',
  `type` int NULL DEFAULT NULL COMMENT '日程类型',
  `priority` int NULL DEFAULT NULL COMMENT '优先级',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `remind` bit(1) NULL DEFAULT NULL COMMENT '是否提醒',
  `reminded` bit(1) NULL DEFAULT NULL COMMENT '是否已提醒',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 日程表';

-- ----------------------------
-- 34. OA 日程参与人 oa_schedule_participant
-- ----------------------------

-- 8. oa_schedule_participant
CREATE TABLE IF NOT EXISTS `oa_schedule_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参与记录编号',
  `schedule_id` bigint NULL DEFAULT NULL COMMENT '日程编号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '参与人用户编号',
  `read_status` bit(1) NULL DEFAULT NULL COMMENT '是否已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '首次阅读时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_schedule_id`(`schedule_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 日程参与人表';

-- ----------------------------
-- 35. OA 印章 oa_seal
-- ----------------------------

-- 9. oa_seal
CREATE TABLE IF NOT EXISTS `oa_seal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '所属部门编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '印章编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '印章名称',
  `type` int NULL DEFAULT NULL COMMENT '印章类型',
  `category` int NULL DEFAULT NULL COMMENT '印章分类',
  `keeper_user_id` bigint NULL DEFAULT NULL COMMENT '保管人用户编号',
  `keeper_dept_id` bigint NULL DEFAULT NULL COMMENT '保管部门编号',
  `status` int NULL DEFAULT NULL COMMENT '印章台账状态',
  `purchase_time` datetime NULL DEFAULT NULL COMMENT '购买时间',
  `enable_time` datetime NULL DEFAULT NULL COMMENT '启用时间',
  `disable_time` datetime NULL DEFAULT NULL COMMENT '停用时间',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '印章照片地址',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_keeper_user_id`(`keeper_user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 印章表';

-- ----------------------------
-- 36. OA 用印申请 oa_seal_apply
-- ----------------------------

-- 10. oa_seal_apply
CREATE TABLE IF NOT EXISTS `oa_seal_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请单号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '申请人用户编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `seal_id` bigint NULL DEFAULT NULL COMMENT '印章编号',
  `seal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '印章编号快照',
  `seal_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '印章名称快照',
  `seal_type` int NULL DEFAULT NULL COMMENT '印章类型快照',
  `keeper_dept_id` bigint NULL DEFAULT NULL COMMENT '保管部门编号快照',
  `keeper_user_id` bigint NULL DEFAULT NULL COMMENT '保管人用户编号快照',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用印事由',
  `type` int NULL DEFAULT NULL COMMENT '用印类型',
  `mode` int NULL DEFAULT NULL COMMENT '用印方式',
  `document_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件标题',
  `document_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型',
  `document_count` int NULL DEFAULT NULL COMMENT '文件份数',
  `contract_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '合同金额',
  `contract_party` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '合同对方',
  `expected_use_time` datetime NULL DEFAULT NULL COMMENT '预计用印时间',
  `actual_use_time` datetime NULL DEFAULT NULL COMMENT '实际用印时间',
  `expected_return_time` datetime NULL DEFAULT NULL COMMENT '预计归还时间',
  `actual_return_time` datetime NULL DEFAULT NULL COMMENT '实际归还时间',
  `use_status` int NULL DEFAULT NULL COMMENT '用印业务状态',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，取 BPM 流程实例状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `urgent` bit(1) NULL DEFAULT NULL COMMENT '是否紧急',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_seal_id`(`seal_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 用印申请表';

-- ----------------------------
-- 37. OA 用品领用申请 oa_supply_apply
-- ----------------------------

-- 11. oa_supply_item
CREATE TABLE IF NOT EXISTS `oa_supply_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '所属部门编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物品名称',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物品编码',
  `category` int NULL DEFAULT NULL COMMENT '类别',
  `manage_type` int NULL DEFAULT NULL COMMENT '管理类型',
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '计量单位',
  `reference_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '参考单价',
  `stock_quantity` int NULL DEFAULT NULL COMMENT '库存数量',
  `min_stock_quantity` int NULL DEFAULT NULL COMMENT '最低库存预警',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物品图片 URL',
  `status` int NULL DEFAULT NULL COMMENT '状态',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 办公用品表';

-- ----------------------------
-- 40. OA 任务 oa_task
-- ----------------------------

-- 12. oa_supply_apply
CREATE TABLE IF NOT EXISTS `oa_supply_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请单号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `apply_time` datetime NULL DEFAULT NULL COMMENT '领用日期',
  `use_type` int NULL DEFAULT NULL COMMENT '使用类型',
  `pickup_method` int NULL DEFAULT NULL COMMENT '领取方式',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请事由',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 用品领用申请表';

-- ----------------------------
-- 38. OA 用品申请明细 oa_supply_apply_item
-- ----------------------------

-- 13. oa_supply_apply_item
CREATE TABLE IF NOT EXISTS `oa_supply_apply_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `apply_id` bigint NULL DEFAULT NULL COMMENT '领用申请编号',
  `item_id` bigint NULL DEFAULT NULL COMMENT '办公用品编号',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '物品名称快照',
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规格型号快照',
  `unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '计量单位快照',
  `manage_type` int NULL DEFAULT NULL COMMENT '管理类型快照',
  `apply_quantity` int NULL DEFAULT NULL COMMENT '申请数量',
  `issued_quantity` int NULL DEFAULT NULL COMMENT '实发数量',
  `returned_quantity` int NULL DEFAULT NULL COMMENT '累计归还数量',
  `status` int NULL DEFAULT NULL COMMENT '明细状态',
  `issue_user_id` bigint NULL DEFAULT NULL COMMENT '发放人用户编号',
  `issue_time` datetime NULL DEFAULT NULL COMMENT '发放时间',
  `issue_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发放备注',
  `return_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '归还备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_apply_id`(`apply_id` ASC) USING BTREE,
  INDEX `idx_item_id`(`item_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 用品申请明细表';

-- ----------------------------
-- 39. OA 办公用品 oa_supply_item
-- ----------------------------

-- 14. oa_travel_apply
CREATE TABLE IF NOT EXISTS `oa_travel_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单据编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '出差事由',
  `start_time` datetime NULL DEFAULT NULL COMMENT '出差开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '出差结束时间',
  `days` int NULL DEFAULT NULL COMMENT '出差天数',
  `companion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '同行人',
  `estimated_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '预计费用，单位：元',
  `reimburse_status` bit(1) NULL DEFAULT NULL COMMENT '是否已报销',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '行程明细',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 出差申请表';

-- ----------------------------
-- 44. OA 出差报销 oa_travel_reimbursement
-- ----------------------------

-- 15. oa_travel_reimbursement
CREATE TABLE IF NOT EXISTS `oa_travel_reimbursement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单据编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `travel_apply_id` bigint NULL DEFAULT NULL COMMENT '关联出差申请编号，未关联申请时为空',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '出差事由',
  `start_time` datetime NULL DEFAULT NULL COMMENT '出差开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '出差结束时间',
  `days` int NULL DEFAULT NULL COMMENT '出差天数',
  `total_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '报销总金额，单位：元',
  `pay_status` bit(1) NULL DEFAULT NULL COMMENT '是否已支付',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '费用明细',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_travel_apply_id`(`travel_apply_id` ASC) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 出差报销表';

-- ----------------------------
-- 45. OA 车辆 oa_vehicle
-- ----------------------------

-- 16. oa_work_report
CREATE TABLE IF NOT EXISTS `oa_work_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汇报编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '汇报单号',
  `type` int NULL DEFAULT NULL COMMENT '汇报类型',
  `status` int NULL DEFAULT NULL COMMENT '汇报状态',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '汇报人所属部门编号',
  `start_time` datetime NULL DEFAULT NULL COMMENT '周期开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '周期结束时间',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '汇报标题',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作总结',
  `plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作计划补充说明',
  `problem` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '问题与协调事项',
  `work_items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '已完成工作项',
  `plan_items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工作计划项',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 工作汇报表';

-- 17. oa_meeting_room
CREATE TABLE IF NOT EXISTS `oa_meeting_room` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会议室名称',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会议室位置',
  `type` int NULL DEFAULT NULL COMMENT '会议室类型',
  `manager_user_id` bigint NULL DEFAULT NULL COMMENT '负责人用户编号',
  `status` int NULL DEFAULT NULL COMMENT '可用状态',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会议室图片 URL',
  `seat_count` int NULL DEFAULT NULL COMMENT '坐席数',
  `equipments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '设备列表',
  `allow_booking` bit(1) NULL DEFAULT NULL COMMENT '是否允许预定',
  `need_approval` bit(1) NULL DEFAULT NULL COMMENT '预定是否需要审批',
  `booking_scope` int NULL DEFAULT NULL COMMENT '可预定范围',
  `booking_user_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '可预定用户编号列表',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 会议室表';

-- ----------------------------
-- 22. OA 笔记目录 oa_note_category
-- ----------------------------

-- 18. oa_meeting_room_booking
CREATE TABLE IF NOT EXISTS `oa_meeting_room_booking` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预定申请单号',
  `room_id` bigint NULL DEFAULT NULL COMMENT '会议室编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会议主题',
  `start_time` datetime NULL DEFAULT NULL COMMENT '会议开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '会议结束时间',
  `moderator_user_id` bigint NULL DEFAULT NULL COMMENT '主持人用户编号',
  `attendee_user_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '参会用户编号列表',
  `reminder_type` int NULL DEFAULT NULL COMMENT '会议提醒类型',
  `reminded` bit(1) NULL DEFAULT NULL COMMENT '是否已发送会议开始提醒',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '会议说明',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，无需审批时提交后直接通过',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号，无需审批时为空',
  `need_approval` bit(1) NULL DEFAULT NULL COMMENT '提交时是否需要审批',
  `use_status` int NULL DEFAULT NULL COMMENT '使用状态',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room_id_start_time`(`room_id` ASC, `start_time` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_use_status`(`use_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 会议室预定表';

-- ----------------------------
-- 21. OA 会议室 oa_meeting_room
-- ----------------------------

-- 19. oa_vehicle
CREATE TABLE IF NOT EXISTS `oa_vehicle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车牌号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车辆名称',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '所属部门编号',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车型',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车辆分类',
  `brand_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '品牌型号',
  `seat_count` int NULL DEFAULT NULL COMMENT '座位数',
  `bare_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '裸车价格（元）',
  `compulsory_insurance_expire_time` datetime NULL DEFAULT NULL COMMENT '交强险到期时间',
  `commercial_insurance_expire_time` datetime NULL DEFAULT NULL COMMENT '商业险到期时间',
  `inspection_expire_time` datetime NULL DEFAULT NULL COMMENT '年检到期时间',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车辆照片 URL',
  `status` int NULL DEFAULT NULL COMMENT '车辆台账状态',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dept_id`(`dept_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 车辆表';

-- ----------------------------
-- 46. OA 用车申请 oa_vehicle_apply
-- ----------------------------

-- 20. oa_vehicle_apply
CREATE TABLE IF NOT EXISTS `oa_vehicle_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用车申请单号',
  `vehicle_id` bigint NULL DEFAULT NULL COMMENT '车辆编号',
  `vehicle_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '车牌号快照',
  `user_id` bigint NULL DEFAULT NULL COMMENT '申请人用户编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `start_time` datetime NULL DEFAULT NULL COMMENT '预计出车时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '预计回车时间',
  `start_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出车地点',
  `end_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预计回车地点',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用车事由',
  `passenger` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '随行人',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `return_status` int NULL DEFAULT NULL COMMENT '还车业务状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_vehicle_id`(`vehicle_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 用车申请表';

-- ----------------------------
-- 47. OA 还车申请 oa_vehicle_return
-- ----------------------------

-- 21. oa_vehicle_return
CREATE TABLE IF NOT EXISTS `oa_vehicle_return` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '还车申请单号',
  `apply_id` bigint NULL DEFAULT NULL COMMENT '用车申请编号',
  `vehicle_id` bigint NULL DEFAULT NULL COMMENT '车辆编号，从用车申请生成',
  `user_id` bigint NULL DEFAULT NULL COMMENT '申请人用户编号',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '申请部门编号',
  `actual_start_time` datetime NULL DEFAULT NULL COMMENT '实际出车时间',
  `start_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际出车地点',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用车事由',
  `passenger` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '随行人',
  `actual_return_time` datetime NULL DEFAULT NULL COMMENT '实际回车时间',
  `return_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '实际回车地点',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '还车说明',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_apply_id`(`apply_id` ASC) USING BTREE,
  INDEX `idx_vehicle_id`(`vehicle_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 还车申请表';

-- ----------------------------
-- 48. OA 工作汇报 oa_work_report
-- ----------------------------

-- 22. oa_task
CREATE TABLE IF NOT EXISTS `oa_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务编号',
  `type` int NULL DEFAULT NULL COMMENT '任务类型',
  `status` int NULL DEFAULT NULL COMMENT '总体状态',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务描述',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务评价',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '业务发布时间，新增及编辑任务时刷新',
  `top` bit(1) NULL DEFAULT NULL COMMENT '是否置顶',
  `canceled` bit(1) NULL DEFAULT NULL COMMENT '是否取消',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 任务表';

-- ----------------------------
-- 41. OA 任务反馈日志 oa_task_log
-- ----------------------------

-- 23. oa_task_log
CREATE TABLE IF NOT EXISTS `oa_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `task_id` bigint NULL DEFAULT NULL COMMENT '任务编号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '反馈人用户编号',
  `status` int NULL DEFAULT NULL COMMENT '变更后的状态',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '反馈内容',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 任务反馈日志表';

-- ----------------------------
-- 42. OA 任务接收人 oa_task_receiver
-- ----------------------------

-- 24. oa_task_receiver
CREATE TABLE IF NOT EXISTS `oa_task_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `task_id` bigint NULL DEFAULT NULL COMMENT '任务编号',
  `user_id` bigint NULL DEFAULT NULL COMMENT '接收人用户编号',
  `status` int NULL DEFAULT NULL COMMENT '任务状态',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id_user_id`(`task_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 任务接收人表';

-- ----------------------------
-- 43. OA 出差申请 oa_travel_apply
-- ----------------------------

-- 25. oa_official_doc_send
CREATE TABLE IF NOT EXISTS `oa_official_doc_send` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单据编号',
  `template_id` bigint NULL DEFAULT NULL COMMENT '套红模板编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `no_prefix` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发文字号前缀',
  `year` int NULL DEFAULT NULL COMMENT '发文年度',
  `sequence` int NULL DEFAULT NULL COMMENT '发文序号',
  `document_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '公文文号',
  `secrecy_level` int NULL DEFAULT NULL COMMENT '密级',
  `urgency_level` int NULL DEFAULT NULL COMMENT '紧急程度',
  `disclosure_type` int NULL DEFAULT NULL COMMENT '公开类别',
  `issue_time` datetime NULL DEFAULT NULL COMMENT '发文时间',
  `send_dept_id` bigint NULL DEFAULT NULL COMMENT '发文部门编号',
  `main_dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主送部门编号列表',
  `copy_dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '抄送部门编号列表',
  `signer_user_id` bigint NULL DEFAULT NULL COMMENT '签发人用户编号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '正文',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `formal_file_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '正式公文地址',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '附注',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_document_no`(`document_no` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 公文发文表';

-- ----------------------------
-- 27. OA 套红模板 oa_official_doc_template
-- ----------------------------

-- 26. oa_official_doc_receive
CREATE TABLE IF NOT EXISTS `oa_official_doc_receive` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单据编号',
  `send_id` bigint NULL DEFAULT NULL COMMENT '来源发文编号，手工新增收文时为空',
  `receive_type` int NULL DEFAULT NULL COMMENT '收文类型',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `document_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来文字号',
  `secrecy_level` int NULL DEFAULT NULL COMMENT '密级',
  `urgency_level` int NULL DEFAULT NULL COMMENT '紧急程度',
  `receive_time` datetime NULL DEFAULT NULL COMMENT '收文时间',
  `receive_dept_id` bigint NULL DEFAULT NULL COMMENT '收文部门编号',
  `handler_user_id` bigint NULL DEFAULT NULL COMMENT '主办人用户编号',
  `instruction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '领导批示',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '办理结果',
  `deadline_time` datetime NULL DEFAULT NULL COMMENT '办理期限',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容摘要',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '附件地址列表',
  `formal_file_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '正式公文地址',
  `handle_status` int NULL DEFAULT NULL COMMENT '办理状态',
  `status` int NULL DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_send_id`(`send_id` ASC) USING BTREE,
  INDEX `idx_receive_dept_id`(`receive_dept_id` ASC) USING BTREE,
  INDEX `idx_handle_status`(`handle_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 公文收文表';

-- ----------------------------
-- 26. OA 公文发文 oa_official_doc_send
-- ----------------------------

-- 27. oa_official_doc_template
CREATE TABLE IF NOT EXISTS `oa_official_doc_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板名称',
  `authority_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '红头名称',
  `font_size` int NULL DEFAULT NULL COMMENT '红头名称字号',
  `no_prefix` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发文字号前缀',
  `seal_pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '印章图片地址',
  `separator_type` int NULL DEFAULT NULL COMMENT '分隔线类型',
  `status` int NULL DEFAULT NULL COMMENT '状态',
  `sort` int NULL DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 套红模板表';

-- ----------------------------
-- 28. OA 加班申请 oa_overtime_apply
-- ----------------------------

-- 28. oa_regular_apply
CREATE TABLE IF NOT EXISTS `oa_regular_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请标题',
  `urgency` int NULL DEFAULT NULL COMMENT '紧急程度',
  `start_time` datetime NULL DEFAULT NULL COMMENT '试用开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '试用结束时间',
  `days` int NULL DEFAULT NULL COMMENT '试用天数',
  `experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '试用期或实习期心得体会',
  `understanding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '对本岗位职责要求的理解',
  `growth` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '试用期或实习期的成长',
  `deficiency` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '目前存在的不足',
  `improvement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '本岗位工作改进计划',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '对公司产品的意见及建议',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 转正申请表';

-- ----------------------------
-- 31. OA 费用报销 oa_reimbursement
-- ----------------------------

-- 29. oa_resign_apply
CREATE TABLE IF NOT EXISTS `oa_resign_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '申请标题',
  `urgency` int NULL DEFAULT NULL COMMENT '紧急程度',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请原因',
  `handover_user_id` bigint NULL DEFAULT NULL COMMENT '工作交接人用户编号',
  `unfinished_work` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '未完成事宜',
  `has_pending_reimbursement` bit(1) NULL DEFAULT NULL COMMENT '是否有未完成的费用报销',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请人的意见及建议',
  `status` int NULL DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_creator`(`creator` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'OA 离职申请表';

-- ----------------------------
-- 33. OA 日程 oa_schedule
-- ----------------------------

-- 共 29 张表

-- =============================================================================
-- 增补：请假以企业微信为审批出口。MySQL 8 的 ALTER 没有 IF NOT EXISTS，
-- 重复执行前先看 information_schema。
-- =============================================================================

-- 1. 记住企微审批单号与幂等请求号：提交失败可重试同一个 request_id，同步状态也按它回读。
ALTER TABLE `oa_leave_apply`
  ADD COLUMN `wecom_approval_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    NULL DEFAULT NULL COMMENT '企微审批单号 sp_no' AFTER `process_instance_id`,
  ADD COLUMN `wecom_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    NULL DEFAULT NULL COMMENT '提交企微的幂等请求号' AFTER `wecom_approval_no`;
ALTER TABLE `oa_leave_apply`
  ADD UNIQUE INDEX `uk_wecom_request_id`(`wecom_request_id` ASC);

-- 2. 审批人存在单据上而不是提交请求里：企微要求发起人自选审批人，草稿重新提交要沿用同一批人。
ALTER TABLE `oa_leave_apply`
  ADD COLUMN `approvers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
    NULL COMMENT '自选审批人的员工编号 JSON 数组' AFTER `reason`;
