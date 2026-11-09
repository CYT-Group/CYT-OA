-- Warm-Flow 1.8.9 引擎表。
-- 来源：https://github.com/dromara/warm-flow/blob/v1.8.9/sql/mysql/warm-flow-all.sql（官方脚本，按 tag 取，未改字段），
-- 另外补 sql/mysql/v1-upgrade/warm-flow_form.sql 的 flow_form（官方标注「form 开发中」）。
-- 与本地 entity 的逐列核对（javap 读 warm-flow-mybatis-plus-core-1.8.9.jar 的 org/dromara/warm/flow/orm/entity）：
--   flow_definition 的 node_list/user_list、flow_task 的 flow_name/business_id/permission_list/user_list、
--   flow_his_task 的 flow_name/business_id/permission_list、flow_instance 的 flow_name/form_custom/form_path、
--   flow_skip 的 node_id 都带 @TableField(exist = false)，是 join 或内存字段，不建列。
--   除以上字段外，entity 需要的列官方脚本全覆盖，反之亦然。
-- 改动只有三处：
--   1) 加 IF NOT EXISTS，方便重复执行；
--   2) flow_form 补 create_by/update_by——FlowForm 实体有这两个字段，官方 form 脚本漏了；
--   3) 统一 utf8mb4 + utf8mb4_unicode_ci，与本项目其余表一致。
-- 主键不自增：Warm-Flow 用 starter 注册的 MybatisPlusIdGen 自己发号，所以 id 是 NOT NULL 而没有 AUTO_INCREMENT。
-- del_flag 由实体上的 @TableLogic(value = "0", delval = "1") 驱动，本项目没配全局逻辑删除字段，不会互相干扰。
-- tenant_id 保留但恒为 NULL：本系统不做多租户。

