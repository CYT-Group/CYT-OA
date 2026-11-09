-- 业务单据与企微审批单的链接表（审批统一走企微后新增）
-- 执行库：cyt_oa_center
CREATE TABLE IF NOT EXISTS oa_wecom_approval_link (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    apply_type VARCHAR(32) NOT NULL COMMENT '业务类型：leave/overtime/reimbursement/travel/travel-reimbursement/seal/supply/hrm-leave',
    biz_id BIGINT NOT NULL COMMENT '业务单据编号',
    request_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '提交幂等请求号 oa-{type}-{bizId}',
    approval_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '企微审批单号，出单后回填',
    creator VARCHAR(32) DEFAULT '' COMMENT '创建者',
    updater VARCHAR(32) DEFAULT '' COMMENT '更新者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_biz (apply_type, biz_id),
    UNIQUE KEY uk_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务单据与企微审批单链接表';
