-- 迁自 CYT-Portal models.py；保留旧列和主键，历史关系不重新编号。

CREATE TABLE IF NOT EXISTS `portal_material_site_owners` (
  `site` VARCHAR(8) NOT NULL,
  `kind` VARCHAR(16) NOT NULL,
  `owner_username` VARCHAR(64) NOT NULL,
  `owner_name` VARCHAR(64) NOT NULL,
  `updated_by` VARCHAR(64) NULL,
  `updated_by_name` VARCHAR(64) NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`site`,`kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_products` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kind` VARCHAR(16) NOT NULL,
  `category_id` BIGINT NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `brand` VARCHAR(50) NULL,
  `description` LONGTEXT NULL,
  `image_urls` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `variant_dims` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `archived_at` DATETIME NULL,
  `archived_by` VARCHAR(64) NULL,
  `created_by` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_materials` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `kind` VARCHAR(16) NOT NULL DEFAULT 'sample',
  `product_id` BIGINT NULL,
  `variant_values` JSON NOT NULL DEFAULT (JSON_OBJECT()),
  `site` VARCHAR(8) NOT NULL DEFAULT 'SZ',
  `category` VARCHAR(32) NOT NULL,
  `sku` VARCHAR(64) NULL,
  `name` VARCHAR(200) NOT NULL,
  `brand` VARCHAR(50) NULL,
  `spec` VARCHAR(64) NULL,
  `unit_price` DECIMAL(10,2) NULL,
  `image_urls` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `stock_qty` BIGINT NOT NULL DEFAULT '1',
  `location` VARCHAR(64) NOT NULL DEFAULT '陈列室',
  `current_borrowers` JSON NOT NULL DEFAULT (JSON_ARRAY()),
  `owner_username` VARCHAR(64) NOT NULL,
  `owner_name` VARCHAR(64) NOT NULL,
  `note` LONGTEXT NULL,
  `archived_at` DATETIME NULL,
  `archived_by` VARCHAR(64) NULL,
  `created_by` VARCHAR(64) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY idx_material_product(product_id),
  KEY idx_material_kind_site(kind,site),
  CHECK(stock_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_applies` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_no` VARCHAR(32) NOT NULL,
  `apply_type` VARCHAR(16) NOT NULL DEFAULT 'sample_borrow',
  `applicant_username` VARCHAR(64) NOT NULL,
  `applicant_name` VARCHAR(64) NOT NULL,
  `department` VARCHAR(64) NULL,
  `purpose` VARCHAR(200) NOT NULL DEFAULT '',
  `expect_return_date` DATE NULL,
  `actual_return_date` DATE NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending',
  `remark` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_material_apply_no(apply_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_apply_items` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_id` BIGINT NOT NULL,
  `material_id` BIGINT NOT NULL,
  `material_name` VARCHAR(200) NOT NULL,
  `material_sku` VARCHAR(64) NOT NULL DEFAULT '',
  `spec` VARCHAR(64) NULL,
  `quantity` BIGINT NOT NULL,
  `approved_qty` BIGINT NULL,
  `returned_quantity` BIGINT NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY idx_material_apply(apply_id),
  KEY idx_material_reference(material_id),
  CHECK(quantity > 0),
  CHECK(approved_qty IS NULL OR (approved_qty>=0 AND approved_qty<=quantity)),
  CHECK(returned_quantity>=0 AND returned_quantity<=COALESCE(approved_qty,quantity))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `portal_material_apply_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `apply_id` BIGINT NOT NULL,
  `operation` VARCHAR(32) NOT NULL,
  `operator_username` VARCHAR(64) NOT NULL,
  `operator_name` VARCHAR(64) NOT NULL,
  `before_value` LONGTEXT NULL,
  `after_value` LONGTEXT NULL,
  `remark` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
