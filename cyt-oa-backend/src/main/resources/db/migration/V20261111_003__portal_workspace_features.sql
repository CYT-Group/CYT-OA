-- 补齐门户功能树：目标、问卷、规则、审批及人事评议。旧表和旧接口继续保留。
CREATE TABLE IF NOT EXISTS portal_workspace_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  kind VARCHAR(32) NOT NULL,
  record_key VARCHAR(100) NULL,
  owner_id BIGINT NOT NULL,
  dept_id BIGINT NULL,
  parent_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'draft',
  payload JSON NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_workspace_record_key (kind,record_key),
  KEY idx_workspace_owner (kind,owner_id,status,id),
  KEY idx_workspace_parent (kind,parent_id),
  KEY idx_workspace_dept (kind,dept_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portal_workspace_answers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  survey_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  answers JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_workspace_survey_answer (survey_id,user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS portal_workspace_approval_steps (
  id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  step_no INT NOT NULL,
  approver_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'waiting',
  comment VARCHAR(1000) NULL,
  acted_at DATETIME NULL,
  PRIMARY KEY(id),
  UNIQUE KEY uk_workspace_request_step (request_id,step_no),
  KEY idx_workspace_approver (approver_id,status,request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 能力位可通过现有角色授权页面分配；不把系统管理员的权限授予普通角色。
INSERT INTO sys_menu (name,type,permission)
SELECT '发声与调研管理','button','can_manage_voice'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission='can_manage_voice');
INSERT INTO sys_menu (name,type,permission)
SELECT '审批流程与制度规则','button','can_manage_workspace_rules'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission='can_manage_workspace_rules');
INSERT IGNORE INTO sys_role_menu (role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission IN ('can_manage_voice','can_manage_workspace_rules')
WHERE r.code IN ('admin','management');
INSERT IGNORE INTO sys_role_menu (role_id,menu_id)
SELECT r.id,m.id FROM sys_role r JOIN sys_menu m ON m.permission='can_manage_voice'
WHERE r.code='ops';
