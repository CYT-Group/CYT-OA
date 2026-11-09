-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: cyt_oa_center
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `flow_definition`
--

DROP TABLE IF EXISTS `flow_definition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_definition` (
  `id` bigint NOT NULL COMMENT '主键id',
  `flow_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程编码',
  `flow_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程名称',
  `model_value` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
  `category` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程类别',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程版本',
  `is_publish` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `activity_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
  `listener_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器类型',
  `listener_path` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器路径',
  `ext` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_flow_code` (`flow_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_form`
--

DROP TABLE IF EXISTS `flow_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_form` (
  `id` bigint NOT NULL COMMENT '主键id',
  `form_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单编码',
  `form_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单名称',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表单版本',
  `is_publish` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
  `form_type` tinyint(1) DEFAULT '0' COMMENT '表单类型（0内置表单 存 form_content 1外挂表单 存form_path）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表单路径',
  `form_content` longtext COLLATE utf8mb4_unicode_ci COMMENT '表单内容',
  `ext` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '表单扩展，用户自行使用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_form_code` (`form_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程表单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_his_task`
--

DROP TABLE IF EXISTS `flow_his_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_his_task` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
  `instance_id` bigint NOT NULL COMMENT '对应flow_instance表的id',
  `task_id` bigint NOT NULL COMMENT '对应flow_task表的id',
  `node_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开始节点名称',
  `node_type` tinyint(1) DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `target_node_code` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标节点编码',
  `target_node_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结束节点名称',
  `approver` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批人',
  `cooperate_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
  `collaborator` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '协作人',
  `skip_type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
  `flow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `variable` text COLLATE utf8mb4_unicode_ci COMMENT '任务变量',
  `ext` text COLLATE utf8mb4_unicode_ci COMMENT '业务详情 存业务表对象json字符串',
  `create_time` datetime DEFAULT NULL COMMENT '任务开始时间',
  `update_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='历史任务记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_instance`
--

DROP TABLE IF EXISTS `flow_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_instance` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
  `business_id` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务id',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `node_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程节点名称',
  `variable` text COLLATE utf8mb4_unicode_ci COMMENT '任务变量',
  `flow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `activity_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
  `def_json` text COLLATE utf8mb4_unicode_ci COMMENT '流程定义json',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `ext` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_business_id` (`business_id`),
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_node`
--

DROP TABLE IF EXISTS `flow_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_node` (
  `id` bigint NOT NULL COMMENT '主键id',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `definition_id` bigint NOT NULL COMMENT '流程定义id',
  `node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程节点名称',
  `permission_flag` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
  `node_ratio` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程签署比例值',
  `coordinate` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '坐标',
  `any_node_skip` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任意结点跳转',
  `listener_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器类型',
  `listener_path` varchar(400) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '监听器路径',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `version` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '版本',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `ext` text COLLATE utf8mb4_unicode_ci COMMENT '节点扩展属性',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_skip`
--

DROP TABLE IF EXISTS `flow_skip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_skip` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '流程定义id',
  `now_node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '当前流程节点的编码',
  `now_node_type` tinyint(1) DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `next_node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '下一个流程节点的编码',
  `next_node_type` tinyint(1) DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `skip_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转名称',
  `skip_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
  `skip_condition` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转条件',
  `coordinate` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '坐标',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点跳转关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_task`
--

DROP TABLE IF EXISTS `flow_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_task` (
  `id` bigint NOT NULL COMMENT '主键id',
  `definition_id` bigint NOT NULL COMMENT '对应flow_definition表的id',
  `instance_id` bigint NOT NULL COMMENT '对应flow_instance表的id',
  `node_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点编码',
  `node_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '节点名称',
  `node_type` tinyint(1) NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
  `flow_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
  `form_custom` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
  `form_path` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批表单路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_instance_id` (`instance_id`),
  KEY `idx_definition_id` (`definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flow_user`
--

DROP TABLE IF EXISTS `flow_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_user` (
  `id` bigint NOT NULL COMMENT '主键id',
  `type` char(1) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
  `processed_by` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '权限人',
  `associated` bigint NOT NULL COMMENT '任务表id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新人',
  `del_flag` char(1) COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT '删除标志',
  `tenant_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `user_processed_type` (`processed_by`,`type`),
  KEY `user_associated` (`associated`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_attendance_clock`
--

DROP TABLE IF EXISTS `hrm_attendance_clock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_attendance_clock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '打卡记录编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `clock_time` datetime DEFAULT NULL COMMENT '打卡时间',
  `type` int DEFAULT NULL COMMENT '打卡类型 枚举  字典',
  `attendance_time` datetime DEFAULT NULL COMMENT '应打卡时间',
  `source_type` int DEFAULT NULL COMMENT '打卡来源 枚举  字典',
  `status` int DEFAULT NULL COMMENT '打卡状态 枚举  字典',
  `stage` int DEFAULT NULL COMMENT '打卡阶段 枚举',
  `address` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '打卡地址',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `ssid` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'WiFi 名称',
  `mac` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'WiFi MAC 地址',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_clock_time` (`clock_time`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 考勤打卡记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_attendance_group`
--

DROP TABLE IF EXISTS `hrm_attendance_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_attendance_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤组编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考勤组名称',
  `dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '适用部门编号',
  `employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '适用员工编号',
  `open_wifi_card` bit(1) DEFAULT NULL COMMENT '是否启用 WiFi 打卡',
  `open_point_card` bit(1) DEFAULT NULL COMMENT '是否启用定位打卡',
  `shifts` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '班次配置',
  `rest` bit(1) DEFAULT NULL COMMENT '是否法定节假日休息',
  `special_dates` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '特殊日期设置数组',
  `points` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '打卡地点配置',
  `wifis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'WiFi 打卡配置',
  `deduct_rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '扣款规则',
  `default_status` bit(1) DEFAULT NULL COMMENT '是否默认考勤组',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_default_status` (`default_status`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 考勤组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_attendance_holiday`
--

DROP TABLE IF EXISTS `hrm_attendance_holiday`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_attendance_holiday` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节假日编号',
  `date` datetime DEFAULT NULL COMMENT '日期',
  `type` int DEFAULT NULL COMMENT '日期类型 枚举  字典',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_date` (`date`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 考勤节假日';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_attendance_leave`
--

DROP TABLE IF EXISTS `hrm_attendance_leave`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_attendance_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '请假记录编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假类型 字典',
  `start_time` datetime DEFAULT NULL COMMENT '请假开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '请假结束时间',
  `day` decimal(10,4) DEFAULT NULL COMMENT '请假时长',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请假理由',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `approval_status` int DEFAULT NULL COMMENT '审批状态 枚举',
  `process_instance_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `approval_time` datetime DEFAULT NULL COMMENT '审批完成时间',
  `approval_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '审批或取消原因',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_approval_status` (`approval_status`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 考勤请假';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_config`
--

DROP TABLE IF EXISTS `hrm_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `type` int DEFAULT NULL COMMENT '配置类型 枚举',
  `value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置值',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 通用业务配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee`
--

DROP TABLE IF EXISTS `hrm_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '员工姓名',
  `job_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工号',
  `user_id` bigint DEFAULT NULL COMMENT '后台用户编号 允许为空，用于待入职、离职历史等没有登录账号的员工档案',
  `mobile` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  `country` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家或地区',
  `nation` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '民族',
  `id_type` int DEFAULT NULL COMMENT '证件类型 枚举',
  `id_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证件号码',
  `sex` int DEFAULT NULL COMMENT '性别 枚举',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `native_place` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '籍贯',
  `birthday` datetime DEFAULT NULL COMMENT '出生时间',
  `age` int DEFAULT NULL COMMENT '年龄',
  `address` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '户籍地址',
  `highest_education` int DEFAULT NULL COMMENT '最高学历 枚举  字典',
  `dept_id` bigint DEFAULT NULL COMMENT '部门编号',
  `leader_employee_id` bigint DEFAULT NULL COMMENT '直属上级员工编号',
  `entry_status` int DEFAULT NULL COMMENT '入职状态 枚举  字典',
  `status` int DEFAULT NULL COMMENT '员工状态 枚举  字典',
  `type` int DEFAULT NULL COMMENT '聘用形式 枚举  字典',
  `entry_time` datetime DEFAULT NULL COMMENT '入职时间',
  `probation` int DEFAULT NULL COMMENT '试用期，单位：月 0 表示无试用期',
  `regular_time` datetime DEFAULT NULL COMMENT '转正时间',
  `leave_time` datetime DEFAULT NULL COMMENT '离职时间',
  `post_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位名称',
  `post_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '岗位职级',
  `work_city` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作城市',
  `work_address` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作地址',
  `work_detail_address` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作详细地址',
  `channel_id` bigint DEFAULT NULL COMMENT '招聘渠道编号',
  `company_age_start_time` datetime DEFAULT NULL COMMENT '司龄开始时间',
  `company_age` int DEFAULT NULL COMMENT '司龄，单位：年',
  `candidate_id` bigint DEFAULT NULL COMMENT '招聘候选人编号',
  `celebration_opt_out` tinyint(1) NOT NULL DEFAULT '0' COMMENT '本人不参与生日/周年庆祝',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `active_user_id` bigint GENERATED ALWAYS AS ((case when (`deleted` = 0) then `user_id` else NULL end)) STORED,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_hrm_employee_active_user` (`active_user_id`),
  KEY `idx_entry_status` (`entry_status`) USING BTREE,
  KEY `idx_job_number` (`job_number`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_entry_time` (`entry_time`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_certificate`
--

DROP TABLE IF EXISTS `hrm_employee_certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_certificate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '证书编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证书名称',
  `level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证书级别',
  `no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证书编号',
  `start_time` datetime DEFAULT NULL COMMENT '有效开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '有效结束日期',
  `issuing_authority` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发证机构',
  `issuing_time` datetime DEFAULT NULL COMMENT '发证日期',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工证书';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_change_record`
--

DROP TABLE IF EXISTS `hrm_employee_change_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '异动记录编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `type` int DEFAULT NULL COMMENT '异动类型 枚举',
  `reason` int DEFAULT NULL COMMENT '异动原因 枚举',
  `old_dept_id` bigint DEFAULT NULL COMMENT '原部门编号',
  `new_dept_id` bigint DEFAULT NULL COMMENT '新部门编号',
  `old_post_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原岗位',
  `new_post_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新岗位',
  `old_post_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原职级',
  `new_post_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新职级',
  `old_work_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原工作地点',
  `new_work_address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '新工作地点',
  `old_leader_employee_id` bigint DEFAULT NULL COMMENT '原直属上级员工编号',
  `new_leader_employee_id` bigint DEFAULT NULL COMMENT '新直属上级员工编号',
  `probation` int DEFAULT NULL COMMENT '试用期，单位：月',
  `effect_time` datetime DEFAULT NULL COMMENT '生效日期',
  `applied_time` datetime DEFAULT NULL COMMENT '实际生效时间',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_effect_time` (`effect_time`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工异动记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_contact`
--

DROP TABLE IF EXISTS `hrm_employee_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人名称',
  `relation` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '与员工的关系',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人电话',
  `work_unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人工作单位',
  `post_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人职务',
  `address` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人地址',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工联系人';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_contract`
--

DROP TABLE IF EXISTS `hrm_employee_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '合同编号',
  `type` int DEFAULT NULL COMMENT '合同类型 枚举',
  `start_time` datetime DEFAULT NULL COMMENT '合同开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '合同结束日期',
  `term` int DEFAULT NULL COMMENT '合同期限，单位：年',
  `status` int DEFAULT NULL COMMENT '合同状态 枚举',
  `sign_company` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签约公司',
  `sign_time` datetime DEFAULT NULL COMMENT '合同签订日期',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `expire_remind` bit(1) DEFAULT NULL COMMENT '是否到期提醒',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址数组',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_end_time` (`end_time`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_expire_remind` (`expire_remind`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工合同';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_education_experience`
--

DROP TABLE IF EXISTS `hrm_employee_education_experience`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_education_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教育经历编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `education` int DEFAULT NULL COMMENT '学历 枚举  字典',
  `graduate_school` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '毕业院校',
  `major` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业',
  `admission_time` datetime DEFAULT NULL COMMENT '入学日期',
  `graduation_time` datetime DEFAULT NULL COMMENT '毕业日期',
  `teaching_methods` int DEFAULT NULL COMMENT '教学方式 枚举',
  `first_degree` bit(1) DEFAULT NULL COMMENT '是否第一学历',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工教育经历';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_file`
--

DROP TABLE IF EXISTS `hrm_employee_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `type` int DEFAULT NULL COMMENT '附件类型 枚举',
  `url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件地址',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工材料附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_personal_note`
--

DROP TABLE IF EXISTS `hrm_employee_personal_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_personal_note` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint DEFAULT NULL COMMENT '所属员工编号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备忘内容',
  `reminder_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_reminder_time` (`reminder_time`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工个人备忘';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_quit_info`
--

DROP TABLE IF EXISTS `hrm_employee_quit_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_quit_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '离职信息编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `plan_quit_time` datetime DEFAULT NULL COMMENT '计划离职时间',
  `apply_quit_time` datetime DEFAULT NULL COMMENT '申请离职时间',
  `salary_settlement_time` datetime DEFAULT NULL COMMENT '薪资结算时间',
  `type` int DEFAULT NULL COMMENT '离职类型 枚举',
  `reason` int DEFAULT NULL COMMENT '离职原因 枚举',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `old_employee_status` int DEFAULT NULL COMMENT '离职前员工状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_plan_quit_time` (`plan_quit_time`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工离职信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_salary_card`
--

DROP TABLE IF EXISTS `hrm_employee_salary_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_salary_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `bank_card_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '银行卡号',
  `bank_area_id` int DEFAULT NULL COMMENT '开户地区编号',
  `bank_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '银行名称',
  `bank_branch_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开户支行名称',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工工资卡';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_training_experience`
--

DROP TABLE IF EXISTS `hrm_employee_training_experience`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_training_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '培训经历编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `course` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '培训课程',
  `organization_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '培训机构名称',
  `start_time` datetime DEFAULT NULL COMMENT '培训开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '培训结束日期',
  `duration` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '培训时长',
  `result` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '培训成绩',
  `certificate_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '培训证书名称',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工培训经历';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_wecom_sync`
--

DROP TABLE IF EXISTS `hrm_employee_wecom_sync`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_wecom_sync` (
  `employee_id` bigint NOT NULL,
  `birthday` datetime DEFAULT NULL,
  `entry_time` datetime DEFAULT NULL,
  `synced_at` datetime NOT NULL,
  PRIMARY KEY (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企微花名册日期同步基线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_employee_work_experience`
--

DROP TABLE IF EXISTS `hrm_employee_work_experience`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_employee_work_experience` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工作经历编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `work_unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作单位',
  `post_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职务',
  `start_time` datetime DEFAULT NULL COMMENT '工作开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '工作结束日期',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '离职原因',
  `witness_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证明人',
  `witness_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证明人手机号',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工作备注',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工工作经历';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_insurance_employee_info`
--

DROP TABLE IF EXISTS `hrm_insurance_employee_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_insurance_employee_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `first_social_security` bit(1) DEFAULT NULL COMMENT '是否首次参保',
  `first_accumulation_fund` bit(1) DEFAULT NULL COMMENT '是否首次缴纳公积金',
  `social_security_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '社保账号',
  `accumulation_fund_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公积金账号',
  `social_security_start_month` datetime DEFAULT NULL COMMENT '社保起缴月份',
  `scheme_id` bigint DEFAULT NULL COMMENT '社保方案编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_scheme_id` (`scheme_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工参保信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_insurance_month_employee_record`
--

DROP TABLE IF EXISTS `hrm_insurance_month_employee_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_insurance_month_employee_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `month_record_id` bigint DEFAULT NULL COMMENT '月度社保表编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `scheme_id` bigint DEFAULT NULL COMMENT '社保方案编号',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` int DEFAULT NULL COMMENT '月份',
  `personal_insurance_amount` decimal(12,2) DEFAULT NULL COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(12,2) DEFAULT NULL COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(12,2) DEFAULT NULL COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(12,2) DEFAULT NULL COMMENT '公司公积金金额',
  `status` int DEFAULT NULL COMMENT '参保状态 枚举',
  `projects` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '参保项目快照',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_month_record_id` (`month_record_id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_scheme_id` (`scheme_id`) USING BTREE,
  KEY `idx_year` (`year`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工月度社保记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_insurance_month_record`
--

DROP TABLE IF EXISTS `hrm_insurance_month_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_insurance_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` int DEFAULT NULL COMMENT '月份',
  `insured_employee_count` int DEFAULT NULL COMMENT '参保人数',
  `stopped_employee_count` int DEFAULT NULL COMMENT '停保人数',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `personal_insurance_amount` decimal(12,2) DEFAULT NULL COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(12,2) DEFAULT NULL COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(12,2) DEFAULT NULL COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(12,2) DEFAULT NULL COMMENT '公司公积金金额',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_year` (`year`) USING BTREE,
  KEY `idx_month` (`month`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 月度社保表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_insurance_scheme`
--

DROP TABLE IF EXISTS `hrm_insurance_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_insurance_scheme` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方案名称',
  `area_id` int DEFAULT NULL COMMENT '参保地区编号',
  `household_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '户籍类型',
  `type` int DEFAULT NULL COMMENT '方案类型 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_area_id` (`area_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 社保方案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_insurance_scheme_project`
--

DROP TABLE IF EXISTS `hrm_insurance_scheme_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_insurance_scheme_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `scheme_id` bigint DEFAULT NULL COMMENT '社保方案编号',
  `type` int DEFAULT NULL COMMENT '项目类型 枚举',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项目名称',
  `base_amount` decimal(12,2) DEFAULT NULL COMMENT '缴纳基数',
  `corporate_rate` decimal(10,4) DEFAULT NULL COMMENT '公司缴纳比例',
  `personal_rate` decimal(10,4) DEFAULT NULL COMMENT '个人缴纳比例',
  `corporate_amount` decimal(12,2) DEFAULT NULL COMMENT '公司缴纳金额',
  `personal_amount` decimal(12,2) DEFAULT NULL COMMENT '个人缴纳金额',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_scheme_id` (`scheme_id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 社保方案项目';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment`
--

DROP TABLE IF EXISTS `hrm_performance_assessment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工绩效考核编号',
  `plan_id` bigint DEFAULT NULL COMMENT '绩效计划编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `status` int DEFAULT NULL COMMENT '考核状态 枚举',
  `process_status` int DEFAULT NULL COMMENT '流程处理状态 枚举',
  `stage_type` int DEFAULT NULL COMMENT '当前业务阶段类型 枚举',
  `stage_sort` int DEFAULT NULL COMMENT '当前阶段排序',
  `score` decimal(12,2) DEFAULT NULL COMMENT '综合得分',
  `result_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果等级',
  `coefficient` decimal(10,4) DEFAULT NULL COMMENT '绩效系数',
  `target_confirmation_result` int DEFAULT NULL COMMENT '目标确认结果 枚举',
  `target_confirmation_comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标确认意见',
  `target_confirmation_time` datetime DEFAULT NULL COMMENT '目标确认时间',
  `self_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '自评说明',
  `reviewer_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '他评说明',
  `result_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '结果说明',
  `result_confirmation_time` datetime DEFAULT NULL COMMENT '结果确认时间',
  `result_audit_status` int DEFAULT NULL COMMENT '结果审核状态 枚举',
  `result_audit_time` datetime DEFAULT NULL COMMENT '结果审核时间',
  `result_audit_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果审核意见',
  `appeal_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申诉原因',
  `appeal_file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申诉附件地址列表',
  `appeal_submit_time` datetime DEFAULT NULL COMMENT '申诉提交时间',
  `appeal_status` int DEFAULT NULL COMMENT '申诉状态 枚举',
  `appeal_time` datetime DEFAULT NULL COMMENT '申诉完成时间',
  `appeal_comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申诉处理意见',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_plan_id` (`plan_id`) USING BTREE,
  KEY `idx_archive_time` (`archive_time`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_action_record`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_action_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_action_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动作记录编号',
  `assessment_id` bigint DEFAULT NULL COMMENT '员工绩效考核编号',
  `stage_id` bigint DEFAULT NULL COMMENT '员工绩效考核阶段编号',
  `employee_id` bigint DEFAULT NULL COMMENT '操作员工编号',
  `type` int DEFAULT NULL COMMENT '动作类型 枚举',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '动作标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '动作内容',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `status` int DEFAULT NULL COMMENT '动作完成后的阶段状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_assessment_id` (`assessment_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核动作记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_appeal_record`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_appeal_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_appeal_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申诉记录编号',
  `assessment_id` bigint DEFAULT NULL COMMENT '员工绩效考核编号',
  `stage_id` bigint DEFAULT NULL COMMENT '员工绩效考核阶段编号',
  `status` int DEFAULT NULL COMMENT '处理状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_assessment_id` (`assessment_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核申诉记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_dimension`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_dimension`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_dimension` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核维度编号',
  `assessment_id` bigint DEFAULT NULL COMMENT '员工绩效考核编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '维度名称',
  `quota_type` int DEFAULT NULL COMMENT '指标类型 枚举',
  `weight` decimal(10,4) DEFAULT NULL COMMENT '维度权重',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `allow_edit` bit(1) DEFAULT NULL COMMENT '是否允许员工编辑指标',
  `sort` int DEFAULT NULL COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_assessment_id` (`assessment_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核维度';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_quota`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_quota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_quota` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核指标编号',
  `assessment_id` bigint DEFAULT NULL COMMENT '员工绩效考核编号',
  `dimension_id` bigint DEFAULT NULL COMMENT '考核维度编号',
  `preset` bit(1) DEFAULT NULL COMMENT '是否预置指标',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '指标名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '指标说明',
  `standard` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评分标准',
  `weight` decimal(10,4) DEFAULT NULL COMMENT '指标权重',
  `score_type` int DEFAULT NULL COMMENT '评分类型 枚举',
  `target_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '目标值',
  `actual_value` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际值',
  `self_score` decimal(12,2) DEFAULT NULL COMMENT '自评汇总分',
  `reviewer_score` decimal(12,2) DEFAULT NULL COMMENT '他评汇总分',
  `final_score` decimal(12,2) DEFAULT NULL COMMENT '最终指标得分',
  `sort` int DEFAULT NULL COMMENT '排序',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_assessment_id` (`assessment_id`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核指标';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_quota_score`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_quota_score`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_quota_score` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核指标评分编号',
  `assessment_stage_id` bigint DEFAULT NULL COMMENT '员工绩效考核阶段编号',
  `assessment_quota_id` bigint DEFAULT NULL COMMENT '员工绩效考核指标编号',
  `score` decimal(12,2) DEFAULT NULL COMMENT '指标得分',
  `comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '指标评语',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_assessment_stage_id` (`assessment_stage_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核指标评分';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_stage`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考核阶段编号',
  `assessment_id` bigint DEFAULT NULL COMMENT '员工绩效考核编号',
  `type` int DEFAULT NULL COMMENT '业务阶段类型 枚举',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段名称',
  `handler_employee_id` bigint DEFAULT NULL COMMENT '实际处理员工编号',
  `rater_type` int DEFAULT NULL COMMENT '评分人类型 枚举',
  `weight` decimal(10,4) DEFAULT NULL COMMENT '评分权重快照',
  `scoring_type` int DEFAULT NULL COMMENT '评分方式快照 枚举',
  `visible_content` int DEFAULT NULL COMMENT '评分内容可见范围快照 枚举',
  `required_setting` bit(1) DEFAULT NULL COMMENT '评语是否必填',
  `reject_authority` bit(1) DEFAULT NULL COMMENT '是否允许驳回',
  `sort` int DEFAULT NULL COMMENT '排序',
  `status` int DEFAULT NULL COMMENT '阶段处理状态 枚举',
  `score` decimal(12,2) DEFAULT NULL COMMENT '阶段汇总得分',
  `result_level` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段结果等级',
  `comment` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '阶段意见',
  `reject_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '最近一次驳回原因',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_assessment_id` (`assessment_id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工绩效考核阶段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_assessment_template`
--

DROP TABLE IF EXISTS `hrm_performance_assessment_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_assessment_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绩效考核模板编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板名称',
  `illustrate` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '模板说明',
  `score_calculation` int DEFAULT NULL COMMENT '计分方式 枚举',
  `upper_limit_type` int DEFAULT NULL COMMENT '分数上限类型 枚举',
  `upper_limit_score` decimal(12,2) DEFAULT NULL COMMENT '分数上限',
  `dimension_count` int DEFAULT NULL COMMENT '维度数量',
  `quota_count` int DEFAULT NULL COMMENT '指标数量',
  `dimensions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '考核维度配置',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 绩效考核模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_plan`
--

DROP TABLE IF EXISTS `hrm_performance_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计划编号',
  `status` int DEFAULT NULL COMMENT '计划状态 枚举',
  `stage_type` int DEFAULT NULL COMMENT '当前业务阶段类型 枚举',
  `operation_type` int DEFAULT NULL COMMENT '当前可执行操作类型 枚举',
  `terminate_time` datetime DEFAULT NULL COMMENT '终止时间',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计划名称',
  `cycle_type` int DEFAULT NULL COMMENT '考核周期类型 枚举',
  `cycle` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考核周期',
  `quarter` int DEFAULT NULL COMMENT '季度 枚举',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '考核说明',
  `scopes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '考评范围列表',
  `assessment_template_id` bigint DEFAULT NULL COMMENT '考核模板编号',
  `assessment_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '考核配置快照',
  `quota_setting_type` int DEFAULT NULL COMMENT '指标制定类型 枚举',
  `target_confirmation` bit(1) DEFAULT NULL COMMENT '是否开启目标确认',
  `target_confirmation_stage` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '目标确认节点',
  `review_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '评分阶段列表',
  `result_audit` bit(1) DEFAULT NULL COMMENT '是否开启结果审核',
  `result_audit_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '结果审核节点列表',
  `result_confirmation` bit(1) DEFAULT NULL COMMENT '是否开启结果确认',
  `appeal_stages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申诉处理节点列表',
  `appeal_timeout_days` int DEFAULT NULL COMMENT '申诉超期天数',
  `appeal_timeout_action` int DEFAULT NULL COMMENT '申诉超期处理动作 枚举',
  `result_template_id` bigint DEFAULT NULL COMMENT '结果模板编号',
  `result_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '结果配置快照',
  `sync_to_salary` bit(1) DEFAULT NULL COMMENT '是否同步到薪资',
  `paid_for_month` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计薪月份',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_paid_for_month` (`paid_for_month`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 绩效考核计划';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_performance_result_template`
--

DROP TABLE IF EXISTS `hrm_performance_result_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_performance_result_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绩效结果模板编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板名称',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `levels` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '结果等级配置',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_update_time` (`update_time`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 绩效结果模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_recruit_candidate`
--

DROP TABLE IF EXISTS `hrm_recruit_candidate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_recruit_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '候选人编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '候选人姓名',
  `mobile` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号码',
  `sex` int DEFAULT NULL COMMENT '性别 枚举',
  `age` int DEFAULT NULL COMMENT '年龄',
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `post_id` bigint DEFAULT NULL COMMENT '应聘职位编号',
  `stage_number` int DEFAULT NULL COMMENT '面试轮次',
  `work_time` int DEFAULT NULL COMMENT '工作年限，单位：年',
  `education` int DEFAULT NULL COMMENT '学历 字典',
  `graduate_school` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '毕业院校',
  `latest_work_place` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最近工作单位',
  `channel_id` bigint DEFAULT NULL COMMENT '招聘渠道编号',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `status` int DEFAULT NULL COMMENT '候选人状态 枚举  字典',
  `eliminate` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '淘汰原因',
  `status_update_time` datetime DEFAULT NULL COMMENT '状态更新时间',
  `entry_time` datetime DEFAULT NULL COMMENT '入职时间',
  `resume_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '简历附件地址数组',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_status_update_time` (`status_update_time`) USING BTREE,
  KEY `idx_channel_id` (`channel_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 招聘候选人';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_recruit_channel`
--

DROP TABLE IF EXISTS `hrm_recruit_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_recruit_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘渠道编号',
  `system_flag` bit(1) DEFAULT NULL COMMENT '是否系统内置',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '渠道名称',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 招聘渠道';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_recruit_interview`
--

DROP TABLE IF EXISTS `hrm_recruit_interview`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_recruit_interview` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '面试编号',
  `candidate_id` bigint DEFAULT NULL COMMENT '候选人编号',
  `type` int DEFAULT NULL COMMENT '面试方式 字典',
  `stage_number` int DEFAULT NULL COMMENT '面试轮次',
  `interview_employee_id` bigint DEFAULT NULL COMMENT '主面试官员工编号',
  `other_interview_employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '其他面试官员工编号数组',
  `interview_time` datetime DEFAULT NULL COMMENT '面试时间',
  `address` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '面试地址',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `result` int DEFAULT NULL COMMENT '面试结果 枚举  字典',
  `evaluate` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '面试评价',
  `cancel_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '取消原因',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_candidate_id` (`candidate_id`) USING BTREE,
  KEY `idx_stage_number` (`stage_number`) USING BTREE,
  KEY `idx_interview_time` (`interview_time`) USING BTREE,
  KEY `idx_result` (`result`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 招聘面试记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_recruit_post`
--

DROP TABLE IF EXISTS `hrm_recruit_post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_recruit_post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘职位编号',
  `post_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位名称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门编号',
  `job_nature` int DEFAULT NULL COMMENT '工作性质 字典',
  `area_id` int DEFAULT NULL COMMENT '工作城市地区编号',
  `recruit_num` int DEFAULT NULL COMMENT '招聘人数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '招聘原因',
  `work_time` int DEFAULT NULL COMMENT '工作经验要求 字典',
  `education_require` int DEFAULT NULL COMMENT '学历要求 字典',
  `min_salary` decimal(12,2) DEFAULT NULL COMMENT '最低薪资 -1 表示面议',
  `max_salary` decimal(12,2) DEFAULT NULL COMMENT '最高薪资 -1 表示面议',
  `salary_unit` int DEFAULT NULL COMMENT '薪资单位 字典',
  `min_age` int DEFAULT NULL COMMENT '最小年龄 -1 表示不限',
  `max_age` int DEFAULT NULL COMMENT '最大年龄 -1 表示不限',
  `latest_entry_time` datetime DEFAULT NULL COMMENT '最迟到岗时间',
  `owner_employee_id` bigint DEFAULT NULL COMMENT '招聘负责人员工编号',
  `interview_employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '面试官员工编号数组',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '职位描述',
  `emergency_level` int DEFAULT NULL COMMENT '紧急程度 字典',
  `post_type_id` bigint DEFAULT NULL COMMENT '职位类型编号',
  `status` int DEFAULT NULL COMMENT '职位状态 枚举  字典',
  `stop_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '停止原因',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_owner_employee_id` (`owner_employee_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 招聘职位';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_recruit_post_type`
--

DROP TABLE IF EXISTS `hrm_recruit_post_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_recruit_post_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位类型编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父类型编号',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_sort` (`sort`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 招聘职位类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_change_record`
--

DROP TABLE IF EXISTS `hrm_salary_change_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `type` int DEFAULT NULL COMMENT '记录类型 枚举',
  `reason` int DEFAULT NULL COMMENT '调整原因 枚举',
  `effect_time` datetime DEFAULT NULL COMMENT '生效时间',
  `before_total` decimal(12,2) DEFAULT NULL COMMENT '调整前正式薪资',
  `after_total` decimal(12,2) DEFAULT NULL COMMENT '调整后正式薪资',
  `probation_before_total` decimal(12,2) DEFAULT NULL COMMENT '调整前试用期薪资',
  `probation_after_total` decimal(12,2) DEFAULT NULL COMMENT '调整后试用期薪资',
  `salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '正式薪资项后态快照',
  `probation_salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '试用期薪资项后态快照',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_effect_time` (`effect_time`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 定薪/调薪记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_change_template`
--

DROP TABLE IF EXISTS `hrm_salary_change_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_change_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调薪模板编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板名称',
  `default_status` bit(1) DEFAULT NULL COMMENT '是否默认模板',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '调薪项配置',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 调薪模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_config`
--

DROP TABLE IF EXISTS `hrm_salary_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cycle_start_day` int DEFAULT NULL COMMENT '计薪周期开始日',
  `cycle_end_day` int DEFAULT NULL COMMENT '计薪周期结束日',
  `social_security_month_type` int DEFAULT NULL COMMENT '社保对应月份类型 枚举',
  `start_year` int DEFAULT NULL COMMENT '工资开始年份',
  `start_month` int DEFAULT NULL COMMENT '工资开始月份',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 计薪配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_employee_info`
--

DROP TABLE IF EXISTS `hrm_salary_employee_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_employee_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `change_reason` int DEFAULT NULL COMMENT '定薪、调薪原因 枚举',
  `effect_time` datetime DEFAULT NULL COMMENT '生效时间',
  `change_type` int DEFAULT NULL COMMENT '薪资信息变更类型 枚举',
  `probation_salary` decimal(12,2) DEFAULT NULL COMMENT '试用期工资',
  `regular_salary` decimal(12,2) DEFAULT NULL COMMENT '正式工资',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '正式薪资项 薪资项随员工薪资信息整体维护',
  `probation_salary_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '试用期薪资项 薪资项随员工薪资信息整体维护',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_change_type` (`change_type`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工薪资信息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_group`
--

DROP TABLE IF EXISTS `hrm_salary_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_group` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '薪资组名称',
  `salary_standard` decimal(12,2) DEFAULT NULL COMMENT '月计薪标准',
  `change_rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '转正、调薪月规则',
  `dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '适用部门编号列表',
  `employee_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '适用员工编号列表',
  `tax_rule_id` bigint DEFAULT NULL COMMENT '计税规则编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tax_rule_id` (`tax_rule_id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 薪资组';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_month_employee_record`
--

DROP TABLE IF EXISTS `hrm_salary_month_employee_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_month_employee_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `month_record_id` bigint DEFAULT NULL COMMENT '月度工资表编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `actual_work_day` decimal(10,4) DEFAULT NULL COMMENT '实际工作天数',
  `need_work_day` decimal(10,4) DEFAULT NULL COMMENT '应工作天数',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` int DEFAULT NULL COMMENT '月份',
  `expected_pay_salary` decimal(12,2) DEFAULT NULL COMMENT '应发工资',
  `taxable_salary` decimal(12,2) DEFAULT NULL COMMENT '应税工资',
  `personal_tax` decimal(12,2) DEFAULT NULL COMMENT '个人所得税',
  `real_pay_salary` decimal(12,2) DEFAULT NULL COMMENT '实发工资',
  `option_values` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '薪资项值快照 薪资项值随员工月度工资记录整体维护',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_month_record_id` (`month_record_id`) USING BTREE,
  KEY `idx_year` (`year`) USING BTREE,
  KEY `idx_month` (`month`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 员工月度工资记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_month_record`
--

DROP TABLE IF EXISTS `hrm_salary_month_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工资表标题',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` int DEFAULT NULL COMMENT '月份',
  `employee_count` int DEFAULT NULL COMMENT '员工人数',
  `start_time` datetime DEFAULT NULL COMMENT '计薪开始日期',
  `end_time` datetime DEFAULT NULL COMMENT '计薪结束日期',
  `expected_pay_salary` decimal(12,2) DEFAULT NULL COMMENT '应发工资',
  `personal_tax` decimal(12,2) DEFAULT NULL COMMENT '个人所得税',
  `real_pay_salary` decimal(12,2) DEFAULT NULL COMMENT '实发工资',
  `personal_insurance_amount` decimal(12,2) DEFAULT NULL COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(12,2) DEFAULT NULL COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(12,2) DEFAULT NULL COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(12,2) DEFAULT NULL COMMENT '公司公积金金额',
  `option_headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '薪资项表头快照',
  `status` int DEFAULT NULL COMMENT '状态 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_year` (`year`) USING BTREE,
  KEY `idx_month` (`month`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 月度工资表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_option`
--

DROP TABLE IF EXISTS `hrm_salary_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '薪资项编号',
  `template_id` bigint DEFAULT NULL COMMENT '标准薪资项目录编号（可选）',
  `code` int DEFAULT NULL COMMENT '薪资项编码',
  `parent_code` int DEFAULT NULL COMMENT '父薪资项编码',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '薪资项名称',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `system_flag` bit(1) DEFAULT NULL COMMENT '是否系统默认项',
  `type` int DEFAULT NULL COMMENT '薪资项类型 枚举',
  `visible` bit(1) DEFAULT NULL COMMENT '是否显示',
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `tax_enabled` bit(1) DEFAULT NULL COMMENT '是否计税',
  `calculate_enabled` bit(1) DEFAULT NULL COMMENT '是否参与计算',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_code` (`code`) USING BTREE,
  KEY `idx_enabled` (`enabled`) USING BTREE,
  KEY `idx_visible` (`visible`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 工资表薪资项';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_option_template`
--

DROP TABLE IF EXISTS `hrm_salary_option_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_option_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标准薪资项编号',
  `code` int DEFAULT NULL COMMENT '薪资项编码',
  `parent_code` int DEFAULT NULL COMMENT '父薪资项编码',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '薪资项名称',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `system_flag` bit(1) DEFAULT NULL COMMENT '是否系统默认项',
  `type` int DEFAULT NULL COMMENT '薪资项类型 枚举',
  `visible` bit(1) DEFAULT NULL COMMENT '是否显示',
  `tax_enabled` bit(1) DEFAULT NULL COMMENT '是否计税',
  `calculate_enabled` bit(1) DEFAULT NULL COMMENT '是否参与计算',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_code` (`code`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 标准薪资项目录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_slip`
--

DROP TABLE IF EXISTS `hrm_salary_slip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_slip` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `send_record_id` bigint DEFAULT NULL COMMENT '工资条发放记录编号',
  `month_employee_record_id` bigint DEFAULT NULL COMMENT '员工月度工资记录编号',
  `employee_id` bigint DEFAULT NULL COMMENT '员工编号',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` int DEFAULT NULL COMMENT '月份',
  `read_status` int DEFAULT NULL COMMENT '阅读状态 枚举',
  `real_pay_salary` decimal(12,2) DEFAULT NULL COMMENT '实发工资',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工资条项 工资条项随员工工资条整体维护，通过  表达父子关系',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_employee_id` (`employee_id`) USING BTREE,
  KEY `idx_send_record_id` (`send_record_id`) USING BTREE,
  KEY `idx_read_status` (`read_status`) USING BTREE,
  KEY `idx_month_employee_record_id` (`month_employee_record_id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 工资条';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_slip_send_record`
--

DROP TABLE IF EXISTS `hrm_salary_slip_send_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_slip_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `month_record_id` bigint DEFAULT NULL COMMENT '月度工资表编号',
  `employee_count` int DEFAULT NULL COMMENT '工资表总人数',
  `send_employee_count` int DEFAULT NULL COMMENT '本批次发放人数',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` int DEFAULT NULL COMMENT '月份',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_year` (`year`) USING BTREE,
  KEY `idx_month` (`month`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 工资条发放记录 DO 同一月度工资表可以分多批次发放工资条，因此发放记录保持独立实体。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_slip_template`
--

DROP TABLE IF EXISTS `hrm_salary_slip_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_slip_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板名称',
  `hide_empty` bit(1) DEFAULT NULL COMMENT '是否隐藏空项',
  `default_status` bit(1) DEFAULT NULL COMMENT '是否默认模板',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工资条模板项 模板项随工资条模板整体维护，通过  表达父子关系',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_default_status` (`default_status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 工资条模板 DO 平台级默认模板保存在 {@link cn.iocoder.yudao.framework.te';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hrm_salary_tax_rule`
--

DROP TABLE IF EXISTS `hrm_salary_tax_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hrm_salary_tax_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '计税规则名称',
  `type` int DEFAULT NULL COMMENT '计税类型 枚举',
  `tax_enabled` bit(1) DEFAULT NULL COMMENT '是否计税',
  `threshold` decimal(12,2) DEFAULT NULL COMMENT '起征阈值',
  `decimal_scale` int DEFAULT NULL COMMENT '小数位数',
  `cycle_type` int DEFAULT NULL COMMENT '计税周期类型 枚举',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HRM 计税规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `milestones`
--

DROP TABLE IF EXISTS `milestones`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `milestones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `type` varchar(20) NOT NULL COMMENT 'birthday/anniversary/promotion/probation',
  `milestone_date` date NOT NULL,
  `title` varchar(200) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`milestone_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='个人里程碑';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人',
  `category` varchar(20) NOT NULL COMMENT 'todo / milestone / compliance / corp',
  `type` varchar(40) NOT NULL DEFAULT '' COMMENT '具体类型，如 report_due',
  `title` varchar(200) NOT NULL,
  `link` varchar(255) DEFAULT NULL COMMENT '一键跳转到对应模块',
  `read_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读',
  `channel` varchar(20) NOT NULL DEFAULT 'site' COMMENT 'site / wecom',
  `dedup_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '去重键，同一用户同键只保留一条',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dedup` (`user_id`,`dedup_key`),
  KEY `idx_user_read` (`user_id`,`read_flag`),
  KEY `idx_user_created` (`user_id`,`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='我的提醒';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notify_preferences`
--

DROP TABLE IF EXISTS `notify_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notify_preferences` (
  `user_id` bigint NOT NULL,
  `report_due` tinyint(1) NOT NULL DEFAULT '1',
  `overdue` tinyint(1) NOT NULL DEFAULT '1',
  `milestone` tinyint(1) NOT NULL DEFAULT '1',
  `corp_news` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒偏好';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_announcement`
--

DROP TABLE IF EXISTS `oa_announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告编号',
  `type` int DEFAULT NULL COMMENT '公告类型',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '内容',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '相关链接',
  `top` bit(1) DEFAULT NULL COMMENT '是否置顶',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_priority` (`priority`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_announcement_receiver`
--

DROP TABLE IF EXISTS `oa_announcement_receiver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_announcement_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接收关系编号',
  `announcement_id` bigint DEFAULT NULL COMMENT '公告编号',
  `receiver_user_id` bigint DEFAULT NULL COMMENT '接收人用户编号',
  `read_status` bit(1) DEFAULT NULL COMMENT '是否已读',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_announcement_id` (`announcement_id`) USING BTREE,
  KEY `idx_receiver_user_id` (`receiver_user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 公告接收关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_attendance`
--

DROP TABLE IF EXISTS `oa_attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户编号',
  `type` int DEFAULT NULL COMMENT '考勤类型',
  `status` int DEFAULT NULL COMMENT '考勤状态',
  `attendance_time` datetime DEFAULT NULL COMMENT '考勤时间',
  `attendance_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考勤 IP',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '考勤备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_attendance_time` (`attendance_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 考勤表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_leave_apply`
--

DROP TABLE IF EXISTS `oa_leave_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_leave_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请标题',
  `urgency` int DEFAULT NULL COMMENT '紧急程度',
  `type` int DEFAULT NULL COMMENT '请假类型',
  `start_time` datetime DEFAULT NULL COMMENT '请假开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '请假结束时间',
  `days` int DEFAULT NULL COMMENT '请假天数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请原因',
  `approvers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '自选审批人的员工编号 JSON 数组',
  `status` int DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `wecom_approval_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企微审批单号 sp_no',
  `wecom_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '提交企微的幂等请求号',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_wecom_request_id` (`wecom_request_id`),
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_process_instance_id` (`process_instance_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 请假申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_meeting_room`
--

DROP TABLE IF EXISTS `oa_meeting_room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_meeting_room` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会议室名称',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会议室位置',
  `type` int DEFAULT NULL COMMENT '会议室类型',
  `manager_user_id` bigint DEFAULT NULL COMMENT '负责人用户编号',
  `status` int DEFAULT NULL COMMENT '可用状态',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会议室图片 URL',
  `seat_count` int DEFAULT NULL COMMENT '坐席数',
  `equipments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '设备列表',
  `allow_booking` bit(1) DEFAULT NULL COMMENT '是否允许预定',
  `need_approval` bit(1) DEFAULT NULL COMMENT '预定是否需要审批',
  `booking_scope` int DEFAULT NULL COMMENT '可预定范围',
  `booking_user_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '可预定用户编号列表',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 会议室表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_meeting_room_booking`
--

DROP TABLE IF EXISTS `oa_meeting_room_booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_meeting_room_booking` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预定申请单号',
  `room_id` bigint DEFAULT NULL COMMENT '会议室编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会议主题',
  `start_time` datetime DEFAULT NULL COMMENT '会议开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '会议结束时间',
  `moderator_user_id` bigint DEFAULT NULL COMMENT '主持人用户编号',
  `attendee_user_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '参会用户编号列表',
  `reminder_type` int DEFAULT NULL COMMENT '会议提醒类型',
  `reminded` bit(1) DEFAULT NULL COMMENT '是否已发送会议开始提醒',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '会议说明',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `status` int DEFAULT NULL COMMENT '审批状态，无需审批时提交后直接通过',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号，无需审批时为空',
  `need_approval` bit(1) DEFAULT NULL COMMENT '提交时是否需要审批',
  `use_status` int DEFAULT NULL COMMENT '使用状态',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_room_id_start_time` (`room_id`,`start_time`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_use_status` (`use_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 会议室预定表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_official_doc_receive`
--

DROP TABLE IF EXISTS `oa_official_doc_receive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_official_doc_receive` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单据编号',
  `send_id` bigint DEFAULT NULL COMMENT '来源发文编号，手工新增收文时为空',
  `receive_type` int DEFAULT NULL COMMENT '收文类型',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `document_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来文字号',
  `secrecy_level` int DEFAULT NULL COMMENT '密级',
  `urgency_level` int DEFAULT NULL COMMENT '紧急程度',
  `receive_time` datetime DEFAULT NULL COMMENT '收文时间',
  `receive_dept_id` bigint DEFAULT NULL COMMENT '收文部门编号',
  `handler_user_id` bigint DEFAULT NULL COMMENT '主办人用户编号',
  `instruction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '领导批示',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '办理结果',
  `deadline_time` datetime DEFAULT NULL COMMENT '办理期限',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '内容摘要',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `formal_file_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '正式公文地址',
  `handle_status` int DEFAULT NULL COMMENT '办理状态',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_send_id` (`send_id`) USING BTREE,
  KEY `idx_receive_dept_id` (`receive_dept_id`) USING BTREE,
  KEY `idx_handle_status` (`handle_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 公文收文表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_official_doc_send`
--

DROP TABLE IF EXISTS `oa_official_doc_send`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_official_doc_send` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单据编号',
  `template_id` bigint DEFAULT NULL COMMENT '套红模板编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `no_prefix` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发文字号前缀',
  `year` int DEFAULT NULL COMMENT '发文年度',
  `sequence` int DEFAULT NULL COMMENT '发文序号',
  `document_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公文文号',
  `secrecy_level` int DEFAULT NULL COMMENT '密级',
  `urgency_level` int DEFAULT NULL COMMENT '紧急程度',
  `disclosure_type` int DEFAULT NULL COMMENT '公开类别',
  `issue_time` datetime DEFAULT NULL COMMENT '发文时间',
  `send_dept_id` bigint DEFAULT NULL COMMENT '发文部门编号',
  `main_dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '主送部门编号列表',
  `copy_dept_ids` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '抄送部门编号列表',
  `signer_user_id` bigint DEFAULT NULL COMMENT '签发人用户编号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '正文',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `formal_file_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '正式公文地址',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附注',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_document_no` (`document_no`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 公文发文表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_official_doc_template`
--

DROP TABLE IF EXISTS `oa_official_doc_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_official_doc_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板名称',
  `authority_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '红头名称',
  `font_size` int DEFAULT NULL COMMENT '红头名称字号',
  `no_prefix` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发文字号前缀',
  `seal_pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章图片地址',
  `separator_type` int DEFAULT NULL COMMENT '分隔线类型',
  `status` int DEFAULT NULL COMMENT '状态',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 套红模板表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_overtime_apply`
--

DROP TABLE IF EXISTS `oa_overtime_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_overtime_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请标题',
  `urgency` int DEFAULT NULL COMMENT '紧急程度',
  `type` int DEFAULT NULL COMMENT '加班类型',
  `start_time` datetime DEFAULT NULL COMMENT '加班开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '加班结束时间',
  `days` decimal(10,2) DEFAULT NULL COMMENT '加班天数',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请原因',
  `status` int DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 加班申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_regular_apply`
--

DROP TABLE IF EXISTS `oa_regular_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_regular_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请标题',
  `urgency` int DEFAULT NULL COMMENT '紧急程度',
  `start_time` datetime DEFAULT NULL COMMENT '试用开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '试用结束时间',
  `days` int DEFAULT NULL COMMENT '试用天数',
  `experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '试用期或实习期心得体会',
  `understanding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '对本岗位职责要求的理解',
  `growth` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '试用期或实习期的成长',
  `deficiency` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '目前存在的不足',
  `improvement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '本岗位工作改进计划',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '对公司产品的意见及建议',
  `status` int DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 转正申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_reimbursement`
--

DROP TABLE IF EXISTS `oa_reimbursement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_reimbursement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请标题',
  `urgency` int DEFAULT NULL COMMENT '紧急程度',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请原因',
  `witness_user_id` bigint DEFAULT NULL COMMENT '证明人用户编号',
  `customer_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '相关客户名称',
  `payment_method` int DEFAULT NULL COMMENT '报销方式',
  `invoice_count` int DEFAULT NULL COMMENT '票据总数',
  `total_price` decimal(10,2) DEFAULT NULL COMMENT '报销总金额，单位：元',
  `status` int DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '报销明细',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_process_instance_id` (`process_instance_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 费用报销表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_resign_apply`
--

DROP TABLE IF EXISTS `oa_resign_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_resign_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请标题',
  `urgency` int DEFAULT NULL COMMENT '紧急程度',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请原因',
  `handover_user_id` bigint DEFAULT NULL COMMENT '工作交接人用户编号',
  `unfinished_work` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '未完成事宜',
  `has_pending_reimbursement` bit(1) DEFAULT NULL COMMENT '是否有未完成的费用报销',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请人的意见及建议',
  `status` int DEFAULT NULL COMMENT '审批状态，未开始表示尚未提交的草稿',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 离职申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_schedule`
--

DROP TABLE IF EXISTS `oa_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日程编号',
  `type` int DEFAULT NULL COMMENT '日程类型',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '描述',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `remind` bit(1) DEFAULT NULL COMMENT '是否提醒',
  `reminded` bit(1) DEFAULT NULL COMMENT '是否已提醒',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 日程表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_schedule_participant`
--

DROP TABLE IF EXISTS `oa_schedule_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_schedule_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参与记录编号',
  `schedule_id` bigint DEFAULT NULL COMMENT '日程编号',
  `user_id` bigint DEFAULT NULL COMMENT '参与人用户编号',
  `read_status` bit(1) DEFAULT NULL COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '首次阅读时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_schedule_id` (`schedule_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 日程参与人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_seal`
--

DROP TABLE IF EXISTS `oa_seal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_seal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章名称',
  `type` int DEFAULT NULL COMMENT '印章类型',
  `category` int DEFAULT NULL COMMENT '印章分类',
  `keeper_user_id` bigint DEFAULT NULL COMMENT '保管人用户编号',
  `keeper_dept_id` bigint DEFAULT NULL COMMENT '保管部门编号',
  `status` int DEFAULT NULL COMMENT '印章台账状态',
  `purchase_time` datetime DEFAULT NULL COMMENT '购买时间',
  `enable_time` datetime DEFAULT NULL COMMENT '启用时间',
  `disable_time` datetime DEFAULT NULL COMMENT '停用时间',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章照片地址',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_keeper_user_id` (`keeper_user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 印章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_seal_apply`
--

DROP TABLE IF EXISTS `oa_seal_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_seal_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请单号',
  `user_id` bigint DEFAULT NULL COMMENT '申请人用户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `seal_id` bigint DEFAULT NULL COMMENT '印章编号',
  `seal_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章编号快照',
  `seal_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '印章名称快照',
  `seal_type` int DEFAULT NULL COMMENT '印章类型快照',
  `keeper_dept_id` bigint DEFAULT NULL COMMENT '保管部门编号快照',
  `keeper_user_id` bigint DEFAULT NULL COMMENT '保管人用户编号快照',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用印事由',
  `type` int DEFAULT NULL COMMENT '用印类型',
  `mode` int DEFAULT NULL COMMENT '用印方式',
  `document_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件标题',
  `document_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `document_count` int DEFAULT NULL COMMENT '文件份数',
  `contract_price` decimal(10,2) DEFAULT NULL COMMENT '合同金额',
  `contract_party` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '合同对方',
  `expected_use_time` datetime DEFAULT NULL COMMENT '预计用印时间',
  `actual_use_time` datetime DEFAULT NULL COMMENT '实际用印时间',
  `expected_return_time` datetime DEFAULT NULL COMMENT '预计归还时间',
  `actual_return_time` datetime DEFAULT NULL COMMENT '实际归还时间',
  `use_status` int DEFAULT NULL COMMENT '用印业务状态',
  `status` int DEFAULT NULL COMMENT '审批状态，取 BPM 流程实例状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `urgent` bit(1) DEFAULT NULL COMMENT '是否紧急',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_seal_id` (`seal_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 用印申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_supply_apply`
--

DROP TABLE IF EXISTS `oa_supply_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_supply_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请单号',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `apply_time` datetime DEFAULT NULL COMMENT '领用日期',
  `use_type` int DEFAULT NULL COMMENT '使用类型',
  `pickup_method` int DEFAULT NULL COMMENT '领取方式',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '申请事由',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 用品领用申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_supply_apply_item`
--

DROP TABLE IF EXISTS `oa_supply_apply_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_supply_apply_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `apply_id` bigint DEFAULT NULL COMMENT '领用申请编号',
  `item_id` bigint DEFAULT NULL COMMENT '办公用品编号',
  `item_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物品名称快照',
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号快照',
  `unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计量单位快照',
  `manage_type` int DEFAULT NULL COMMENT '管理类型快照',
  `apply_quantity` int DEFAULT NULL COMMENT '申请数量',
  `issued_quantity` int DEFAULT NULL COMMENT '实发数量',
  `returned_quantity` int DEFAULT NULL COMMENT '累计归还数量',
  `status` int DEFAULT NULL COMMENT '明细状态',
  `issue_user_id` bigint DEFAULT NULL COMMENT '发放人用户编号',
  `issue_time` datetime DEFAULT NULL COMMENT '发放时间',
  `issue_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发放备注',
  `return_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '归还备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_apply_id` (`apply_id`) USING BTREE,
  KEY `idx_item_id` (`item_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 用品申请明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_supply_item`
--

DROP TABLE IF EXISTS `oa_supply_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_supply_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门编号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物品名称',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物品编码',
  `category` int DEFAULT NULL COMMENT '类别',
  `manage_type` int DEFAULT NULL COMMENT '管理类型',
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '计量单位',
  `reference_price` decimal(10,2) DEFAULT NULL COMMENT '参考单价',
  `stock_quantity` int DEFAULT NULL COMMENT '库存数量',
  `min_stock_quantity` int DEFAULT NULL COMMENT '最低库存预警',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '物品图片 URL',
  `status` int DEFAULT NULL COMMENT '状态',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 办公用品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_task`
--

DROP TABLE IF EXISTS `oa_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务编号',
  `type` int DEFAULT NULL COMMENT '任务类型',
  `status` int DEFAULT NULL COMMENT '总体状态',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '任务描述',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '任务评价',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `publish_time` datetime DEFAULT NULL COMMENT '业务发布时间，新增及编辑任务时刷新',
  `top` bit(1) DEFAULT NULL COMMENT '是否置顶',
  `canceled` bit(1) DEFAULT NULL COMMENT '是否取消',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_task_log`
--

DROP TABLE IF EXISTS `oa_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `task_id` bigint DEFAULT NULL COMMENT '任务编号',
  `user_id` bigint DEFAULT NULL COMMENT '反馈人用户编号',
  `status` int DEFAULT NULL COMMENT '变更后的状态',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '反馈内容',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_id` (`task_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 任务反馈日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_task_receiver`
--

DROP TABLE IF EXISTS `oa_task_receiver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_task_receiver` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `task_id` bigint DEFAULT NULL COMMENT '任务编号',
  `user_id` bigint DEFAULT NULL COMMENT '接收人用户编号',
  `status` int DEFAULT NULL COMMENT '任务状态',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_task_id_user_id` (`task_id`,`user_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 任务接收人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_travel_apply`
--

DROP TABLE IF EXISTS `oa_travel_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_travel_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单据编号',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '出差事由',
  `start_time` datetime DEFAULT NULL COMMENT '出差开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '出差结束时间',
  `days` int DEFAULT NULL COMMENT '出差天数',
  `companion` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '同行人',
  `estimated_price` decimal(10,2) DEFAULT NULL COMMENT '预计费用，单位：元',
  `reimburse_status` bit(1) DEFAULT NULL COMMENT '是否已报销',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '行程明细',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 出差申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_travel_reimbursement`
--

DROP TABLE IF EXISTS `oa_travel_reimbursement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_travel_reimbursement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '单据编号',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `travel_apply_id` bigint DEFAULT NULL COMMENT '关联出差申请编号，未关联申请时为空',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '出差事由',
  `start_time` datetime DEFAULT NULL COMMENT '出差开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '出差结束时间',
  `days` int DEFAULT NULL COMMENT '出差天数',
  `total_price` decimal(10,2) DEFAULT NULL COMMENT '报销总金额，单位：元',
  `pay_status` bit(1) DEFAULT NULL COMMENT '是否已支付',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '费用明细',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_travel_apply_id` (`travel_apply_id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 出差报销表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_vehicle`
--

DROP TABLE IF EXISTS `oa_vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_vehicle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车牌号',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车辆名称',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门编号',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车型',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车辆分类',
  `brand_model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌型号',
  `seat_count` int DEFAULT NULL COMMENT '座位数',
  `bare_price` decimal(10,2) DEFAULT NULL COMMENT '裸车价格（元）',
  `compulsory_insurance_expire_time` datetime DEFAULT NULL COMMENT '交强险到期时间',
  `commercial_insurance_expire_time` datetime DEFAULT NULL COMMENT '商业险到期时间',
  `inspection_expire_time` datetime DEFAULT NULL COMMENT '年检到期时间',
  `pic_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车辆照片 URL',
  `status` int DEFAULT NULL COMMENT '车辆台账状态',
  `sort` int DEFAULT NULL COMMENT '显示顺序',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 车辆表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_vehicle_apply`
--

DROP TABLE IF EXISTS `oa_vehicle_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_vehicle_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用车申请单号',
  `vehicle_id` bigint DEFAULT NULL COMMENT '车辆编号',
  `vehicle_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车牌号快照',
  `user_id` bigint DEFAULT NULL COMMENT '申请人用户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `start_time` datetime DEFAULT NULL COMMENT '预计出车时间',
  `end_time` datetime DEFAULT NULL COMMENT '预计回车时间',
  `start_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '出车地点',
  `end_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预计回车地点',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用车事由',
  `passenger` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '随行人',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `return_status` int DEFAULT NULL COMMENT '还车业务状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_vehicle_id` (`vehicle_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 用车申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_vehicle_return`
--

DROP TABLE IF EXISTS `oa_vehicle_return`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_vehicle_return` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '还车申请单号',
  `apply_id` bigint DEFAULT NULL COMMENT '用车申请编号',
  `vehicle_id` bigint DEFAULT NULL COMMENT '车辆编号，从用车申请生成',
  `user_id` bigint DEFAULT NULL COMMENT '申请人用户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门编号',
  `actual_start_time` datetime DEFAULT NULL COMMENT '实际出车时间',
  `start_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际出车地点',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用车事由',
  `passenger` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '随行人',
  `actual_return_time` datetime DEFAULT NULL COMMENT '实际回车时间',
  `return_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '实际回车地点',
  `status` int DEFAULT NULL COMMENT '审批状态',
  `process_instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'BPM 流程实例编号',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '还车说明',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_apply_id` (`apply_id`) USING BTREE,
  KEY `idx_vehicle_id` (`vehicle_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 还车申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_work_report`
--

DROP TABLE IF EXISTS `oa_work_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_work_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '汇报编号',
  `no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '汇报单号',
  `type` int DEFAULT NULL COMMENT '汇报类型',
  `status` int DEFAULT NULL COMMENT '汇报状态',
  `dept_id` bigint DEFAULT NULL COMMENT '汇报人所属部门编号',
  `start_time` datetime DEFAULT NULL COMMENT '周期开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '周期结束时间',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '汇报标题',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工作总结',
  `plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工作计划补充说明',
  `problem` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '问题与协调事项',
  `work_items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '已完成工作项',
  `plan_items` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '工作计划项',
  `file_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '附件地址列表',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_creator` (`creator`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_start_time` (`start_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 工作汇报表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oa_work_report_comment`
--

DROP TABLE IF EXISTS `oa_work_report_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oa_work_report_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点评编号',
  `report_id` bigint NOT NULL COMMENT '工作汇报编号',
  `reviewer_id` bigint NOT NULL COMMENT '点评人用户编号',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '评级：A/B/C',
  `review_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点评正文',
  `reply` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '员工回复',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_report` (`report_id`) USING BTREE,
  KEY `idx_reviewer` (`reviewer_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作汇报点评与回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_categories`
--

DROP TABLE IF EXISTS `portal_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scope` varchar(32) NOT NULL COMMENT '业务归属，点号分层：material.sample / material.office / training.course',
  `name` varchar(100) NOT NULL COMMENT '展示名；物资侧另由注册表限 32（请求体冻结面）',
  `code` varchar(32) DEFAULT NULL COMMENT '程序锚点（路由 slug / seed 幂等键），普通分类留空',
  `parent_id` bigint DEFAULT NULL,
  `depth` smallint NOT NULL DEFAULT '1' COMMENT '1 = 顶层（物资品类、培训大类），2 = 子级（培训小类）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同层展示序，再按 name',
  `attrs` json NOT NULL DEFAULT (json_object()) COMMENT '各业务自己的开关与展示字段，键名按 scope 白名单校验',
  `archived_at` datetime DEFAULT NULL COMMENT 'NULL = 在役；时间戳本身即审计信息，不用 boolean',
  `archived_by` varchar(64) DEFAULT NULL,
  `created_by` varchar(64) NOT NULL COMMENT '创建人 username；迁移 seed 行写 migration',
  `active_name` varchar(100) GENERATED ALWAYS AS (if((`archived_at` is null),`name`,NULL)) STORED COMMENT '在役行才非 NULL，给下面的唯一索引当锚：等价于 PG 的部分唯一索引',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_categories_scope_name_active` (`scope`,`active_name`) COMMENT '同 scope 在役不重名，归档行不占名',
  UNIQUE KEY `uq_categories_scope_code` (`scope`,`code`) COMMENT 'code 含归档行一起唯一（NULL 不碰撞，等价 PG 的 WHERE code IS NOT NULL）',
  KEY `ix_categories_scope_archived_sort` (`scope`,`archived_at`,`sort_order`),
  KEY `ix_categories_parent` (`parent_id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `portal_categories` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ck_categories_depth_parent` CHECK (((`parent_id` is null) or (`depth` > 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全门户共用分类字典：scope + depth + attrs + 归档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_news`
--

DROP TABLE IF EXISTS `portal_news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_news` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `summary` text,
  `content` mediumtext NOT NULL COMMENT '富文本正文（Portal 侧是 Text，给到 MEDIUMTEXT 防长稿截断）',
  `category` varchar(50) DEFAULT NULL COMMENT '文本分类，与 portal_categories 无外键：动态的分类口径独立于物资/培训',
  `cover_image` varchar(255) DEFAULT NULL,
  `template_type` varchar(32) NOT NULL DEFAULT 'simple' COMMENT 'simple / 模板化稿型，模板特有字段在 blocks',
  `blocks` json DEFAULT NULL COMMENT '模板数据块（含 dept 等前端要读的字段）',
  `author` varchar(100) DEFAULT NULL COMMENT '作者展示名快照',
  `author_username` varchar(50) DEFAULT NULL COMMENT '作者 username（= 企微 userid）',
  `status` varchar(20) NOT NULL DEFAULT 'draft',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '精选，前端兼作置顶展示',
  `published_at` datetime DEFAULT NULL,
  `pending_notify_wecom` tinyint(1) NOT NULL DEFAULT '0' COMMENT '作者提交的企微推送意图，审批通过时由审核人二次确认并一次性消费',
  `pending_notify_dept_scope` json DEFAULT NULL COMMENT '推送部门范围，NULL = 全员',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_news_status_published` (`status`,`published_at`),
  KEY `ix_news_author` (`author_username`),
  CONSTRAINT `ck_news_status` CHECK ((`status` in (_utf8mb4'draft',_utf8mb4'pending',_utf8mb4'published',_utf8mb4'rejected')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公司动态：审批状态机 + 模板 blocks + 企微推送意图';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_news_comment`
--

DROP TABLE IF EXISTS `portal_news_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_news_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `news_id` bigint NOT NULL,
  `username` varchar(50) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `content` text NOT NULL COMMENT '软删除后写占位文案 [该评论已删除]，列复用不另开 flag 文案',
  `parent_id` bigint DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_news_comments_news_id` (`news_id`),
  KEY `ix_news_comments_created_at` (`created_at`),
  KEY `ix_news_comments_parent` (`parent_id`),
  CONSTRAINT `fk_news_comment_news` FOREIGN KEY (`news_id`) REFERENCES `portal_news` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_news_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `portal_news_comment` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态评论（含楼中楼），删除是软删';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_news_like`
--

DROP TABLE IF EXISTS `portal_news_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_news_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `news_id` bigint NOT NULL,
  `username` varchar(50) NOT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_news_likes_news_user` (`news_id`,`username`),
  KEY `ix_news_likes_news_id` (`news_id`),
  CONSTRAINT `fk_news_like_news` FOREIGN KEY (`news_id`) REFERENCES `portal_news` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态点赞：每人每动态最多一次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_page_views`
--

DROP TABLE IF EXISTS `portal_page_views`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_page_views` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `page_url` varchar(500) NOT NULL,
  `page_title` varchar(300) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL COMMENT 'NULL = 匿名访问',
  `display_name` varchar(100) DEFAULT NULL COMMENT '打点时的名字快照',
  `department` varchar(50) DEFAULT NULL COMMENT '打点时的主部门名快照',
  `referrer` varchar(500) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `viewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_page_views_page_url` (`page_url`),
  KEY `ix_page_views_username` (`username`),
  KEY `ix_page_views_viewed_at` (`viewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='页面浏览记录：谁看了什么、哪些页面热门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `portal_wiki`
--

DROP TABLE IF EXISTS `portal_wiki`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `portal_wiki` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `content` mediumtext,
  `category` varchar(50) DEFAULT NULL,
  `department` varchar(50) DEFAULT NULL COMMENT '单值归属部门（可见范围口径），不是 portal_categories 的行',
  `doc_url` varchar(500) DEFAULT NULL COMMENT '外链文档地址，正文可为空只给链接',
  `tags` json DEFAULT NULL COMMENT '标签数组（原 PG ARRAY(String)），接口层进出都是数组',
  `author_username` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'draft',
  `view_count` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_wiki_status` (`status`),
  KEY `ix_wiki_author` (`author_username`),
  CONSTRAINT `ck_wiki_status` CHECK ((`status` in (_utf8mb4'draft',_utf8mb4'pending',_utf8mb4'published',_utf8mb4'rejected')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库文档';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recognitions`
--

DROP TABLE IF EXISTS `recognitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recognitions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_id` bigint NOT NULL,
  `to_id` bigint NOT NULL,
  `type` varchar(30) NOT NULL COMMENT 'like/praise/star_nomination',
  `scope` varchar(30) DEFAULT NULL,
  `note` varchar(500) DEFAULT NULL,
  `period` varchar(20) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_to` (`to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收到的认可';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rhythm_node_follow`
--

DROP TABLE IF EXISTS `rhythm_node_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rhythm_node_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '员工用户编号',
  `node_id` bigint NOT NULL COMMENT '节奏节点编号',
  `remind_before` int NOT NULL DEFAULT '3' COMMENT '开始前几天提醒',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_node` (`user_id`,`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='我关注的节奏节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `rhythm_nodes`
--

DROP TABLE IF EXISTS `rhythm_nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rhythm_nodes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(120) NOT NULL COMMENT '节点名称',
  `kind` varchar(20) NOT NULL COMMENT 'sale / new / event',
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `dept_actions` text COMMENT '各部门作战安排，JSON',
  `note` varchar(500) DEFAULT NULL COMMENT '作战说明',
  `linked_refs` varchar(500) DEFAULT NULL COMMENT '关联任务 / 文档，逗号分隔',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_range` (`start_date`,`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='大促·新品节奏节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL COMMENT '企微部门 ID，直接沿用不再生成',
  `parent_id` bigint NOT NULL DEFAULT '0',
  `name` varchar(64) NOT NULL,
  `path` varchar(255) NOT NULL DEFAULT '' COMMENT '企微网关解析好的全路径',
  `sort_order` bigint NOT NULL DEFAULT '0',
  `leader_userid` varchar(128) NOT NULL DEFAULT '' COMMENT '部门负责人企微 userid；网关暂未下发，可手工维护',
  `leader_userids` json DEFAULT NULL COMMENT '全部负责人企微 userid 数组；leader_userid 存首个',
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '通讯录里已消失的部门置 0，不物理删除以保留历史归属',
  `synced_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sys_dept_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门：来自企微通讯录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(64) NOT NULL,
  `label` varchar(128) NOT NULL,
  `value` varchar(128) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_data` (`dict_type`,`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(64) NOT NULL COMMENT '字典类型，移植时直接沿用芋道的 hrm_* / oa_* 命名',
  `name` varchar(64) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(255) NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_directory_sync_log`
--

DROP TABLE IF EXISTS `sys_directory_sync_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_directory_sync_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trigger_type` varchar(16) NOT NULL COMMENT 'manual / schedule',
  `actor` varchar(64) NOT NULL DEFAULT '' COMMENT '手动触发时的人，定时为 system',
  `ok` tinyint(1) DEFAULT NULL COMMENT 'NULL=杩愯?涓?紝1=鎴愬姛锛?=澶辫触',
  `user_created` int NOT NULL DEFAULT '0',
  `user_updated` int NOT NULL DEFAULT '0',
  `departments` int NOT NULL DEFAULT '0',
  `conflicts` int NOT NULL DEFAULT '0',
  `missing` int NOT NULL DEFAULT '0',
  `message` varchar(255) NOT NULL DEFAULT '',
  `duration_ms` bigint DEFAULT NULL COMMENT '杩愯?涓?繕娌℃湁鑰楁椂锛屼笉鑳芥嬁 0 鍐掑厖',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sys_directory_sync_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通讯录同步运行记录：运维按时间倒序看每次结果';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_employee_sync_job`
--

DROP TABLE IF EXISTS `sys_employee_sync_job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_employee_sync_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_user_id` bigint DEFAULT NULL COMMENT '发起人，对应 sys_user.id（原表存的是 username）',
  `status` varchar(16) NOT NULL DEFAULT 'queued' COMMENT 'queued/running/ready/applying/applied/failed/stale',
  `stage` varchar(64) NOT NULL DEFAULT '' COMMENT '当前阶段，用于前端进度条',
  `total` int NOT NULL DEFAULT '0',
  `processed` int NOT NULL DEFAULT '0',
  `worker_token` varchar(64) DEFAULT NULL COMMENT '租约持有者令牌，防两个 worker 抢同一任务',
  `lease_expires_at` datetime DEFAULT NULL COMMENT '租约到期时间，过期可被接管',
  `attempt_count` int NOT NULL DEFAULT '0',
  `scan_summary` json DEFAULT NULL COMMENT '扫描结果汇总',
  `preview` json DEFAULT NULL COMMENT '待确认差异预览',
  `unmatched_candidates` json DEFAULT NULL COMMENT '企微里找不到对应员工的差异',
  `apply_summary` json DEFAULT NULL COMMENT '落库结果汇总',
  `error_code` varchar(64) NOT NULL DEFAULT '',
  `error_message` text,
  `heartbeat_at` datetime DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL COMMENT '任务本身的过期时间（与租约不同）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sync_job_status` (`status`,`lease_expires_at`),
  KEY `idx_sync_job_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='企微员工资料扫描任务：扫描只出差异，确认后才写档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT '0',
  `name` varchar(64) NOT NULL,
  `type` varchar(16) NOT NULL DEFAULT 'button' COMMENT 'dir / menu / button',
  `path` varchar(191) NOT NULL DEFAULT '' COMMENT '前端路由，button 类型为空',
  `permission` varchar(128) NOT NULL DEFAULT '' COMMENT '权限串，沿用芋道 oa:leave:create 风格，便于移植代码直接对接',
  `sort_order` int NOT NULL DEFAULT '0',
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_permission` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单与按钮权限';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_operate_log`
--

DROP TABLE IF EXISTS `sys_operate_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_operate_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_id` varchar(64) NOT NULL DEFAULT '' COMMENT '链路追踪编号',
  `user_id` bigint DEFAULT NULL COMMENT '操作人，对应 sys_user.id',
  `user_type` int NOT NULL DEFAULT '0' COMMENT '用户类型，与 Sa-Token 体系对齐',
  `type` varchar(64) NOT NULL DEFAULT '' COMMENT '操作模块类型，如 公司动态/HR 文档',
  `sub_type` varchar(64) NOT NULL DEFAULT '' COMMENT '操作名，如 发布/归档',
  `biz_id` bigint DEFAULT NULL COMMENT '被操作对象的业务编号',
  `action` varchar(512) NOT NULL DEFAULT '' COMMENT '操作内容摘要',
  `extra` text COMMENT '拓展字段',
  `target_table` varchar(64) NOT NULL DEFAULT '' COMMENT 'Portal 口径：被改动的表名',
  `before_value` text COMMENT 'Portal 口径：改动前快照',
  `after_value` text COMMENT 'Portal 口径：改动后快照',
  `comment` varchar(512) NOT NULL DEFAULT '' COMMENT '操作说明',
  `request_method` varchar(16) NOT NULL DEFAULT '',
  `request_url` varchar(255) NOT NULL DEFAULT '',
  `user_ip` varchar(64) NOT NULL DEFAULT '',
  `user_agent` varchar(255) NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_operate_log_user` (`user_id`,`created_at`),
  KEY `idx_operate_log_biz` (`target_table`,`biz_id`),
  KEY `idx_operate_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志：谁在什么时候把哪条记录改成了什么样';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT '角色标识，如 admin / hr / ops / manager / employee',
  `name` varchar(64) NOT NULL,
  `built_in` tinyint(1) NOT NULL DEFAULT '0' COMMENT '内置角色不允许删除',
  `remark` varchar(255) NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色：权限在本地维护，成员来源仍是企微';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wecom_userid` varchar(128) DEFAULT NULL COMMENT '企微成员 userid；纯本地账号为 NULL',
  `username` varchar(64) NOT NULL COMMENT '登录名，与企微 userid 保持一致',
  `name` varchar(64) NOT NULL COMMENT '姓名，以企微通讯录为准',
  `position` varchar(64) NOT NULL DEFAULT '' COMMENT '岗位，来自企微',
  `main_dept_id` bigint DEFAULT NULL COMMENT '主部门，对应 sys_dept.id',
  `mobile` varchar(32) NOT NULL DEFAULT '',
  `email` varchar(128) NOT NULL DEFAULT '',
  `avatar` varchar(512) NOT NULL DEFAULT '',
  `wecom_status` int DEFAULT NULL COMMENT '企微原始状态：1 已激活 2 已禁用 4 未激活 5 已离职',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许登录；企微非激活成员同步后置 0',
  `password_hash` varchar(256) DEFAULT NULL COMMENT '仅本地口令登录使用，线上走企微不依赖该字段',
  `last_login_at` datetime DEFAULT NULL,
  `directory_synced_at` datetime DEFAULT NULL COMMENT '最近一次由通讯录同步覆盖的时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_wecom` (`wecom_userid`),
  KEY `idx_sys_user_main_dept` (`main_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录账号：由企微通讯录同步生成';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_dept`
--

DROP TABLE IF EXISTS `sys_user_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_dept` (`user_id`,`dept_id`),
  KEY `idx_sys_user_dept_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成员与部门的多对多（企微允许一人多部门）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`,`role_id`),
  KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voices`
--

DROP TABLE IF EXISTS `voices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT '为空表示匿名',
  `channel` varchar(30) NOT NULL COMMENT 'suggestion/ceo/anonymous/survey',
  `anonymous` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1 对读者隐去作者姓名',
  `content` varchar(2000) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'open',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`,`created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='我要发声';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-09 18:53:19
