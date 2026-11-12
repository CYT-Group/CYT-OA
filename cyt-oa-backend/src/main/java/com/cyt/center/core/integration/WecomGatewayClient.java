package com.cyt.center.core.integration;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 通讯录与消息一律经 cyt-common-api 网关中转：企微 Secret 和 access_token 从不进入本服务，
 * 本服务只持有网关调用凭据。
 */
@Component
@RequiredArgsConstructor
public class WecomGatewayClient {

    /** 通讯录快照要翻完全员和全部门，给的预算最宽。 */
    private static final Duration DIRECTORY_TIMEOUT = Duration.ofSeconds(200);
    /** 审批类操作是用户在页面上等的同步调用，不能占着请求线程几分钟。 */
    public static final Duration APPROVAL_TIMEOUT = Duration.ofSeconds(30);
    /** 模板创建/更新在企微侧偶尔要 20 秒以上，按网关契约的 60 秒给。 */
    public static final Duration TEMPLATE_TIMEOUT = Duration.ofSeconds(60);

    private final CenterProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** 全员 + 全部门的快照，字段口径由网关固定。 */
    public JsonNode directory() {
        return call("wecom/directory/list", Map.of(), DIRECTORY_TIMEOUT);
    }

    public JsonNode hrFields() { return call("wecom/hr/get-fields", Map.of(), Duration.ofSeconds(45)); }

    public JsonNode hrStaff(String userid, java.util.Collection<WecomHrDates.Field> fields) {
        if (userid == null || userid.isBlank() || fields.isEmpty()) throw new BizException("企微花名册查询缺少员工或字段");
        return call("wecom/hr/get-staff-info", Map.of("userid", userid, "fieldids",
                fields.stream().map(WecomHrDates.Field::id).distinct().map(id -> Map.of("fieldid", id, "sub_idx", 0)).toList()), Duration.ofSeconds(45));
    }

    public JsonNode call(String operationPath, Map<String, Object> params) {
        return call(operationPath, params, DIRECTORY_TIMEOUT);
    }

    public JsonNode call(String operationPath, Map<String, Object> params, Duration timeout) {
        CenterProperties.Wecom wecom = properties.getWecom();
        String base = wecom.getGatewayUrl();
        if (base.isBlank()) {
            throw new BizException("未配置企微网关地址 cyt.wecom.gateway-url，通讯录同步、消息推送与审批均不可用");
        }
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create(trimEndSlashes(base) + "/integrations/" + operationPath))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(params), StandardCharsets.UTF_8));
        if (!wecom.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + wecom.getApiKey().trim());
        } else if (wecom.isAllowDevIdentity()) {
            builder.header("X-System-Id", wecom.getSystemId());
        } else {
            throw new BizException("请在服务端配置 common-api 的 API Key；本机联调可显式开启 cyt.wecom.allow-dev-identity");
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode envelope = parse(response.body());
            String traceId = safeIdentifier(envelope == null ? response.headers().firstValue("X-Trace-Id").orElse("")
                    : envelope.path("error").path("trace_id").asText(response.headers().firstValue("X-Trace-Id").orElse("")));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || envelope == null
                    || !envelope.path("ok").asBoolean(false)) {
                // 不透传任意上游响应，避免把网关配置、请求参数或个人资料写进错误页。
                String type = envelope == null ? "unknown" : safeIdentifier(envelope.path("error").path("type").asText("unknown"));
                if (type.isEmpty()) type = "unknown";
                String code = envelope == null ? "" : safeCode(envelope.path("error").path("code").asText(""));
                if (code.isEmpty()) code = safeCode(type);
                throw new WecomGatewayException(type, traceId, code, "企微网关调用失败（HTTP " + response.statusCode() + "，"
                        + type + "，追踪号 " + traceId + "），请检查网关密钥、操作授权和企微应用配置");
            }
            if (!envelope.path("data").isObject()) {
                throw new WecomGatewayException("bad_envelope", traceId, "企微网关未返回 data，未执行任何同步");
            }
            if ((operationPath.startsWith("wecom/hr/") || operationPath.startsWith("wecom/directory/"))
                    && envelope.path("meta").path("mock").asBoolean(false)) {
                throw new BizException("企微网关处于模拟模式，未写入任何通讯录或人事资料");
            }
            if (operationPath.startsWith("wecom/hr/") && envelope.path("data").has("errcode")
                    && envelope.path("data").path("errcode").asInt(-1) != 0) {
                String code = safeCode(envelope.path("data").path("errcode").asText(""));
                throw new WecomGatewayException("upstream_error", traceId, code, "企微人事接口未成功返回资料，请检查应用权限与员工账号");
            }
            return envelope.path("data");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BizException("企微网关请求已中断，未执行同步");
        } catch (IOException error) {
            throw new BizException("企微网关连接失败或超时，请稍后重试");
        }
    }

    private static String safeIdentifier(String value) {
        return value != null && value.matches("[A-Za-z0-9_.:-]{1,128}") ? value : "";
    }

    private static String safeCode(String value) {
        if (value == null || !value.matches("(?:WECOM_)?\\d{1,10}")) return "";
        return value.startsWith("WECOM_") ? value : "WECOM_" + value;
    }

    private JsonNode parse(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (IOException error) {
            return null;
        }
    }

    private String writeJson(Map<String, Object> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException error) {
            throw new BizException("企微网关请求体序列化失败");
        }
    }

    private static String trimEndSlashes(String value) {
        String result = value.trim();
        while (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

}
