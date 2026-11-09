package com.cyt.center.module.oa.service.leave;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.integration.WecomGatewayException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.date.LocalDateTimeUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySubmitReqVO;
import com.cyt.center.module.oa.dal.dataobject.leave.OaLeaveApplyDO;
import com.cyt.center.module.oa.dal.mysql.leave.OaLeaveApplyMapper;
import com.cyt.center.module.oa.enums.leave.OaLeaveTypeEnum;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import com.cyt.center.module.oa.service.attendance.OaAttendanceService;
import com.cyt.center.module.oa.enums.attendance.OaAttendanceTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.oa.enums.ErrorCodeConstants.*;

/**
 * 请假申请 Service 实现类
 *
 * 请假的审批出口在企业微信（经 common-api 网关），不再起 OA 内部的 Warm-Flow 流程；
 * OA 只保留单据本身，审批结论由页面上的「同步审批状态」按企微单号回读。
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OaLeaveApplyServiceImpl implements OaLeaveApplyService {

    /** 网关模板绑定里的业务别名：请假时长控件与请假事由控件 */
    private static final String FIELD_VACATION = "vacation";
    private static final String FIELD_REASON = "reason";
    /** 企微摘要最多 3 行、每行 20 字 */
    private static final int SUMMARY_LINE_LENGTH = 20;

    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private OaAttendanceService attendanceService;

    @Resource
    private OaLeaveApplyMapper leaveApplyMapper;

    @Resource
    private WecomApprovalClient approvals;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private CenterProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLeaveApply(OaLeaveApplySaveReqVO saveReqVO) {
        // 1. 计算并校验单次请假天数
        Integer days = LocalDateTimeUtils.getDaysBetweenCeiling(saveReqVO.getStartTime(), saveReqVO.getEndTime());
        validateLeaveApplyDays(saveReqVO.getType(), days);

        // 2. 转换请假内容，保存草稿
        OaLeaveApplyDO leaveApply = BeanUtils.toBean(saveReqVO, OaLeaveApplyDO.class)
                .setId(null).setStatus(BpmProcessInstanceStatusEnum.NOT_START.getStatus())
                .setDays(days);
        leaveApplyMapper.insert(leaveApply);
        return leaveApply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLeaveApply(OaLeaveApplySaveReqVO saveReqVO, Long userId) {
        // 1.1 校验申请可修改
        validateLeaveApplyEditable(saveReqVO.getId(), userId);
        // 1.2 计算并校验单次请假天数
        Integer days = LocalDateTimeUtils.getDaysBetweenCeiling(saveReqVO.getStartTime(), saveReqVO.getEndTime());
        validateLeaveApplyDays(saveReqVO.getType(), days);

        // 2. 更新请假申请
        OaLeaveApplyDO leaveApply = BeanUtils.toBean(saveReqVO, OaLeaveApplyDO.class)
                .setDays(days);
        leaveApplyMapper.updateById(leaveApply);
    }

    /**
     * 提交请假：把单据推到企业微信生成审批单。
     *
     * 这里故意不开事务——出单是一次最长 30 秒的外部调用，占着数据库连接没有意义，
     * 而落库只剩最后一条 update。幂等靠固定请求号 oa-leave-{id}：请求已发出但本地回写失败时，
     * 网关会用 conflict 挡回重复提交，此时按原请求号认回那张单，不再换号重发。
     */
    @Override
    public void submitLeaveApply(OaLeaveApplySubmitReqVO submitReqVO, Long userId) {
        // 1.1 校验申请可提交
        OaLeaveApplyDO leaveApply = validateLeaveApplyEditable(submitReqVO.getId(), userId);
        // 1.2 校验历史草稿的单次请假天数
        validateLeaveApplyDays(leaveApply.getType(), leaveApply.getDays());
        // 1.3 审批人是草稿里选好的，提交时换成企微成员 ID
        if (CollUtil.isEmpty(leaveApply.getApprovers())) {
            throw exception(LEAVE_APPLY_WECOM_APPROVER_EMPTY);
        }
        List<String> approvers = wecomUserIds(leaveApply.getApprovers());
        String applicant = wecomUserIds(List.of(userId)).get(0);

        // 2. 发起企微审批
        String requestId = wecomRequestId(leaveApply.getId());
        String approvalNo;
        try {
            approvalNo = approvals.submit(requestId, properties.getWecom().getLeaveTemplate(), applicant,
                    approvers, approvalFields(leaveApply), approvalSummary(leaveApply));
        } catch (WecomGatewayException error) {
            if (!error.isDuplicate()) {
                throw error;
            }
            approvalNo = approvals.track(requestId).approvalNo();
        }

        // 3. 状态改为审批中并绑定企微单号
        leaveApplyMapper.updateById(new OaLeaveApplyDO().setId(leaveApply.getId())
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus())
                .setWecomRequestId(requestId).setWecomApprovalNo(approvalNo));
    }

    @Override
    public OaLeaveApplyDO syncLeaveApplyStatus(Long id, Long userId) {
        // 1. 校验本人已提交的申请
        OaLeaveApplyDO leaveApply = validateLeaveApplyExists(id);
        if (ObjUtil.notEqual(leaveApply.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }
        String requestId = leaveApply.getWecomRequestId();
        String approvalNo = leaveApply.getWecomApprovalNo();
        if (StrUtil.isBlank(requestId) && StrUtil.isBlank(approvalNo)) {
            throw exception(LEAVE_APPLY_NOT_SUBMITTED);
        }

        // 2. 回读企微：优先用请求号，历史单据退化为按单号查
        WecomApprovalClient.Approval approval = StrUtil.isBlank(requestId)
                ? approvals.get(approvalNo) : approvals.track(requestId);
        Integer status = toApprovalStatus(approval);
        if (status == null) {
            return leaveApply;
        }
        updateLeaveApplyStatus(id, status);
        return leaveApplyMapper.selectById(id);
    }

    /**
     * 企微状态词表映射到 OA 的审批状态；审批中或未识别的状态返回 null，表示本地不动。
     * 企微侧的 cancelled/approval_revoked/deleted 都是「这张单不再往下走」，统一记为已取消。
     */
    private static Integer toApprovalStatus(WecomApprovalClient.Approval approval) {
        return switch (approval.status()) {
            case "approved" -> BpmProcessInstanceStatusEnum.APPROVE.getStatus();
            case "rejected" -> BpmProcessInstanceStatusEnum.REJECT.getStatus();
            case "cancelled", "approval_revoked", "deleted" -> BpmProcessInstanceStatusEnum.CANCEL.getStatus();
            default -> null;
        };
    }

    /** 审批单表单值：键用网关绑定里的业务别名，控件 ID 与中文字段名由网关的 field_aliases 负责翻译。 */
    private Map<String, Object> approvalFields(OaLeaveApplyDO leaveApply) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(FIELD_VACATION, WecomApprovalClient.vacationField(leaveTypeName(leaveApply.getType()),
                leaveApply.getStartTime(), leaveApply.getEndTime(), leaveApply.getDays()));
        fields.put(FIELD_REASON, leaveApply.getReason());
        return fields;
    }

    private List<String> approvalSummary(OaLeaveApplyDO leaveApply) {
        return List.of(
                brief("类型：" + leaveTypeName(leaveApply.getType())),
                brief("天数：" + leaveApply.getDays() + " 天"),
                brief("事由：" + leaveApply.getReason()));
    }

    private static String leaveTypeName(Integer type) {
        OaLeaveTypeEnum leaveType = OaLeaveTypeEnum.valueOf(type);
        return leaveType == null ? "请假" : leaveType.getName();
    }

    /** 企微摘要每行不超过 20 个字符，超出的截断，完整事由仍写在表单里。 */
    private static String brief(String text) {
        return text.length() <= SUMMARY_LINE_LENGTH ? text : text.substring(0, SUMMARY_LINE_LENGTH);
    }

    private static String wecomRequestId(Long id) {
        return "oa-leave-" + id;
    }

    /**
     * 员工编号转企微成员 ID。sys_user.username 就是通讯录同步下来的 userid，
     * 缺失说明这个人不在企微通讯录里，提前拦下来比让网关报一个上游错误更好定位。
     */
    private List<String> wecomUserIds(Collection<Long> userIds) {
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        List<String> result = new ArrayList<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = users.get(userId);
            if (user == null || StrUtil.isBlank(user.getUsername())) {
                throw exception(LEAVE_APPLY_WECOM_IDENTITY_MISSING,
                        user == null ? "编号 " + userId + " 的员工" : user.getNickname());
            }
            if (!result.contains(user.getUsername())) {
                result.add(user.getUsername());
            }
        }
        return result;
    }

    @Override
    public OaLeaveApplyDO getLeaveApply(Long id) {
        return leaveApplyMapper.selectById(id);
    }

    @Override
    public PageResult<OaLeaveApplyDO> getLeaveApplyPage(Long userId, OaLeaveApplyPageReqVO pageReqVO) {
        return leaveApplyMapper.selectPage(userId, pageReqVO);
    }

    @Override
    public Map<Long, Integer> getApprovedLeaveDaysMap(Collection<Long> userIds, LocalDateTime[] startTime) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return leaveApplyMapper.selectDaysMapByCreatorsAndStatusAndStartTime(
                userIds, BpmProcessInstanceStatusEnum.APPROVE.getStatus(), startTime);
    }

    /**
     * 校验单次请假天数，不校验年度累计额度
     *
     * @param type 请假类型
     * @param days 请假天数
     */
    private void validateLeaveApplyDays(Integer type, Integer days) {
        OaLeaveTypeEnum leaveType = OaLeaveTypeEnum.valueOf(type);
        if (leaveType != null && leaveType.getMaxDays() != null && days > leaveType.getMaxDays()) {
            throw exception(LEAVE_APPLY_DAYS_EXCEEDED, leaveType.getName(), leaveType.getMaxDays());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLeaveApplyStatus(Long id, Integer status) {
        // 1. 校验申请存在
        OaLeaveApplyDO apply = validateLeaveApplyExists(id);
        // 「同步审批状态」是用户可以反复点的按钮，状态没变就整段跳过，否则每次点都会多写一条请假考勤
        if (ObjUtil.equals(apply.getStatus(), status)) {
            return;
        }

        // 2. 更新审批状态
        leaveApplyMapper.updateById(new OaLeaveApplyDO().setId(id).setStatus(status));

        // 3. 审批通过后生成考勤明细
        if (ObjUtil.equal(status, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            attendanceService.createApplyAttendance(Long.valueOf(apply.getCreator()),
                    OaAttendanceTypeEnum.LEAVE, apply.getStartTime());
        }
    }

    /**
     * 校验申请可修改或提交
     *
     * @param id 申请编号
     * @param userId 当前用户编号
     * @return 本人的未提交申请
     */
    private OaLeaveApplyDO validateLeaveApplyEditable(Long id, Long userId) {
        // 1. 查询申请
        OaLeaveApplyDO leaveApply = leaveApplyMapper.selectById(id);
        if (leaveApply == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        // 2. 校验本人归属
        if (ObjUtil.notEqual(leaveApply.getCreator(), userId.toString())) {
            throw exception(APPLY_ACCESS_DENIED);
        }
        // 3. 只有未提交草稿允许修改或提交
        if (ObjUtil.notEqual(leaveApply.getStatus(), BpmProcessInstanceStatusEnum.NOT_START.getStatus())) {
            throw exception(APPLY_STATUS_INVALID);
        }
        return leaveApply;
    }

    /**
     * 校验请假申请存在
     *
     * @param id 申请编号
     * @return 申请
     */
    @SuppressWarnings("UnusedReturnValue")
    private OaLeaveApplyDO validateLeaveApplyExists(Long id) {
        OaLeaveApplyDO leaveApply = leaveApplyMapper.selectById(id);
        if (leaveApply == null) {
            throw exception(APPLY_NOT_EXISTS);
        }
        return leaveApply;
    }

}
