package com.cyt.center.core.integration;

import com.cyt.center.framework.common.util.date.LocalDateTimeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 请假审批在企微侧的三个动作：提交、按请求号回读、以及把它们需要的参数拼成网关契约。
 * 企微 Secret 与 access_token 仍留在网关侧，本服务只认 request_id 与 approval_no。
 */
@Component
@RequiredArgsConstructor
public class WecomApprovalClient {

    private final WecomGatewayClient gateway;

    /** 企微审批详情里的状态：pending / approved / rejected / cancelled / approval_revoked / deleted / unknown。 */
    public record Approval(String approvalNo, int statusCode, String status, String statusLabel, boolean terminal) {
    }

    /**
     * 提交一张审批单。requestId 决定幂等：同一个 requestId 重复提交会被网关挡回 conflict，
     * 调用方应当改用 {@link #track} 回读原单，而不是换个号再发。
     *
     * @param template 网关里绑定的业务模板别名，不是企微模板 ID
     * @param approvers 审批人的企微 userid，按顺序依次审批
     */
    public String submit(String requestId, String template, String applicant, List<String> approvers,
                         Map<String, Object> fields, List<String> summary) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "approve");
        node.put("mode", "sequential");
        node.put("users", approvers);
        List<Map<String, Object>> flow = new ArrayList<>();
        flow.add(node);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("template", template);
        params.put("applicant", applicant);
        params.put("request_id", requestId);
        params.put("fields", fields);
        params.put("flow", flow);
        if (!summary.isEmpty()) {
            params.put("summary", summary);
        }
        return gateway.call("wecom/approvals/submit", params, WecomGatewayClient.APPROVAL_TIMEOUT)
                .path("approval_no").asText("");
    }

    /** 提交一张审批单，审批链用企微模板自带的审批流（不传 flow，网关会按 use_template_approver=1 出单）。 */
    public String submitWithTemplateFlow(String requestId, String template, String applicant,
                                         Map<String, Object> fields, List<String> summary) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("template", template);
        params.put("applicant", applicant);
        params.put("request_id", requestId);
        params.put("fields", fields);
        if (!summary.isEmpty()) {
            params.put("summary", summary);
        }
        return gateway.call("wecom/approvals/submit", params, WecomGatewayClient.APPROVAL_TIMEOUT)
                .path("approval_no").asText("");
    }

    /** 按提交时的请求号回读审批状态。 */
    public Approval track(String requestId) {
        return readStatus(gateway.call("wecom/approvals/track", Map.of("request_id", requestId),
                WecomGatewayClient.APPROVAL_TIMEOUT));
    }

    /** 按企微单号回读审批状态，用于请求号缺失的历史单据。 */
    public Approval get(String approvalNo) {
        return readStatus(detail(approvalNo));
    }

    /**
     * 按企微单号取审批单原始详情。网关在这里返回 template_id，
     * {@link #get} 只留状态会把它丢掉，模板绑定回填必须走这个原始口径。
     */
    public JsonNode detail(String approvalNo) {
        return gateway.call("wecom/approvals/get", Map.of("approval_no", approvalNo),
                WecomGatewayClient.APPROVAL_TIMEOUT);
    }

    // ===== 审批模板管理：企微没有模板列表接口，列表以网关绑定表为口径 =====

    /** 本系统已绑定的模板列表（别名 → 企微模板 ID）。 */
    public JsonNode templates() {
        return gateway.call("wecom/approval_templates/list", Map.of(), WecomGatewayClient.TEMPLATE_TIMEOUT);
    }

    /** 模板详情：绑定的别名或裸 template_id 都能查，绑定前预览用裸 ID。 */
    public JsonNode templateDetail(String template) {
        return gateway.call("wecom/approval_templates/get", Map.of("template", template),
                WecomGatewayClient.TEMPLATE_TIMEOUT);
    }

    /** 绑定/改绑：网关会先拉模板详情校验字段别名对应的控件 ID，再落绑定表。 */
    public JsonNode templateBind(Map<String, Object> params) {
        return gateway.call("wecom/approval_templates/bind", params, WecomGatewayClient.TEMPLATE_TIMEOUT);
    }

    public JsonNode templateUnbind(String alias) {
        return gateway.call("wecom/approval_templates/unbind", Map.of("alias", alias),
                WecomGatewayClient.TEMPLATE_TIMEOUT);
    }

    /**
     * 在企微上真实新建/更新模板，别名非空时自动绑定；克隆以已有别名为源。
     * 网关对模板写操作强制幂等键，模板没有业务主键可派生，故一次用户动作一个键。
     */
    public JsonNode templateSave(String operation, Map<String, Object> params) {
        Map<String, Object> body = new LinkedHashMap<>(params);
        body.put("request_id", "oa-tpl-" + operation + "-" + UUID.randomUUID().toString().replace("-", ""));
        return gateway.call("wecom/approval_templates/" + operation, body, WecomGatewayClient.TEMPLATE_TIMEOUT);
    }

    /**
     * 请假时长控件的取值。企微要求显式给出时长，网关不会替调用方把非工作时间剔除，
     * 所以这里按 OA 已算好的向上取整天数换算，保证页面天数与审批单时长口径一致。
     */
    public static Map<String, Object> vacationField(String leaveTypeName, LocalDateTime startTime,
                                                    LocalDateTime endTime, int days) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("leave_type", leaveTypeName);
        value.put("start_at", epoch(startTime));
        value.put("end_at", epoch(endTime));
        value.put("duration_seconds", (long) days * 24 * 3600);
        return value;
    }

    private static long epoch(LocalDateTime time) {
        return LocalDateTimeUtils.toEpochSecond(time);
    }

    private static Approval readStatus(JsonNode data) {
        return new Approval(data.path("approval_no").asText(""), data.path("status_code").asInt(0),
                data.path("status").asText("unknown"), data.path("status_label").asText(""),
                data.path("terminal").asBoolean(false));
    }
}
