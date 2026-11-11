-- =====================================================================
-- B3 制度签收：Portal 的 portal_handbook_signoff 搬到 OA MySQL。
-- 口径出处：docs/Portal管理面迁移契约.md §一「制度签收」、§二「类型转换口径」、§四 批次 3。
--
-- 签收记录：员工对某份 requires_signoff 文档的某个版本线上签收，人事档案存档。
-- UNIQUE(user_id, document, version) 保证一人对一版本只签一次（幂等）。
-- document_id 引用 portal_hr_documents.id（RESTRICT）；document 保留为历史字符串快照（slug）。
-- 催签（notify-unsigned）依赖企微推送通道，随推送切片落地；这里只迁签收记录本体。
-- =====================================================================

CREATE TABLE IF NOT EXISTS portal_handbook_signoff (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '签收人 sys_user.id（Portal 的 portal_users.id 原样保留）',
    username    VARCHAR(50)  NOT NULL COMMENT '签收人企微 userid 快照',
    document_id BIGINT       NULL COMMENT '稳定文档 ID → portal_hr_documents.id（RESTRICT）',
    document    VARCHAR(64)  NOT NULL COMMENT '文档标识快照（slug；老记录可能是 legacy alias）',
    version     VARCHAR(16)  NOT NULL,
    signed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip          VARCHAR(64)  NULL,
    user_agent  VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_handbook_signoff_user_doc_ver (user_id, document, version),
    KEY ix_handbook_signoff_document_id (document_id),
    CONSTRAINT fk_handbook_signoff_document FOREIGN KEY (document_id) REFERENCES portal_hr_documents (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '员工手册/制度文档线上签收记录（人事档案存档）';
