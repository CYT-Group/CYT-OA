-- 在 hrm-schema.sql、system-schema.sql 之后执行；可重复执行，不覆盖已有模板或授权。
CREATE TABLE IF NOT EXISTS hrm_contract_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  version VARCHAR(40) NOT NULL,
  kind VARCHAR(20) NOT NULL COMMENT 'onboard / offboard',
  contract_type INT NOT NULL DEFAULT 1,
  scope VARCHAR(255) NOT NULL DEFAULT '',
  content MEDIUMTEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  reviewer BIGINT NULL,
  review_note VARCHAR(500) NULL,
  creator BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  UNIQUE KEY uk_name_version (name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hrm_employment_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_key VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  employee_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  kind VARCHAR(20) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  effective_date DATE NOT NULL,
  account_disabled TINYINT NOT NULL DEFAULT 0,
  creator BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_request (request_key),
  KEY idx_employee (employee_id),
  KEY idx_disable (kind, account_disabled, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hrm_candidate_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  media_type VARCHAR(100) NOT NULL,
  content MEDIUMBLOB NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_candidate (candidate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 只注册本次人事工作区所用权限。HR 不获得角色配置、通讯录同步或全员导出权。
INSERT INTO sys_menu(name,type,permission)
SELECT p.name,'button',p.permission FROM (
 SELECT '人事工作台' name,'hrm:home:hr-query' permission UNION ALL
 SELECT '维护员工','hrm:employee:update' UNION ALL
 SELECT '新增员工','hrm:employee:create' UNION ALL
 SELECT '查询员工','hrm:employee:query' UNION ALL
 SELECT '招聘查询','hrm:recruit:candidate:query' UNION ALL
 SELECT '新增候选人','hrm:recruit:candidate:create' UNION ALL
 SELECT '候选人流转','hrm:recruit:candidate:update' UNION ALL
 SELECT '招聘职位查询','hrm:recruit:post:query' UNION ALL
 SELECT '招聘职位维护','hrm:recruit:post:create' UNION ALL
 SELECT '招聘渠道查询','hrm:recruit:channel:query' UNION ALL
 SELECT '面试查询','hrm:recruit:interview:query' UNION ALL
 SELECT '安排面试','hrm:recruit:interview:create' UNION ALL
 SELECT '面试反馈','hrm:recruit:interview:update' UNION ALL
 SELECT '合同模板查询','hrm:contract-template:query' UNION ALL
 SELECT '提交合同模板','hrm:contract-template:create' UNION ALL
 SELECT '审核合同模板','hrm:contract-template:review' UNION ALL
 SELECT '人事后台入口','can_enter_admin'
) p WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.permission=p.permission);

INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.code IN ('hr','admin') AND (
 m.permission IN ('hrm:home:hr-query','hrm:employee:query','hrm:employee:create','hrm:employee:update',
 'hrm:recruit:candidate:query','hrm:recruit:candidate:create','hrm:recruit:candidate:update',
 'hrm:recruit:post:query','hrm:recruit:post:create','hrm:recruit:channel:query',
 'hrm:recruit:interview:query','hrm:recruit:interview:create','hrm:recruit:interview:update',
 'hrm:contract-template:query','hrm:contract-template:create','can_enter_admin')
 OR (r.code='admin' AND m.permission='hrm:contract-template:review'));
