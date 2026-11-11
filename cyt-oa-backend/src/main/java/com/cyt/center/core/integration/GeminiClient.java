package com.cyt.center.core.integration;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini 文本生成客户端（迁自 Portal 的 services/ai/providers.py 的 _complete_gemini 一段）。
 *
 * 只做「发 generateContent 请求 + 异常翻译」这一件事；任务注册表、限速、结构化重试、记账、日志
 * 等横切事在更上层的 AI 网关里做（对齐 Portal 的分层：providers 只发请求）。
 *
 * 安全红线（照搬 Portal）：key 只放请求头 x-goog-api-key，不进 URL、不进日志/异常/审计。
 * key 优先读配置 cyt.ai.gemini-key，其次读环境变量 GEMINI_API_KEY；都缺 fail-closed 抛异常。
 */
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final String GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";

    private final CenterProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** 文本生成：返回模型正文；JSON 模式（jsonSchema 非空）返回原始 JSON 字符串。 */
    public String generate(String model, String system, String userText, int maxTokens,
                           String thinkingLevel, JsonNode jsonSchema) {
        return generateDetailed(model, system, userText, maxTokens, thinkingLevel, jsonSchema, null).text();
    }

    public record Generated(String text, Long tokensIn, Long tokensOut) {}

    public boolean configured(String managedKey) {
        return managedKey != null && !managedKey.isBlank() || !key().isBlank();
    }

    public Generated generateDetailed(String model, String system, String userText, int maxTokens,
                                     String thinkingLevel, JsonNode jsonSchema, String managedKey) {
        if (model == null || !model.matches("[a-zA-Z0-9._-]{1,100}")) throw new BizException("模型标识无效");
        String apiKey = managedKey == null || managedKey.isBlank() ? key() : managedKey;
        if (apiKey.isBlank()) {
            throw new BizException("AI 服务未配置：请设置 cyt.ai.gemini-key 或 GEMINI_API_KEY");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GENERATE_URL.replace("{model}", model)))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(system, userText, maxTokens,
                        thinkingLevel, jsonSchema), StandardCharsets.UTF_8))
                .build();
        return execute(request, apiKey);
    }

    /** PDF/图片附件只通过请求体传送；文件名和个人资料不进入 URL、日志或异常。 */
    public String generateDocument(String model, String system, String userText, String mimeType,
                                   byte[] content, int maxTokens, JsonNode jsonSchema) {
        return generateDocumentDetailed(model, system, userText, mimeType, content, maxTokens, jsonSchema, null).text();
    }

    public Generated generateDocumentDetailed(String model, String system, String userText, String mimeType,
                                               byte[] content, int maxTokens, JsonNode jsonSchema, String managedKey) {
        if (model == null || !model.matches("[a-zA-Z0-9._-]{1,100}")) throw new BizException("模型标识无效");
        String apiKey = managedKey == null || managedKey.isBlank() ? key() : managedKey;
        if (apiKey.isBlank()) throw new BizException("AI 服务未配置：请设置 cyt.ai.gemini-key 或 GEMINI_API_KEY");
        if (content == null || content.length == 0 || content.length > 10 * 1024 * 1024)
            throw new BizException("AI 附件须为 1 字节至 10MB");
        String body;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode data = (com.fasterxml.jackson.databind.node.ObjectNode)
                    objectMapper.readTree(buildBody(system, userText, maxTokens, null, jsonSchema));
            com.fasterxml.jackson.databind.node.ArrayNode parts = (com.fasterxml.jackson.databind.node.ArrayNode)
                    data.path("contents").get(0).path("parts");
            parts.addObject().putObject("inlineData").put("mimeType", mimeType)
                    .put("data", java.util.Base64.getEncoder().encodeToString(content));
            body = objectMapper.writeValueAsString(data);
        } catch (IOException ex) { throw new BizException("AI 附件序列化失败"); }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GENERATE_URL.replace("{model}", model)))
                .timeout(Duration.ofSeconds(120)).header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return execute(request, apiKey);
    }

    private Generated execute(HttpRequest request, String apiKey) {
        try {
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode data = objectMapper.readTree(resp.body());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("AI 上游请求失败（HTTP " + resp.statusCode() + "）："
                        + sanitize(errorMessage(data, resp.body()), apiKey));
            }
            String text = extractText(data);
            if (text == null) {
                String block = data.path("promptFeedback").path("blockReason").asText(null);
                throw new BizException(block != null ? "AI 拒绝处理：" + block : "AI 未返回可解析的结果");
            }
            JsonNode usage = data.path("usageMetadata");
            Long tokensIn = usage.has("promptTokenCount") ? usage.path("promptTokenCount").asLong() : null;
            Long tokensOut = usage.has("candidatesTokenCount")
                    ? usage.path("candidatesTokenCount").asLong() + usage.path("thoughtsTokenCount").asLong(0) : null;
            return new Generated(text, tokensIn, tokensOut);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("AI 请求已中断");
        } catch (IOException e) {
            throw new BizException("AI 上游连接失败或超时，请稍后重试");
        }
    }

    private String buildBody(String system, String userText, int maxTokens, String thinkingLevel,
                             JsonNode jsonSchema) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("maxOutputTokens", maxTokens);
        if (jsonSchema != null) {
            config.put("responseMimeType", "application/json");
            config.put("responseJsonSchema", jsonSchema);
        }
        if (thinkingLevel != null && !thinkingLevel.isBlank()) {
            config.put("thinkingConfig", Map.of("thinkingLevel", thinkingLevel));
        }
        List<Map<String, Object>> parts = new ArrayList<>();
        if (userText != null && !userText.isBlank()) {
            parts.add(Map.of("text", userText));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        body.put("generationConfig", config);
        if (system != null && !system.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", system))));
        }
        return writeJson(body);
    }

    /** 取首个候选的非 thought 文本拼接；没有返回 null。 */
    private String extractText(JsonNode data) {
        JsonNode candidates = data.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (JsonNode part : candidates.get(0).path("content").path("parts")) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            JsonNode text = part.get("text");
            if (text != null && text.isTextual()) {
                out.append(text.asText());
            }
        }
        return out.toString();
    }

    private String key() {
        String fromConfig = properties.getAi().getGeminiKey();
        if (fromConfig != null && !fromConfig.isBlank()) {
            return fromConfig.trim();
        }
        String fromEnv = System.getenv("GEMINI_API_KEY");
        return fromEnv == null ? "" : fromEnv.trim();
    }

    private String errorMessage(JsonNode data, String fallbackBody) {
        JsonNode message = data.path("error").path("message");
        if (message.isTextual()) {
            return message.asText();
        }
        return fallbackBody == null || fallbackBody.length() > 200 ? "HTTP " + fallbackBody : fallbackBody;
    }

    private static String sanitize(String text, String apiKey) {
        if (apiKey == null || apiKey.isBlank() || text == null) {
            return text;
        }
        String out = text.replace(apiKey, "***");
        return out.length() > 200 ? out.substring(0, 200) : out;
    }

    private String writeJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new BizException("AI 请求体序列化失败");
        }
    }

}
