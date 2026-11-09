-- =====================================================================
-- 个人中心（/api/me/**）所需表。
-- 沿用 P0 建表的口径：本项目自建表用 created_at，不继承 BaseDO；
-- 只有挂在芋道移植表（oa_work_report）下的子表才用审计列。
-- MySQL 8 的 ALTER 没有 IF NOT EXISTS，重复执行前先看 information_schema。
-- =====================================================================

-- 1. 提醒的去重键：同一件事重复同步只更新不新增。
ALTER TABLE `notifications`
  ADD COLUMN `dedup_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    NULL DEFAULT NULL COMMENT '去重键，同一用户同键只保留一条' AFTER `channel`;
ALTER TABLE `notifications`
  ADD UNIQUE INDEX `uk_user_dedup`(`user_id` ASC, `dedup_key` ASC);

-- 2. 员工关注的大促/新品节点，到点前生成提醒。
CREATE TABLE IF NOT EXISTS `rhythm_node_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id` bigint NOT NULL COMMENT '员工用户编号',
  `node_id` bigint NOT NULL COMMENT '节奏节点编号',
  `remind_before` int NOT NULL DEFAULT 3 COMMENT '开始前几天提醒',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_node`(`user_id` ASC, `node_id` ASC)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '我关注的节奏节点';

-- 3. 工作汇报点评与回复（挂在 oa_work_report 下，跟随父表审计列口径）
CREATE TABLE IF NOT EXISTS `oa_work_report_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点评编号',
  `report_id` bigint NOT NULL COMMENT '工作汇报编号',
  `reviewer_id` bigint NOT NULL COMMENT '点评人用户编号',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '评级：A/B/C',
  `review_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '点评正文',
  `reply` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '员工回复',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report`(`report_id` ASC) USING BTREE,
  INDEX `idx_reviewer`(`reviewer_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工作汇报点评与回复';

-- 4. 发声列表按人过滤并按时间倒序。
ALTER TABLE `voices` ADD INDEX `idx_user_created`(`user_id` ASC, `created_at` DESC);

-- 5. 匿名发声保留作者：author 自己仍能看到并提交记录，只对读者隐去姓名。
ALTER TABLE `voices`
  ADD COLUMN `anonymous` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 对读者隐去作者姓名' AFTER `channel`;

-- 6. 节奏节点表：随个人中心一起接管，dept_actions 的 JSON 结构就是 RhythmNodeView.DeptAction
--    的形状（数组 + items 列表），旧样例里的「部门: 单条安排」扁平对象解析不出来。
CREATE TABLE IF NOT EXISTS `rhythm_nodes` (
  `id`           bigint NOT NULL AUTO_INCREMENT,
  `name`         varchar(120) NOT NULL COMMENT '节点名称',
  `kind`         varchar(20) NOT NULL COMMENT 'sale / new / event',
  `start_date`   date NOT NULL,
  `end_date`     date NOT NULL,
  `dept_actions` text COMMENT '各部门作战安排，JSON',
  `note`         varchar(500) DEFAULT NULL COMMENT '作战说明',
  `linked_refs`  varchar(500) DEFAULT NULL COMMENT '关联任务 / 文档，逗号分隔',
  `created_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_range` (`start_date`, `end_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大促·新品节奏节点';

INSERT INTO `rhythm_nodes`
  (`id`, `name`, `kind`, `start_date`, `end_date`, `dept_actions`, `note`) VALUES
  (1, '双十一大促', 'sale', '2026-11-01', '2026-11-11',
   '[{"dept":"市场","items":["投放加码"]},{"dept":"运营","items":["各店冲刺"]},{"dept":"客服","items":["排班已出"]},{"dept":"仓储","items":["备货到位"]}]',
   '全年峰值大促，目标 GMV 冲历史新高。客服排班表已出、仓储提前备货、市场投放加码，各店同步冲刺。'),
  (2, '新旗舰键盘 全球首发', 'new', '2026-11-15', '2026-11-15',
   '[{"dept":"市场","items":["落地页 + KOL 寄测"]},{"dept":"设计","items":["主图 / 视频"]},{"dept":"运营","items":["首发折扣"]}]',
   '全球同步首发。市场出落地页并安排 KOL 寄测，设计产出主图与视频物料，运营配置首发折扣。'),
  (3, '公司 8 周年司庆', 'event', '2026-11-20', '2026-11-20',
   '[{"dept":"全员","items":["司庆活动"]},{"dept":"行政","items":["筹备"]}]',
   '公司 8 周年，全员活动。行政统筹筹备，鼓励各团队参与。'),
  (4, '黑五 Black Friday', 'sale', '2026-11-28', '2026-11-28',
   '[{"dept":"海外站","items":["主战场"]},{"dept":"客服","items":["海外时区排班"]},{"dept":"市场","items":["海外投放"]}]',
   '海外站主战场。客服按海外时区排班，市场加大海外投放，承接全年最大海外流量。'),
  (5, '网一 Cyber Monday', 'sale', '2026-12-01', '2026-12-01',
   '[{"dept":"运营","items":["承接黑五流量"]}]',
   '承接黑五流量的第二波。运营做好流量承接与转化优化。'),
  (6, '双十二', 'sale', '2026-12-12', '2026-12-12',
   '[{"dept":"国内","items":["收尾冲量"]}]',
   '国内收尾冲量，全年大促收官。'),
  (7, '冬季限定新色 首发', 'new', '2026-12-20', '2026-12-20',
   '[{"dept":"市场","items":["预热"]},{"dept":"设计","items":["视觉"]}]',
   '冬季限定新色首发。市场提前预热，设计产出视觉物料。')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `kind` = VALUES(`kind`),
  `start_date` = VALUES(`start_date`), `end_date` = VALUES(`end_date`),
  `dept_actions` = VALUES(`dept_actions`), `note` = VALUES(`note`);

-- 7. 早期样例提醒里的跳转还指着旧前端的 /personal?view=xxx，统一换成个人中心的真实路由。
UPDATE `notifications` SET `link` = CASE `type`
    WHEN 'new_product'       THEN '/me/rhythm'
    WHEN 'approval_pending'  THEN '/me/apply'
    WHEN 'anniversary'       THEN '/me/profile'
    WHEN 'report_due'        THEN '/me/report?tab=draft'
    ELSE '/me/reminder'
  END
WHERE `link` LIKE '/personal?view=%';
