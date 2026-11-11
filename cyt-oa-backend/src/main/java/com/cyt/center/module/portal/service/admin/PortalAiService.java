package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.GeminiClient;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.portal.domain.hrdoc.HrDocumentBlockValidator;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalHtmlSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortalAiService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final CurrentUser currentUser;
    private final PortalRoleService roles;
    private final PortalSecretBox secrets;
    private final GeminiClient gemini;
    private final PortalAudit audit;
    private final CenterProperties properties;
    private List<Map<String, Object>> defaults = List.of();
    private JsonNode layoutSchema;
    private final Map<Long, Deque<Instant>> trials = new HashMap<>();

    @PostConstruct
    void loadDefaults() throws Exception {
        try (var in = new ClassPathResource("portal/ai-tasks.json").getInputStream()) {
            defaults = json.readValue(in, new TypeReference<>() {});
        }
        try (var in = new ClassPathResource("portal/hr-layout-schema.json").getInputStream()) {
            layoutSchema = json.readTree(in);
        }
    }

    public Map<String, Object> config() {
        currentUser.requirePermission("can_manage_ai_settings");
        var models =
                PortalRows.camel(
                        jdbc.queryForList("SELECT * FROM portal_ai_models ORDER BY sort_order,id"));
        var providers =
                PortalRows.camel(
                        jdbc.queryForList(
                                "SELECT"
                                    + " provider,is_default,tier_fast,tier_standard,tier_quality,api_key_hint,api_key_updated_at"
                                    + " FROM portal_ai_provider_settings WHERE provider='gemini'"));
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (var base : defaults) {
            var task = new LinkedHashMap<>(base);
            String name = String.valueOf(base.get("name"));
            var overrides =
                    jdbc.queryForList(
                            "SELECT * FROM portal_ai_task_settings WHERE task_name=?", name);
            task.put(
                    "override",
                    overrides.isEmpty() ? Map.of() : PortalRows.camel(overrides.getFirst()));
            tasks.add(task);
        }
        return Map.of("models", models, "providers", providers, "tasks", tasks);
    }

    public Map<String, Object> syncModels() {
        currentUser.requirePermission("can_manage_ai_settings");
        String key = managedKey();
        if (key == null || key.isBlank()) key = properties.getAi().getGeminiKey();
        if (key == null || key.isBlank()) key = System.getenv().getOrDefault("GEMINI_API_KEY", "");
        if (key.isBlank()) throw new BizException("Gemini API key 未配置");
        List<Map<String, String>> remote = new ArrayList<>();
        String next = "";
        var client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        try {
            for (int page = 0; page < 10; page++) {
                String url =
                        "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000"
                                + (next.isBlank()
                                        ? ""
                                        : "&pageToken="
                                                + java.net.URLEncoder.encode(
                                                        next,
                                                        java.nio.charset.StandardCharsets.UTF_8));
                var req =
                        HttpRequest.newBuilder(URI.create(url))
                                .timeout(Duration.ofSeconds(20))
                                .header("x-goog-api-key", key)
                                .GET()
                                .build();
                var response = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200)
                    throw new BizException("获取 Gemini 模型失败（HTTP " + response.statusCode() + "）");
                JsonNode payload = json.readTree(response.body());
                for (JsonNode model : payload.path("models")) {
                    String id = model.path("name").asText("").replaceFirst("^models/", "");
                    boolean textModel = false;
                    for (JsonNode method : model.path("supportedGenerationMethods"))
                        if ("generateContent".equals(method.asText())) textModel = true;
                    if (!textModel
                            || !id.matches("gemini-[a-zA-Z0-9._-]{1,57}")
                            || List.of(
                                            "tts",
                                            "image",
                                            "live",
                                            "embedding",
                                            "audio",
                                            "transcribe",
                                            "robotics",
                                            "computer")
                                    .stream()
                                    .anyMatch(id::contains)) continue;
                    remote.add(
                            Map.of(
                                    "modelId",
                                    id,
                                    "displayName",
                                    model.path("displayName").asText(id)));
                }
                next = payload.path("nextPageToken").asText("");
                if (next.isBlank()) break;
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BizException("模型获取已中断");
        } catch (java.io.IOException error) {
            throw new BizException("无法连接 Gemini，请稍后重试");
        }
        int added = 0;
        for (var model : remote) {
            String label = model.get("displayName");
            if (label.length() > 100) label = label.substring(0, 100);
            added +=
                    jdbc.update(
                            "INSERT INTO"
                                + " portal_ai_models(provider,model_id,display_name,enabled,sort_order,created_by,updated_by)"
                                + " SELECT 'gemini',?,?,1,0,?,? WHERE NOT EXISTS(SELECT 1 FROM"
                                + " portal_ai_models WHERE provider='gemini' AND model_id=?)",
                            model.get("modelId"),
                            label,
                            currentUser.require().username(),
                            currentUser.require().username(),
                            model.get("modelId"));
        }
        return Map.of("available", remote.size(), "added", added);
    }

    private Map<String, Object> task(String name) {
        return defaults.stream()
                .filter(x -> name.equals(x.get("name")))
                .findFirst()
                .orElseThrow(() -> new BizException("AI 任务不存在"));
    }

    private void checkModel(String model) {
        if (model != null
                && !model.isBlank()
                && jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_ai_models WHERE provider='gemini' AND"
                                    + " model_id=? AND enabled=1",
                                Long.class,
                                model)
                        == 0) throw new BizException("请选择目录中已启用的 Gemini 模型");
    }

    @Transactional
    public void saveTask(Map<String, Object> body) {
        currentUser.requirePermission("can_manage_ai_settings");
        String name = PortalRows.text(body, "taskName", "");
        task(name);
        // 配置行在首次写入时也必须占锁，避免两个管理员同时生成相同版本号。
        jdbc.update(
                "INSERT IGNORE INTO portal_ai_task_settings(task_name,updated_by) VALUES(?,?)",
                name,
                currentUser.require().username());
        jdbc.queryForList(
                "SELECT task_name FROM portal_ai_task_settings WHERE task_name=? FOR UPDATE", name);
        String model = blank(body.get("model"));
        checkModel(model);
        String effort = blank(body.get("effort"));
        if (effort != null && !Set.of("low", "medium", "high").contains(effort))
            throw new BizException("思考档位无效");
        String system = blank(body.get("systemPrompt")), template = blank(body.get("userTemplate"));
        if (system != null && system.length() > 100000
                || template != null && template.length() > 100000) throw new BizException("提示词过长");
        jdbc.update(
                "UPDATE portal_ai_task_settings SET"
                    + " provider='gemini',model=?,effort=?,system_prompt=?,user_template=?,updated_by=?,updated_at=NOW()"
                    + " WHERE task_name=?",
                model,
                effort,
                system,
                template,
                currentUser.require().username(),
                name);
        Long version =
                jdbc.queryForObject(
                        "SELECT COALESCE(MAX(version_no),0)+1 FROM portal_ai_task_setting_versions"
                            + " WHERE task_name=?",
                        Long.class,
                        name);
        jdbc.update(
                "INSERT INTO"
                    + " portal_ai_task_setting_versions(task_name,version_no,provider,model,effort,system_prompt,user_template,note,created_by)"
                    + " VALUES(?,?,'gemini',?,?,?,?,?,?)",
                name,
                version,
                model,
                effort,
                system,
                template,
                PortalRows.text(body, "note", ""),
                currentUser.require().username());
        audit.record(
                currentUser.requireUserId(),
                "ai_task_save",
                "portal_ai_task_settings",
                name,
                null,
                "v" + version,
                null);
    }

    public List<Map<String, Object>> versions(String name) {
        currentUser.requirePermission("can_manage_ai_settings");
        task(name);
        return PortalRows.camel(
                jdbc.queryForList(
                        "SELECT * FROM portal_ai_task_setting_versions WHERE task_name=? ORDER BY"
                            + " version_no DESC",
                        name));
    }

    @Transactional
    public void restore(String name, long version) {
        currentUser.requirePermission("can_manage_ai_settings");
        if (version == 0) {
            saveTask(new LinkedHashMap<>(Map.of("taskName", name, "note", "恢复代码默认")));
            return;
        }
        var rows =
                jdbc.queryForList(
                        "SELECT * FROM portal_ai_task_setting_versions WHERE task_name=? AND"
                            + " version_no=?",
                        name,
                        version);
        if (rows.isEmpty()) throw new BizException("配置版本不存在");
        var body = PortalRows.camel(rows.getFirst());
        body.put("taskName", name);
        body.put("note", "恢复到 v" + version);
        saveTask(body);
    }

    @Transactional
    public void saveModel(Map<String, Object> body) {
        currentUser.requirePermission("can_manage_ai_settings");
        String model = PortalRows.text(body, "modelId", "").trim(),
                label = PortalRows.text(body, "displayName", model).trim();
        if (!model.matches("gemini-[a-zA-Z0-9._-]{1,90}")
                || label.isBlank()
                || label.length() > 100) throw new BizException("Gemini 模型标识或名称无效");
        Long id = body.get("id") == null ? null : PortalRows.number(body.get("id"));
        boolean enabled = !Boolean.FALSE.equals(body.get("enabled"));
        BigDecimal input = price(body.get("inputPrice")), output = price(body.get("outputPrice"));
        if (id == null)
            jdbc.update(
                    "INSERT INTO"
                        + " portal_ai_models(provider,model_id,display_name,enabled,input_price,output_price,note,sort_order,created_by,updated_by)"
                        + " VALUES('gemini',?,?,?,?,?,?,0,?,?)",
                    model,
                    label,
                    enabled,
                    input,
                    output,
                    PortalRows.text(body, "note", ""),
                    currentUser.require().username(),
                    currentUser.require().username());
        else {
            var rows =
                    jdbc.queryForList("SELECT * FROM portal_ai_models WHERE id=? FOR UPDATE", id);
            if (rows.isEmpty()) throw new BizException("模型不存在");
            String old = String.valueOf(rows.getFirst().get("model_id"));
            if (!enabled || !old.equals(model)) requireUnreferenced(old);
            jdbc.update(
                    "UPDATE portal_ai_models SET"
                        + " model_id=?,display_name=?,enabled=?,input_price=?,output_price=?,note=?,updated_by=?,updated_at=NOW()"
                        + " WHERE id=? AND provider='gemini'",
                    model,
                    label,
                    enabled,
                    input,
                    output,
                    PortalRows.text(body, "note", ""),
                    currentUser.require().username(),
                    id);
        }
    }

    private BigDecimal price(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            BigDecimal price = new BigDecimal(String.valueOf(value));
            if (price.signum() < 0) throw new NumberFormatException();
            return price;
        } catch (Exception e) {
            throw new BizException("价格须为非负数字（美元/百万 token）");
        }
    }

    private void requireUnreferenced(String model) {
        long count =
                jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_ai_task_settings WHERE model=?",
                                Long.class,
                                model)
                        + jdbc.queryForObject(
                                "SELECT COUNT(*) FROM portal_ai_provider_settings WHERE tier_fast=?"
                                    + " OR tier_standard=? OR tier_quality=?",
                                Long.class,
                                model,
                                model,
                                model);
        if (count > 0) throw new BizException("模型仍被功能配置或档位引用，请先解除引用");
    }

    @Transactional
    public void deleteModel(long id) {
        currentUser.requirePermission("can_manage_ai_settings");
        var rows =
                jdbc.queryForList(
                        "SELECT model_id FROM portal_ai_models WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new BizException("模型不存在");
        requireUnreferenced(String.valueOf(rows.getFirst().get("model_id")));
        jdbc.update("DELETE FROM portal_ai_models WHERE id=?", id);
    }

    @Transactional
    public void saveProvider(Map<String, Object> body) {
        currentUser.requirePermission("can_manage_ai_settings");
        for (String key : List.of("tierFast", "tierStandard", "tierQuality"))
            checkModel(blank(body.get(key)));
        jdbc.update(
                "INSERT INTO"
                    + " portal_ai_provider_settings(provider,is_default,tier_fast,tier_standard,tier_quality,updated_by)"
                    + " VALUES('gemini',1,?,?,?,?) ON DUPLICATE KEY UPDATE"
                    + " tier_fast=VALUES(tier_fast),tier_standard=VALUES(tier_standard),tier_quality=VALUES(tier_quality),updated_by=VALUES(updated_by),updated_at=NOW()",
                blank(body.get("tierFast")),
                blank(body.get("tierStandard")),
                blank(body.get("tierQuality")),
                currentUser.require().username());
    }

    @Transactional
    public void saveKey(String key) {
        roles.requireAdmin();
        if (key == null
                || key.isBlank()
                || key.length() > 500
                || key.chars().anyMatch(Character::isWhitespace)) throw new BizException("请输入有效密钥");
        String encrypted = secrets.encrypt(key.trim());
        jdbc.update(
                "INSERT INTO"
                    + " portal_ai_provider_settings(provider,is_default,api_key_ciphertext,api_key_hint,api_key_updated_by,api_key_updated_at)"
                    + " VALUES('gemini',1,?,?,?,NOW()) ON DUPLICATE KEY UPDATE"
                    + " api_key_ciphertext=VALUES(api_key_ciphertext),api_key_hint=VALUES(api_key_hint),api_key_updated_by=VALUES(api_key_updated_by),api_key_updated_at=NOW()",
                encrypted,
                "…" + key.substring(Math.max(0, key.length() - 4)),
                currentUser.require().username());
        audit.record(
                currentUser.requireUserId(),
                "ai_key_set",
                "portal_ai_provider_settings",
                "gemini",
                null,
                "已设置",
                null);
    }

    public void clearKey() {
        roles.requireAdmin();
        jdbc.update(
                "UPDATE portal_ai_provider_settings SET"
                    + " api_key_ciphertext=NULL,api_key_hint=NULL,api_key_updated_at=NOW() WHERE"
                    + " provider='gemini'");
    }

    private String managedKey() {
        var rows =
                jdbc.queryForList(
                        "SELECT api_key_ciphertext FROM portal_ai_provider_settings WHERE"
                            + " provider='gemini'");
        if (rows.isEmpty() || rows.getFirst().get("api_key_ciphertext") == null) return null;
        try {
            return secrets.decrypt(String.valueOf(rows.getFirst().get("api_key_ciphertext")));
        } catch (BizException e) {
            return null;
        }
    }

    public void requireConfiguration() {
        if (!gemini.configured(managedKey())) throw new BizException("AI 服务未配置，请先在 AI 管理中配置密钥");
    }

    public String generate(
            String name,
            String fallbackModel,
            String fallbackSystem,
            String text,
            JsonNode schema) {
        return generateDocument(name, fallbackModel, fallbackSystem, text, null, null, schema);
    }

    public String generateDocument(
            String name,
            String fallbackModel,
            String fallbackSystem,
            String text,
            String mime,
            byte[] content,
            JsonNode schema) {
        return generateDocumentForActor(name, fallbackModel, fallbackSystem, text, mime, content,
                schema, currentUser.require().username());
    }

    /** 后台排版任务保留提交人审计，不依赖已结束的 HTTP 请求上下文。 */
    public String generateForActor(String name, String fallbackModel, String fallbackSystem,
                                   String text, JsonNode schema, String actorUsername) {
        return generateDocumentForActor(name, fallbackModel, fallbackSystem, text, null, null,
                schema, actorUsername);
    }

    public String generateDocumentForActor(String name, String fallbackModel, String fallbackSystem,
                                           String text, String mime, byte[] content, JsonNode schema,
                                           String actorUsername) {
        if (actorUsername == null || actorUsername.isBlank()) throw new BizException("缺少任务提交人");
        var meta = task(name);
        var rows =
                jdbc.queryForList("SELECT * FROM portal_ai_task_settings WHERE task_name=?", name);
        Map<String, Object> settings = rows.isEmpty() ? Map.of() : rows.getFirst();
        String model = blank(settings.get("model"));
        if (model == null || model.startsWith("claude-")) {
            String tier = PortalRows.text(meta, "tier", "standard");
            var providers =
                    jdbc.queryForList(
                            "SELECT tier_fast,tier_standard,tier_quality FROM"
                                + " portal_ai_provider_settings WHERE provider='gemini'");
            model =
                    providers.isEmpty()
                            ? fallbackModel
                            : PortalRows.text(providers.getFirst(), "tier_" + tier, fallbackModel);
        }
        checkModel(model);
        String system = PortalRows.text(settings, "system_prompt", fallbackSystem);
        String effort = PortalRows.text(settings, "effort", PortalRows.text(meta, "effort", "low"));
        String template = blank(settings.get("user_template"));
        if (template != null)
            text =
                    template.replace(
                                            "{today}",
                                            LocalDate.now(ZoneId.of("Asia/Shanghai")).toString())
                                    .replace("{text}", text)
                            + (template.contains("{text}") ? "" : "\n\n" + text);
        long start = System.nanoTime();
        String status = "error";
        GeminiClient.Generated result = null;
        try {
            int maxTokens = ((Number) meta.getOrDefault("maxTokens", 4096)).intValue();
            result =
                    content == null
                            ? gemini.generateDetailed(
                                    model, system, text, maxTokens, effort, schema, managedKey())
                            : gemini.generateDocumentDetailed(
                                    model,
                                    system,
                                    text,
                                    mime,
                                    content,
                                    maxTokens,
                                    schema,
                                    managedKey());
            status = "ok";
            return result.text();
        } finally {
            try {
                // 调用记录不保存提示词或原件，不把 API key 和候选人信息带入成本日志。
                Long in = result == null ? null : result.tokensIn(),
                        out = result == null ? null : result.tokensOut();
                BigDecimal cost = null;
                var prices =
                        jdbc.queryForList(
                                "SELECT input_price,output_price FROM portal_ai_models WHERE"
                                    + " provider='gemini' AND model_id=?",
                                model);
                if (in != null
                        && out != null
                        && !prices.isEmpty()
                        && prices.getFirst().get("input_price") != null
                        && prices.getFirst().get("output_price") != null)
                    cost =
                            new BigDecimal(prices.getFirst().get("input_price").toString())
                                    .multiply(BigDecimal.valueOf(in))
                                    .add(
                                            new BigDecimal(
                                                            prices.getFirst()
                                                                    .get("output_price")
                                                                    .toString())
                                                    .multiply(BigDecimal.valueOf(out)))
                                    .divide(BigDecimal.valueOf(1000000), 8, RoundingMode.HALF_UP);
                jdbc.update(
                        "INSERT INTO"
                            + " portal_api_call_log(provider,model,endpoint,tokens_in,tokens_out,cost_usd,latency_ms,status,called_by,called_at)"
                            + " VALUES('gemini',?,?,?,?,?,?,?,?,NOW())",
                        model,
                        name,
                        in,
                        out,
                        cost,
                        (System.nanoTime() - start) / 1000000,
                        status,
                        actorUsername.length() > 50 ? actorUsername.substring(0, 50) : actorUsername);
            } catch (org.springframework.dao.DataAccessException error) {
                log.warn("AI 成本记录写入失败，task={}", name);
            }
        }
    }

    public String polish(String text, String kind) {
        var principal = currentUser.require();
        if (!principal.has("can_create_content") && !principal.has("can_edit_hr_documents"))
            throw com.cyt.center.core.exception.AuthException.forbidden("没有内容编辑权限");
        checkText(text);
        limitTrial(60);
        var meta = task("ai.polish");
        String input =
                PortalRows.text(meta, "userTemplate", "{text}")
                        .replace("{kind}", kind == null ? "文档" : kind)
                        .replace("{text}", text);
        return generate(
                "ai.polish",
                "gemini-3.5-flash-lite",
                PortalRows.text(meta, "systemPrompt", ""),
                input,
                null);
    }

    public Map<String, Object> layout(String text) {
        currentUser.requirePermission("can_edit_hr_documents");
        checkText(text);
        limitTrial(20);
        var meta = task("hr.layout");
        String feedback = "";
        for (int attempt = 0; attempt < 2; attempt++) {
            String input =
                    PortalRows.text(meta, "userTemplate", "{text}")
                            .replace("{feedback}", feedback)
                            .replace("{text}", text);
            String output =
                    generate(
                            "hr.layout",
                            "gemini-3.8-flash",
                            PortalRows.text(meta, "systemPrompt", ""),
                            input,
                            layoutSchema);
            try {
                Map<String, Object> result = json.readValue(output, new TypeReference<>() {});
                Object raw = result.get("blocks");
                if (!(raw instanceof List<?> list)) throw new BizException("AI 未返回文档块");
                List<Object> blocks = (List<Object>) withoutNulls(list);
                HrDocumentBlockValidator.validate(blocks);
                result.put(
                        "blocks",
                        HrDocumentBlockValidator.sanitize(blocks, PortalHtmlSanitizer::sanitize));
                return result;
            } catch (Exception error) {
                feedback = "生成的块未通过结构校验，请根据以下问题修正，不删除原文：" + error.getMessage();
            }
        }
        throw new BizException("AI 排版未通过文档结构校验，请重试或手动编辑");
    }

    private Object withoutNulls(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach(
                    (k, v) -> {
                        if (v != null) out.put(k.toString(), withoutNulls(v));
                    });
            return out;
        }
        if (value instanceof List<?> list)
            return list.stream().filter(Objects::nonNull).map(this::withoutNulls).toList();
        return value;
    }

    private void checkText(String input) {
        if (input == null || input.isBlank() || input.length() > 100000)
            throw new BizException("请输入1至100000字文本");
    }

    private void limitTrial(int max) {
        synchronized (trials) {
            var times =
                    trials.computeIfAbsent(currentUser.requireUserId(), k -> new ArrayDeque<>());
            Instant limit = Instant.now().minusSeconds(3600);
            while (!times.isEmpty() && times.peekFirst().isBefore(limit)) times.removeFirst();
            if (times.size() >= max) throw new BizException("AI 操作过于频繁，请稍后重试");
            times.addLast(Instant.now());
        }
    }

    public String test(Map<String, Object> body) {
        currentUser.requirePermission("can_manage_ai_settings");
        synchronized (trials) {
            var times =
                    trials.computeIfAbsent(currentUser.requireUserId(), k -> new ArrayDeque<>());
            Instant limit = Instant.now().minusSeconds(3600);
            while (!times.isEmpty() && times.peekFirst().isBefore(limit)) times.removeFirst();
            if (times.size() >= 20) throw new BizException("每小时最多试运行20次");
            times.addLast(Instant.now());
        }
        String name = PortalRows.text(body, "taskName", "ai.polish");
        var meta = task(name);
        String input = PortalRows.text(body, "input", "");
        if (input.isBlank() || input.length() > 100000) throw new BizException("请输入1至100000字试运行内容");
        return generate(
                name,
                PortalRows.text(
                        body,
                        "model",
                        "fast".equals(meta.get("tier"))
                                ? "gemini-3.5-flash-lite"
                                : "gemini-3.8-flash"),
                PortalRows.text(meta, "systemPrompt", ""),
                input,
                null);
    }

    public Map<String, Object> costs(String month) {
        currentUser.requirePermission("can_view_api_costs");
        YearMonth period;
        try {
            period = YearMonth.parse(month);
        } catch (Exception e) {
            throw new BizException("月份格式应为 YYYY-MM");
        }
        var from = period.atDay(1);
        var to = period.plusMonths(1).atDay(1);
        return Map.of(
                "summary",
                PortalRows.camel(
                        jdbc.queryForMap(
                                "SELECT COUNT(*) AS calls,SUM(cost_usd) AS cost_usd,SUM(tokens_in)"
                                    + " AS tokens_in,SUM(tokens_out) AS"
                                    + " tokens_out,SUM(status<>'ok') AS errors FROM"
                                    + " portal_api_call_log WHERE called_at>=? AND called_at<?",
                                from,
                                to)),
                "endpoints",
                PortalRows.camel(
                        jdbc.queryForList(
                                "SELECT endpoint,provider,model,COUNT(*) AS calls,SUM(cost_usd) AS"
                                    + " cost_usd,SUM(tokens_in) AS tokens_in,SUM(tokens_out) AS"
                                    + " tokens_out FROM portal_api_call_log WHERE called_at>=? AND"
                                    + " called_at<? GROUP BY endpoint,provider,model ORDER BY calls"
                                    + " DESC",
                                from,
                                to)),
                "daily",
                PortalRows.camel(
                        jdbc.queryForList(
                                "SELECT DATE(called_at) AS date,COUNT(*) AS calls,SUM(cost_usd) AS"
                                    + " cost_usd FROM portal_api_call_log WHERE called_at>=? AND"
                                    + " called_at<? GROUP BY DATE(called_at) ORDER BY date",
                                from,
                                to)));
    }

    private static String blank(Object v) {
        return v == null || v.toString().isBlank() ? null : v.toString().trim();
    }
}
