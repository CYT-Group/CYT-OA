-- 迁自 CYT-Portal models.py；保留旧列和主键，历史关系不重新编号。

CREATE TABLE IF NOT EXISTS `portal_ai_models` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider` VARCHAR(16) NOT NULL,
  `model_id` VARCHAR(64) NOT NULL,
  `display_name` VARCHAR(100) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `input_price` DECIMAL(10,4) NULL,
  `output_price` DECIMAL(10,4) NULL,
  `note` VARCHAR(200) NULL,
  `sort_order` BIGINT NOT NULL DEFAULT '0',
  `created_by` VARCHAR(50) NOT NULL,
  `updated_by` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_ai_model(provider,model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_ai_provider_settings` (
  `provider` VARCHAR(16) NOT NULL,
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  `tier_fast` VARCHAR(64) NULL,
  `tier_standard` VARCHAR(64) NULL,
  `tier_quality` VARCHAR(64) NULL,
  `updated_by` VARCHAR(50) NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `api_key_ciphertext` LONGTEXT NULL,
  `api_key_hint` VARCHAR(8) NULL,
  `api_key_updated_by` VARCHAR(50) NULL,
  `api_key_updated_at` DATETIME NULL,
  PRIMARY KEY (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_ai_task_settings` (
  `task_name` VARCHAR(64) NOT NULL,
  `provider` VARCHAR(16) NULL,
  `model` VARCHAR(64) NULL,
  `effort` VARCHAR(8) NULL,
  `system_prompt` LONGTEXT NULL,
  `user_template` LONGTEXT NULL,
  `updated_by` VARCHAR(50) NOT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_ai_task_setting_versions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_name` VARCHAR(64) NOT NULL,
  `version_no` BIGINT NOT NULL,
  `provider` VARCHAR(16) NULL,
  `model` VARCHAR(64) NULL,
  `effort` VARCHAR(8) NULL,
  `system_prompt` LONGTEXT NULL,
  `user_template` LONGTEXT NULL,
  `note` VARCHAR(200) NULL,
  `created_by` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_ai_task_version(task_name,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_api_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `provider` VARCHAR(32) NOT NULL,
  `model` VARCHAR(64) NULL,
  `endpoint` VARCHAR(128) NOT NULL,
  `tokens_in` BIGINT NULL,
  `tokens_out` BIGINT NULL,
  `cost_usd` DECIMAL(10,6) NULL,
  `latency_ms` BIGINT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ok',
  `error_msg` LONGTEXT NULL,
  `called_by` VARCHAR(50) NULL,
  `called_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
