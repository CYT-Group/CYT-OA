package com.cyt.center.module.oa.service.reimbursement;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementSaveReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementSubmitReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.reimbursement.OaReimbursementDO;

/**
 * 费用报销 Service 接口
 *
 * @author 芋道源码
 */
public interface OaReimbursementService {

    /**
     * 创建费用报销草稿
     *
     * @param saveReqVO 申请内容
     * @return 申请编号
     */
    Long createReimbursement(OaReimbursementSaveReqVO saveReqVO);

    /**
     * 更新本人草稿
     *
     * @param saveReqVO 申请内容
     * @param userId 当前用户编号
     */
    void updateReimbursement(OaReimbursementSaveReqVO saveReqVO, Long userId);

    /**
     * 提交本人草稿并发起审批
     *
     * @param submitReqVO 申请编号和自选审批人
     * @param userId 当前用户编号
     */
    void submitReimbursement(OaReimbursementSubmitReqVO submitReqVO, Long userId);

    /**
     * 从企业微信回读费用报销的审批状态
     *
     * @param id 申请编号
     * @param userId 当前用户编号
     * @return 同步后的申请
     */
    OaReimbursementDO syncReimbursementStatus(Long id, Long userId);

    /**
     * 获得费用报销
     *
     * @param id 申请编号
     * @return 费用报销
     */
    OaReimbursementDO getReimbursement(Long id);

    /**
     * 获得本人费用报销分页
     *
     * @param userId 申请人编号
     * @param pageReqVO 分页条件
     * @return 申请分页
     */
    PageResult<OaReimbursementDO> getReimbursementPage(Long userId, OaReimbursementPageReqVO pageReqVO);

    /**
     * 更新费用报销审批状态
     *
     * @param id 申请编号
     * @param status 审批状态
     */
    void updateReimbursementStatus(Long id, Integer status);

}
