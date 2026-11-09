package com.cyt.center.module.approval.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.cyt.center.core.integration.WecomApprovalClient;
import com.cyt.center.core.integration.WecomGatewayException;
import com.cyt.center.module.approval.dal.dataobject.WecomApprovalLinkDO;
import com.cyt.center.module.approval.dal.mysql.WecomApprovalLinkMapper;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.approval.enums.ErrorCodeConstants.*;

/**
 * 业务单据到企微审批的统一桥：所有审批（请假、加班、报销、出差、用章、领用、考勤请假）
 * 都经这里出单、按链接表回读状态，业务侧只负责把字段填进模板别名。
 *
 * 幂等口径：request_id = oa-{type}-{bizId}。请求已发出但本地回写失败时，网关会用 conflict
 * 挡回重复提交，此时按原请求号认回原单，不再换号重发。
 */
@Service
@RequiredArgsConstructor
public class WecomApprovalBridge {

    private final WecomApprovalClient client;
    private final WecomApprovalLinkMapper linkMapper;
    private final AdminUserApi adminUserApi;

    /** 员工编号转企微成员 ID；没有企微账号的在这里拦下，比让网关报上游错误好定位。 */
    public String wecomUserid(Long userId) {
        return wecomUserids(List.of(userId)).getFirst();
    }

    public List<String> wecomUserids(Collection<Long> userIds) {
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        List<String> result = new ArrayList<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = users.get(userId);
            if (user == null || StrUtil.isBlank(user.getUsername())) {
                throw exception(WECOM_IDENTITY_MISSING,
                        user == null ? "编号 " + userId + " 的员工" : user.getNickname());
            }
            if (!result.contains(user.getUsername())) {
                result.add(user.getUsername());
            }
        }
        return result;
    }

    /**
     * 提交企微审批单并落链接表，返回企微单号。
     *
     * @param template 网关里绑定的业务模板别名
     * @param approvers 顺序审批人的企微 userid；为空时用企微模板自带的审批流
     */
    public String submit(String type, String template, Long bizId, String applicant,
                         List<String> approvers, Map<String, Object> fields, List<String> summary) {
        WecomApprovalLinkDO existing = linkMapper.selectByTypeAndBizId(type, bizId);
        String requestId = "oa-" + type + "-" + bizId;
        if (existing != null && StrUtil.isNotBlank(existing.getApprovalNo())) {
            return existing.getApprovalNo();
        }
        String approvalNo;
        try {
            approvalNo = CollUtil.isEmpty(approvers)
                    ? client.submitWithTemplateFlow(requestId, template, applicant, fields, summary)
                    : client.submit(requestId, template, applicant, approvers, fields, summary);
        } catch (WecomGatewayException error) {
            if (!error.isDuplicate()) {
                throw error;
            }
            approvalNo = client.track(requestId).approvalNo();
        }
        linkMapper.upsert(new WecomApprovalLinkDO().setApplyType(type).setBizId(bizId)
                .setRequestId(requestId).setApprovalNo(approvalNo));
        return approvalNo;
    }

    /**
     * 回读企微审批状态。链接表优先；legacy 参数兼容迁到链接表之前落库的老数据（请假）。
     */
    public WecomApprovalClient.Approval sync(String type, Long bizId,
                                             String legacyRequestId, String legacyApprovalNo) {
        WecomApprovalLinkDO link = linkMapper.selectByTypeAndBizId(type, bizId);
        String requestId = link == null ? legacyRequestId : link.getRequestId();
        String approvalNo = link == null ? legacyApprovalNo : link.getApprovalNo();
        if (StrUtil.isBlank(requestId) && StrUtil.isBlank(approvalNo)) {
            throw exception(WECOM_APPLY_NOT_SUBMITTED);
        }
        WecomApprovalClient.Approval approval = StrUtil.isBlank(requestId)
                ? client.get(approvalNo) : client.track(requestId);
        if (link != null && StrUtil.isBlank(link.getApprovalNo())
                && StrUtil.isNotBlank(approval.approvalNo())) {
            linkMapper.upsert(new WecomApprovalLinkDO().setApplyType(type).setBizId(bizId)
                    .setRequestId(requestId).setApprovalNo(approval.approvalNo()));
        }
        return approval;
    }

    /** 链接表里有没有这张单的企微链接（提交过的判断依据）。 */
    public boolean linked(String type, Long bizId) {
        return linkMapper.selectByTypeAndBizId(type, bizId) != null;
    }

    /**
     * 企微状态词表映射到 OA 的审批状态；审批中或未识别的状态返回 null，表示本地不动。
     * 企微侧的 cancelled / approval_revoked / deleted 都是「这张单不再往下走」，统一记为已取消。
     */
    public static Integer toStatus(WecomApprovalClient.Approval approval) {
        return switch (approval.status()) {
            case "approved" -> BpmProcessInstanceStatusEnum.APPROVE.getStatus();
            case "rejected" -> BpmProcessInstanceStatusEnum.REJECT.getStatus();
            case "cancelled", "approval_revoked", "deleted" -> BpmProcessInstanceStatusEnum.CANCEL.getStatus();
            default -> null;
        };
    }

    /** 企微摘要最多 3 行、每行 20 个字符，超出的截断。 */
    public static String brief(String text) {
        return text.length() <= 20 ? text : text.substring(0, 20);
    }

    /** 时长/日期区间控件的取值：start_at / end_at 用 Unix 秒，duration_seconds 由业务按工作日口径算好。 */
    public static Map<String, Object> durationField(java.time.LocalDateTime startTime,
                                                    java.time.LocalDateTime endTime, long durationSeconds) {
        Map<String, Object> value = new java.util.LinkedHashMap<>();
        value.put("start_at", com.cyt.center.framework.common.util.date.LocalDateTimeUtils.toEpochSecond(startTime));
        value.put("end_at", com.cyt.center.framework.common.util.date.LocalDateTimeUtils.toEpochSecond(endTime));
        value.put("duration_seconds", durationSeconds);
        return value;
    }

}
