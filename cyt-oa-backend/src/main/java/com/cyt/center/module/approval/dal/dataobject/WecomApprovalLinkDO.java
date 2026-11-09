package com.cyt.center.module.approval.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyt.center.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务单据与企微审批单的链接：所有走企微审批的单据共用这一张表，
 * 不再往每张业务表里加 wecom_request_id / wecom_approval_no 两列。
 */
@TableName("oa_wecom_approval_link")
@Data
@EqualsAndHashCode(callSuper = true)
public class WecomApprovalLinkDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务类型：leave / overtime / reimbursement / travel / travel-reimbursement / seal / supply / hrm-leave */
    private String applyType;
    /** 业务单据编号 */
    private Long bizId;
    /** 提交时的幂等请求号：oa-{applyType}-{bizId} */
    private String requestId;
    /** 企微审批单号，出单后回填 */
    private String approvalNo;

}
