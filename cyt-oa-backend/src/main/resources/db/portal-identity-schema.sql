-- =====================================================================
-- B1 身份与权限地基：把 Portal 唯一真实的人事主数据搬进 OA 之前要补的列与表。
-- 口径出处：docs/Portal管理面迁移契约.md §一「身份与权限」、§二「类型转换口径」。
-- 与其他 db/*.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
-- MySQL 8 的 ALTER 没有 IF NOT EXISTS，重复执行前先查 information_schema。
-- =====================================================================

-- 1. 部门负责人：Portal 的 portal_departments.leader_userids 是复数（企微支持多负责人），
--    OA 的 leader_userid 是单值，且 AdminUserApiImpl.getUserListBySubordinate 靠它反查下属。
--    保留单值列继续做「主负责人」，新增 JSON 列存全部负责人，迁移时首个写进单值列。
ALTER TABLE `sys_dept`
  ADD COLUMN `leader_userids` JSON NULL COMMENT '全部负责人企微 userid 数组；leader_userid 存首个' AFTER `leader_userid`;

-- 2. 员工本人不参与庆祝：Portal 由 portal_users.celebration_opt_out 承载，
--    搬到档案域后与 birthday / entry_time 同表，避免庆祝模块反向依赖登录账号表。
ALTER TABLE `hrm_employee`
  ADD COLUMN `celebration_opt_out` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本人不参与生日/周年庆祝' AFTER `candidate_id`;

-- 3. 操作日志：Portal 的 portal_admin_audit（364 行，含变更前后快照）迁到这里，
--    顺带补上 OA 自己的缺口——OperateLogApiImpl 目前只 log.debug 落空，注释写着「操作日志表尚未建立」。
--    列名对齐 OperateLogCreateReqDTO，移植业务无需改调用方。
CREATE TABLE IF NOT EXISTS sys_operate_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    trace_id       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '链路追踪编号',
    user_id        BIGINT       NULL     COMMENT '操作人，对应 sys_user.id',
    user_type      INT          NOT NULL DEFAULT 0 COMMENT '用户类型，与 Sa-Token 体系对齐',
    type           VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作模块类型，如 公司动态/HR 文档',
    sub_type       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '操作名，如 发布/归档',
    biz_id         BIGINT       NULL     COMMENT '被操作对象的业务编号',
    action         VARCHAR(512) NOT NULL DEFAULT '' COMMENT '操作内容摘要',
    extra          TEXT         NULL     COMMENT '拓展字段',
    target_table   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT 'Portal 口径：被改动的表名',
    before_value   TEXT         NULL     COMMENT 'Portal 口径：改动前快照',
    after_value    TEXT         NULL     COMMENT 'Portal 口径：改动后快照',
    comment        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '操作说明',
    request_method VARCHAR(16)  NOT NULL DEFAULT '',
    request_url    VARCHAR(255) NOT NULL DEFAULT '',
    user_ip        VARCHAR(64)  NOT NULL DEFAULT '',
    user_agent     VARCHAR(255) NOT NULL DEFAULT '',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operate_log_user (user_id, created_at),
    KEY idx_operate_log_biz (target_table, biz_id),
    KEY idx_operate_log_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '操作日志：谁在什么时候把哪条记录改成了什么样';

-- 4. 员工资料扫描任务：Portal 的 portal_employee_sync_jobs 是带租约（worker_token/lease_expires_at）
--    和进度百分比的长任务状态机，与 sys_directory_sync_log（一次性同步审计）生命周期不同，单独建表。
--    原表主键是 PGUUID，按契约统一改 BIGINT。
CREATE TABLE IF NOT EXISTS sys_employee_sync_job (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id     BIGINT       NULL     COMMENT '发起人，对应 sys_user.id（原表存的是 username）',
    status            VARCHAR(16)  NOT NULL DEFAULT 'queued' COMMENT 'queued/running/ready/applying/applied/failed/stale',
    stage             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '当前阶段，用于前端进度条',
    total             INT          NOT NULL DEFAULT 0,
    processed         INT          NOT NULL DEFAULT 0,
    worker_token      VARCHAR(64)  NULL     COMMENT '租约持有者令牌，防两个 worker 抢同一任务',
    lease_expires_at  DATETIME     NULL     COMMENT '租约到期时间，过期可被接管',
    attempt_count     INT          NOT NULL DEFAULT 0,
    scan_summary      JSON         NULL     COMMENT '扫描结果汇总',
    preview           JSON         NULL     COMMENT '待确认差异预览',
    unmatched_candidates JSON      NULL     COMMENT '企微里找不到对应员工的差异',
    apply_summary     JSON         NULL     COMMENT '落库结果汇总',
    error_code        VARCHAR(64)  NOT NULL DEFAULT '',
    error_message     TEXT         NULL,
    heartbeat_at      DATETIME     NULL,
    expires_at        DATETIME     NULL     COMMENT '任务本身的过期时间（与租约不同）',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sync_job_status (status, lease_expires_at),
    KEY idx_sync_job_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '企微员工资料扫描任务：扫描只出差异，确认后才写档案';
