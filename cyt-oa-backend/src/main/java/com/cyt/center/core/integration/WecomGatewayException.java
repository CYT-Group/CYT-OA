package com.cyt.center.core.integration;

import com.cyt.center.core.exception.BizException;

/**
 * 企微网关返回的结构化失败。展示文案仍按 BizException 直接给用户看，
 * 但保留 error.type 与 trace_id，让调用方能区分「未授权」「重复提交」「上游超时」，
 * 而不是对着一句中文猜该重试还是该找管理员。
 */
public class WecomGatewayException extends BizException {

    private final String type;
    private final String traceId;
    private final String code;

    public WecomGatewayException(String type, String traceId, String message) {
        this(type, traceId, "", message);
    }

    public WecomGatewayException(String type, String traceId, String code, String message) {
        super(message);
        this.type = type == null || type.isBlank() ? "unknown" : type;
        this.traceId = traceId == null ? "" : traceId;
        this.code = code == null ? "" : code;
    }

    public String type() {
        return type;
    }

    public String traceId() {
        return traceId;
    }

    /** 仅保存经过白名单校验的企微错误码，不包含上游原始正文。 */
    public String code() { return code; }

    /** 同一请求号已经提交过了：原审批单在企微存在，应当回读而不是再发一单。 */
    public boolean isDuplicate() {
        return "conflict".equals(type)
                || "idempotency_conflict".equals(type)
                || "idempotency_in_progress".equals(type);
    }
}
