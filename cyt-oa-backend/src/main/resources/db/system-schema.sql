-- 系统域建表脚本：账号、组织、角色、菜单权限、字典、通讯录同步记录。
-- 与 db/schema.sql 一样，启动时不会自动执行，需手工在 cyt_oa_center 库执行一次。
-- 账号与部门一律由企微通讯录同步写入（见 EnterpriseDirectorySyncService），本表组只额外维护角色绑定与本地口令。

CREATE TABLE IF NOT EXISTS sys_user (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    wecom_userid        VARCHAR(128) NULL     COMMENT '企微成员 userid；纯本地账号为 NULL',
    username            VARCHAR(64)  NOT NULL COMMENT '登录名，与企微 userid 保持一致',
    name                VARCHAR(64)  NOT NULL COMMENT '姓名，以企微通讯录为准',
    position            VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '岗位，来自企微',
    main_dept_id        BIGINT       NULL     COMMENT '主部门，对应 sys_dept.id',
    mobile              VARCHAR(32)  NOT NULL DEFAULT '',
    email               VARCHAR(128) NOT NULL DEFAULT '',
    avatar              VARCHAR(512) NOT NULL DEFAULT '',
    wecom_status        INT          NULL     COMMENT '企微原始状态：1 已激活 2 已禁用 4 未激活 5 已离职',
    enabled             TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否允许登录；企微非激活成员同步后置 0',
    password_hash       VARCHAR(256) NULL     COMMENT '仅本地口令登录使用，线上走企微不依赖该字段',
    last_login_at       DATETIME     NULL,
    directory_synced_at DATETIME     NULL     COMMENT '最近一次由通讯录同步覆盖的时间',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_wecom (wecom_userid),
    KEY idx_sys_user_main_dept (main_dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '登录账号：由企微通讯录同步生成';

CREATE TABLE IF NOT EXISTS sys_dept (
    id           BIGINT      NOT NULL COMMENT '企微部门 ID，直接沿用不再生成',
    parent_id    BIGINT      NOT NULL DEFAULT 0,
    name         VARCHAR(64) NOT NULL,
    path         VARCHAR(255) NOT NULL DEFAULT '' COMMENT '企微网关解析好的全路径',
    sort_order   BIGINT      NOT NULL DEFAULT 0 COMMENT '企微 uint32 排序号，不能用有符号 INT',
    leader_userid VARCHAR(128) NOT NULL DEFAULT '' COMMENT '部门负责人企微 userid；网关暂未下发，可手工维护',
    active       TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '通讯录里已消失的部门置 0，不物理删除以保留历史归属',
    synced_at    DATETIME    NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_dept_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '部门：来自企微通讯录';

CREATE TABLE IF NOT EXISTS sys_user_dept (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_dept (user_id, dept_id),
    KEY idx_sys_user_dept_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '成员与部门的多对多（企微允许一人多部门）';

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64) NOT NULL COMMENT '角色标识，如 admin / hr / ops / manager / employee',
    name        VARCHAR(64) NOT NULL,
    built_in    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '内置角色不允许删除',
    remark      VARCHAR(255) NOT NULL DEFAULT '',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '角色：权限在本地维护，成员来源仍是企微';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_menu (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    parent_id  BIGINT      NOT NULL DEFAULT 0,
    name       VARCHAR(64) NOT NULL,
    type       VARCHAR(16) NOT NULL DEFAULT 'button' COMMENT 'dir / menu / button',
    path       VARCHAR(191) NOT NULL DEFAULT '' COMMENT '前端路由，button 类型为空',
    permission VARCHAR(128) NOT NULL DEFAULT '' COMMENT '权限串，形如 oa:leave:create，与代码里的注解保持一致',
    sort_order INT         NOT NULL DEFAULT 0,
    visible    TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_sys_menu_permission (permission)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '菜单与按钮权限';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_menu (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    type       VARCHAR(64) NOT NULL COMMENT '字典类型，与代码里的 DictTypeConstants 常量一一对应',
    name       VARCHAR(64) NOT NULL,
    status     TINYINT(1)  NOT NULL DEFAULT 1,
    remark     VARCHAR(255) NOT NULL DEFAULT '',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    dict_type  VARCHAR(64) NOT NULL,
    label      VARCHAR(128) NOT NULL,
    value      VARCHAR(128) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    status     TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_data (dict_type, value)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_directory_sync_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    trigger_type VARCHAR(16) NOT NULL COMMENT 'manual / schedule',
    actor        VARCHAR(64) NOT NULL DEFAULT '' COMMENT '手动触发时的人，定时为 system',
    ok           TINYINT(1)  NULL DEFAULT NULL COMMENT 'NULL=运行中，1=成功，0=失败',
    user_created INT         NOT NULL DEFAULT 0,
    user_updated INT         NOT NULL DEFAULT 0,
    departments  INT         NOT NULL DEFAULT 0,
    conflicts    INT         NOT NULL DEFAULT 0,
    missing      INT         NOT NULL DEFAULT 0,
    message      VARCHAR(255) NOT NULL DEFAULT '',
    duration_ms  BIGINT      NULL COMMENT '运行中还没有耗时，不能拿 0 冒充',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at  DATETIME    NULL,
    PRIMARY KEY (id),
    KEY idx_sys_directory_sync_log_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '通讯录同步运行记录：运维按时间倒序看每次结果';