CREATE TABLE IF NOT EXISTS `flow_definition`
(
    `id`              bigint          NOT NULL COMMENT '主键id',
    `flow_code`       varchar(40)     NOT NULL COMMENT '流程编码',
    `flow_name`       varchar(100)    NOT NULL COMMENT '流程名称',
    `model_value`     varchar(40)     NOT NULL DEFAULT 'CLASSICS' COMMENT '设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）',
    `category`        varchar(100)             DEFAULT NULL COMMENT '流程类别',
    `version`         varchar(20)     NOT NULL COMMENT '流程版本',
    `is_publish`      tinyint(1)      NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_custom`     char(1)                  DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)             DEFAULT NULL COMMENT '审批表单路径',
    `activity_status` tinyint(1)      NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `listener_type`   varchar(100)             DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)             DEFAULT NULL COMMENT '监听器路径',
    `ext`             varchar(500)             DEFAULT NULL COMMENT '业务详情 存业务表对象json字符串',
    `create_time`     datetime                 DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)              DEFAULT '' COMMENT '创建人',
    `update_time`     datetime                 DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)              DEFAULT '' COMMENT '更新人',
    `del_flag`        char(1)                  DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)              DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_flow_code` (`flow_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='流程定义表';

CREATE TABLE IF NOT EXISTS `flow_node`
(
    `id`              bigint        NOT NULL COMMENT '主键id',
    `node_type`       tinyint(1)      NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `definition_id`   bigint          NOT NULL COMMENT '流程定义id',
    `node_code`       varchar(100)    NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)  DEFAULT NULL COMMENT '流程节点名称',
    `permission_flag` varchar(200)  DEFAULT NULL COMMENT '权限标识（权限类型:权限标识，可以多个，用@@隔开)',
    `node_ratio`      varchar(200)  DEFAULT NULL COMMENT '流程签署比例值',
    `coordinate`      varchar(100)  DEFAULT NULL COMMENT '坐标',
    `any_node_skip`   varchar(100)  DEFAULT NULL COMMENT '任意结点跳转',
    `listener_type`   varchar(100)  DEFAULT NULL COMMENT '监听器类型',
    `listener_path`   varchar(400)  DEFAULT NULL COMMENT '监听器路径',
    `form_custom`     char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`       varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `version`         varchar(20)     NOT NULL COMMENT '版本',
    `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)   DEFAULT '' COMMENT '创建人',
    `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)   DEFAULT '' COMMENT '更新人',
    `ext`             text COMMENT '节点扩展属性',
    `del_flag`        char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_definition_id` (`definition_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='流程节点表';

CREATE TABLE IF NOT EXISTS `flow_skip`
(
    `id`             bigint       NOT NULL COMMENT '主键id',
    `definition_id`  bigint          NOT NULL COMMENT '流程定义id',
    `now_node_code`  varchar(100)    NOT NULL COMMENT '当前流程节点的编码',
    `now_node_type`  tinyint(1)   DEFAULT NULL COMMENT '当前节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `next_node_code` varchar(100)    NOT NULL COMMENT '下一个流程节点的编码',
    `next_node_type` tinyint(1)   DEFAULT NULL COMMENT '下一个节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `skip_name`      varchar(100) DEFAULT NULL COMMENT '跳转名称',
    `skip_type`      varchar(40)  DEFAULT NULL COMMENT '跳转类型（PASS审批通过 REJECT退回）',
    `skip_condition` varchar(200) DEFAULT NULL COMMENT '跳转条件',
    `coordinate`     varchar(100) DEFAULT NULL COMMENT '坐标',
    `create_time`    datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`      varchar(64)  DEFAULT '' COMMENT '创建人',
    `update_time`    datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`      varchar(64)  DEFAULT '' COMMENT '更新人',
    `del_flag`       char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`      varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_definition_id` (`definition_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='节点跳转关联表';

CREATE TABLE IF NOT EXISTS `flow_instance`
(
    `id`              bigint      NOT NULL COMMENT '主键id',
    `definition_id`   bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `business_id`     varchar(40) NOT NULL COMMENT '业务id',
    `node_type`       tinyint(1)  NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `node_code`       varchar(40) NOT NULL COMMENT '流程节点编码',
    `node_name`       varchar(100)         DEFAULT NULL COMMENT '流程节点名称',
    `variable`        text COMMENT '任务变量',
    `flow_status`     varchar(20) NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `activity_status` tinyint(1)  NOT NULL DEFAULT '1' COMMENT '流程激活状态（0挂起 1激活）',
    `def_json`        text COMMENT '流程定义json',
    `create_time`     datetime             DEFAULT NULL COMMENT '创建时间',
    `create_by`       varchar(64)          DEFAULT '' COMMENT '创建人',
    `update_time`     datetime             DEFAULT NULL COMMENT '更新时间',
    `update_by`       varchar(64)          DEFAULT '' COMMENT '更新人',
    `ext`             varchar(500)         DEFAULT NULL COMMENT '扩展字段，预留给业务系统使用',
    `del_flag`        char(1)              DEFAULT '0' COMMENT '删除标志',
    `tenant_id`       varchar(40)          DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_business_id` (`business_id`),
    KEY `idx_definition_id` (`definition_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='流程实例表';

CREATE TABLE IF NOT EXISTS `flow_task`
(
    `id`            bigint       NOT NULL COMMENT '主键id',
    `definition_id` bigint       NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`   bigint       NOT NULL COMMENT '对应flow_instance表的id',
    `node_code`     varchar(100) NOT NULL COMMENT '节点编码',
    `node_name`     varchar(100) DEFAULT NULL COMMENT '节点名称',
    `node_type`     tinyint(1)   NOT NULL COMMENT '节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `flow_status`   varchar(20)  NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`   char(1)      DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`     varchar(100) DEFAULT NULL COMMENT '审批表单路径',
    `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
    `create_by`     varchar(64)  DEFAULT '' COMMENT '创建人',
    `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
    `update_by`     varchar(64)  DEFAULT '' COMMENT '更新人',
    `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志',
    `tenant_id`     varchar(40)  DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_definition_id` (`definition_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='待办任务表';

CREATE TABLE IF NOT EXISTS `flow_his_task`
(
    `id`               bigint      NOT NULL COMMENT '主键id',
    `definition_id`    bigint      NOT NULL COMMENT '对应flow_definition表的id',
    `instance_id`      bigint      NOT NULL COMMENT '对应flow_instance表的id',
    `task_id`          bigint      NOT NULL COMMENT '对应flow_task表的id',
    `node_code`        varchar(100)  DEFAULT NULL COMMENT '开始节点编码',
    `node_name`        varchar(100)  DEFAULT NULL COMMENT '开始节点名称',
    `node_type`        tinyint(1)    DEFAULT NULL COMMENT '开始节点类型（0开始节点 1中间节点 2结束节点 3互斥网关 4并行网关）',
    `target_node_code` varchar(200)  DEFAULT NULL COMMENT '目标节点编码',
    `target_node_name` varchar(200)  DEFAULT NULL COMMENT '结束节点名称',
    `approver`         varchar(40)   DEFAULT NULL COMMENT '审批人',
    `cooperate_type`   tinyint(1)    NOT NULL DEFAULT '0' COMMENT '协作方式(1审批 2转办 3委派 4会签 5票签 6加签 7减签)',
    `collaborator`     varchar(500)  DEFAULT NULL COMMENT '协作人',
    `skip_type`        varchar(10)   NOT NULL COMMENT '流转类型（PASS通过 REJECT退回 NONE无动作）',
    `flow_status`      varchar(20)   NOT NULL COMMENT '流程状态（0待提交 1审批中 2审批通过 4终止 5作废 6撤销 8已完成 9已退回 10失效 11拿回）',
    `form_custom`      char(1)       DEFAULT 'N' COMMENT '审批表单是否自定义（Y是 N否）',
    `form_path`        varchar(100)  DEFAULT NULL COMMENT '审批表单路径',
    `message`          varchar(500)  DEFAULT NULL COMMENT '审批意见',
    `variable`         text COMMENT '任务变量',
    `ext`              text COMMENT '业务详情 存业务表对象json字符串',
    `create_time`      datetime      DEFAULT NULL COMMENT '任务开始时间',
    `update_time`      datetime      DEFAULT NULL COMMENT '审批完成时间',
    `del_flag`         char(1)       DEFAULT '0' COMMENT '删除标志',
    `tenant_id`        varchar(40)   DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_task_id` (`task_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='历史任务记录表';

CREATE TABLE IF NOT EXISTS `flow_user`
(
    `id`           bigint      NOT NULL COMMENT '主键id',
    `type`         char(1)         NOT NULL COMMENT '人员类型（1待办任务的审批人权限 2待办任务的转办人权限 3待办任务的委托人权限）',
    `processed_by` varchar(80) DEFAULT NULL COMMENT '权限人',
    `associated`   bigint          NOT NULL COMMENT '任务表id',
    `create_time`  datetime    DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(80) DEFAULT NULL COMMENT '创建人',
    `update_time`  datetime    DEFAULT NULL COMMENT '更新时间',
    `update_by`    varchar(64) DEFAULT '' COMMENT '更新人',
    `del_flag`     char(1)     DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40) DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `user_processed_type` (`processed_by`, `type`),
    KEY `user_associated` (`associated`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='流程用户表';

CREATE TABLE IF NOT EXISTS `flow_form`
(
    `id`           bigint        NOT NULL COMMENT '主键id',
    `form_code`    varchar(40)   NOT NULL COMMENT '表单编码',
    `form_name`    varchar(100)  NOT NULL COMMENT '表单名称',
    `version`      varchar(20)   NOT NULL COMMENT '表单版本',
    `is_publish`   tinyint(1)    NOT NULL DEFAULT '0' COMMENT '是否发布（0未发布 1已发布 9失效）',
    `form_type`    tinyint(1)    NULL DEFAULT '0' COMMENT '表单类型（0内置表单 存 form_content 1外挂表单 存form_path）',
    `form_path`    varchar(100)  NULL DEFAULT NULL COMMENT '表单路径',
    `form_content` longtext NULL COMMENT '表单内容',
    `ext`          varchar(400)  NULL DEFAULT NULL COMMENT '表单扩展，用户自行使用',
    `create_time`  datetime      NULL DEFAULT NULL COMMENT '创建时间',
    `create_by`    varchar(64)   NULL DEFAULT '' COMMENT '创建人',
    `update_time`  datetime      NULL DEFAULT NULL COMMENT '更新时间',
    `update_by`    varchar(64)   NULL DEFAULT '' COMMENT '更新人',
    `del_flag`     char(1)       NULL DEFAULT '0' COMMENT '删除标志',
    `tenant_id`    varchar(40)   NULL DEFAULT NULL COMMENT '租户id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_form_code` (`form_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='流程表单表';
