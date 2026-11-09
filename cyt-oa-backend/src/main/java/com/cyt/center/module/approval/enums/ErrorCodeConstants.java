package com.cyt.center.module.approval.enums;

import com.cyt.center.framework.common.exception.ErrorCode;

/**
 * 企微审批桥的报错码，段位 1_055。
 */
public interface ErrorCodeConstants {

    ErrorCode WECOM_IDENTITY_MISSING = new ErrorCode(1_055_000_001, "{}没有企业微信账号，无法发起审批");
    ErrorCode WECOM_APPLY_NOT_SUBMITTED = new ErrorCode(1_055_000_002, "该申请还没有提交到企业微信审批");
    ErrorCode WECOM_APPLY_DRAFT = new ErrorCode(1_055_000_003, "该申请还没提交审批，无需撤销，可直接修改后重新提交");
    ErrorCode WECOM_CANCEL_IN_WECOM = new ErrorCode(1_055_000_004, "审批单在企业微信里，请在企业微信撤回这张单，再回到本页点「同步审批状态」更新结果");

}
