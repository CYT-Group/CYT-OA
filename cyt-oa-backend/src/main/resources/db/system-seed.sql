-- 系统域初始数据：内置角色、系统权限、以及一个本地开发账号。
-- 只插入一次；重复执行会因为唯一键冲突报错，不会覆盖运行期数据。

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

-- 本地开发账号：username=admin password=admin123，仅在 cyt.auth.password-login-enabled=true 时可登录。
-- 刻意留空 wecom_userid：这是纯本地身份，企微通讯录同步永远不会改写它，也不会把它认成某个员工。
-- 线上该开关为 false，账号只能由通讯录同步产生；这条记录只作为开发期入口，上生产前应停用。
INSERT INTO sys_user (id, wecom_userid, username, name, position, enabled, password_hash) VALUES
    (1, NULL, 'admin', '张伟', '市场部 / P6', 1,
     'pbkdf2:sha256:210000$m3g503RNYWhLM6ctXjG2dw==$UHKQeq5ibcoaOaW1xVYAJjdhzDh1IXfx9VAeG7+WjHE=');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
