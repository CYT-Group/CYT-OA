package com.cyt.center.module.oa.service.leave;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySubmitReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplyPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.leave.OaLeaveApplyDO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * 请假申请 Service 接口
 *
 * @author 芋道源码
 */
public interface OaLeaveApplyService {

    /**
     * 创建请假申请草稿
     *
     * @param saveReqVO 申请内容
     * @return 申请编号
     */
    Long createLeaveApply(OaLeaveApplySaveReqVO saveReqVO);

    /**
     * 更新本人请假申请草稿
     *
     * @param saveReqVO 申请内容
     * @param userId 当前用户编号
     */
    void updateLeaveApply(OaLeaveApplySaveReqVO saveReqVO, Long userId);

    /**
     * 提交本人请假申请草稿并在企业微信发起审批
     *
     * @param submitReqVO 申请编号和自选审批人
     * @param userId 当前用户编号
     */
    void submitLeaveApply(OaLeaveApplySubmitReqVO submitReqVO, Long userId);

    /**
     * 从企业微信回读本人申请的审批结果并回写状态；审批中或未识别的结果不改本地状态
     *
     * @param id 申请编号
     * @param userId 当前用户编号
     * @return 回写后的申请
     */
    OaLeaveApplyDO syncLeaveApplyStatus(Long id, Long userId);

    /**
     * 获得请假申请
     *
     * @param id 申请编号
     * @return 请假申请
     */
    OaLeaveApplyDO getLeaveApply(Long id);

    /**
     * 获得本人请假申请分页
     *
     * @param userId 申请人编号
     * @param pageReqVO 分页条件
     * @return 申请分页
     */
    PageResult<OaLeaveApplyDO> getLeaveApplyPage(Long userId, OaLeaveApplyPageReqVO pageReqVO);

    /**
     * 获得用户审批通过的请假天数合计
     *
     * @param userIds 用户编号集合
     * @param startTime 申请开始时间范围，包含边界；跨月申请整笔计入开始月份
     * @return 用户编号与请假天数的映射，无申请的用户不返回
     */
    Map<Long, Integer> getApprovedLeaveDaysMap(Collection<Long> userIds, LocalDateTime[] startTime);

    /**
     * 更新请假申请审批状态
     *
     * @param id 申请编号
     * @param status 审批状态
     */
    void updateLeaveApplyStatus(Long id, Integer status);

}
