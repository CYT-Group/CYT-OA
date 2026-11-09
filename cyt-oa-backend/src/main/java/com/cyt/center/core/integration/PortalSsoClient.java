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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只向固定 Portal 后端兑换员工身份，绝不接受浏览器传来的 userid 或 Portal JWT。
 * Portal 返回的每一项都必须与本应用配置逐字一致，任一不符即整体拒绝，避免把 OA 用户带到别的系统去。
 */
@Component
@RequiredArgsConstructor
public class PortalSsoClient {

    /** Portal 侧企微授权的统一回调地址路径，所有接入系统共用。 */
    private static final String OAUTH_CALLBACK_PATH = "/api/wework/callback";
    private static final long CONFIG_CACHE_NANOS = Duration.ofSeconds(30).toNanos();
    private static final int MAX_RESPONSE_CHARS = 32_768;

    private final CenterProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private volatile PublicConfiguration publicConfiguration;

    public record Authorization(String returnBase, Map<String, String> params) {
    }

    /** 开关关闭时前端隐藏企微入口，而不是展示一个必然失败的按钮。 */
    public boolean enabled() {
        return properties.getSso().isEnabled();
    }

    public void requireEnabled() {
        if (!enabled()) {
            throw unavailable();
        }
    }

    /** 前端据此决定要不要显示企微入口：配置开着但 Portal 还没登记本应用时，也不能显示一个必失败的按钮。 */
    public boolean available() {
        if (!enabled()) {
            return false;
        }
        try {
            returnBase();
            return true;
        } catch (BizException error) {
            return false;
        }
    }

    /** 登录页只用它校验二维码回传的 state 归属本系统，不参与身份判定。 */
    public String statePrefix() {
        return properties.getSso().getStatePrefix();
    }

    /** 失败回跳用的基址：Portal 配置不可读时退回本应用配置值，不能因为取配置失败把人卡在错误页。 */
    public String returnBaseQuietly() {
        if (!enabled()) {
            return "";
        }
        try {
            return returnBase();
        } catch (BizException error) {
            try {
                return checkedBase(properties.getSso().getReturnBaseUrl());
            } catch (BizException ignored) {
                return "";
            }
        }
    }

    /** 浏览器可见的 Portal 基址，企微授权与身份签发都以它为准。 */
    public String portalBase() {
        String publicBase = properties.getSso().getPortalPublicBaseUrl();
        return checkedBase(publicBase.isBlank() ? properties.getSso().getPortalBaseUrl() : publicBase);
    }

    public String callbackUri() {
        return portalBase() + OAUTH_CALLBACK_PATH;
    }

    /** 登录完成后回跳的本系统前端基址，必须与 Portal 登记值一致。 */
    public synchronized String returnBase() {
        String apiBase = checkedBase(properties.getSso().getPortalBaseUrl());
        String callback = callbackUri();
        String configured = checkedBase(properties.getSso().getReturnBaseUrl());
        PublicConfiguration cached = publicConfiguration;
        if (cached != null && cached.apiBase().equals(apiBase) && cached.callback().equals(callback)
                && cached.configuredReturn().equals(configured) && System.nanoTime() < cached.expires()) {
            return cached.returnBase();
        }
        JsonNode config = request("/configuration", Map.of());
        if (!callback.equals(text(config, "oauth_callback_uri"))) {
            throw unavailable();
        }
        String returnBase = checkedBase(text(config, "return_base_url"));
        if (!configured.equals(returnBase)) {
            throw unavailable();
        }
        publicConfiguration = new PublicConfiguration(apiBase, callback, configured, returnBase,
                System.nanoTime() + CONFIG_CACHE_NANOS);
        return returnBase;
    }

    public Authorization authorize(String nonce, String challenge) {
        JsonNode data = request("/authorize", Map.of("nonce", nonce, "code_challenge", challenge,
                "code_challenge_method", "S256"));
        CenterProperties.Sso sso = properties.getSso();
        String state = text(data, "state");
        String agentId = text(data, "agentid");
        boolean stateMatched = state != null && state.startsWith(sso.getStatePrefix())
                && state.substring(sso.getStatePrefix().length()).matches("[A-Za-z0-9_-]{43}");
        if (!callbackUri().equals(text(data, "redirect_uri")) || !"CorpApp".equals(text(data, "login_type"))
                || !"top".equals(text(data, "redirect_type")) || !"small".equals(text(data, "panel_size"))
                || !"zh".equals(text(data, "lang")) || !stateMatched
                || !properties.getWecom().getCorpId().equals(text(data, "appid"))
                || agentId == null || agentId.isBlank() || !returnBase().equals(text(data, "return_base_url"))) {
            throw unavailable();
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (String key : List.of("login_type", "appid", "agentid", "redirect_uri", "state", "redirect_type",
                "panel_size", "lang")) {
            params.put(key, text(data, key));
        }
        return new Authorization(checkedBase(text(data, "return_base_url")), Map.copyOf(params));
    }

    public String exchange(String code, String verifier, String nonce) {
        JsonNode identity = request("/exchange", Map.of("code", code, "code_verifier", verifier));
        CenterProperties.Sso sso = properties.getSso();
        long now = Instant.now().getEpochSecond();
        long authenticated = identity.path("auth_time").asLong();
        long expires = identity.path("expires_at").asLong();
        String userid = text(identity, "wecom_userid");
        if (!portalBase().equals(text(identity, "issuer")) || !sso.getAudience().equals(text(identity, "audience"))
                || !properties.getWecom().getCorpId().equals(text(identity, "corp_id"))
                || !nonce.equals(text(identity, "nonce")) || userid == null || userid.isBlank() || userid.length() > 255
                || authenticated > now + 30 || authenticated < now - 330 || expires <= now
                || expires > authenticated + 300) {
            throw unavailable();
        }
        return userid;
    }

    /** 一次性登录码格式由本应用前缀决定，Portal 未为本应用登记时格式必然不符。 */
    public boolean matchesCodeFormat(String code) {
        String prefix = properties.getSso().getCodePrefix();
        return code != null && code.startsWith(prefix)
                && code.substring(prefix.length()).matches("[A-Za-z0-9_-]{43}");
    }

    private JsonNode request(String path, Map<String, Object> body) {
        requireEnabled();
        String url = checkedBase(properties.getSso().getPortalBaseUrl()) + properties.getSso().getEndpointPrefix() + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");
        if (body.isEmpty()) {
            builder.GET();
        } else {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(writeJson(body), StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 || response.body().length() > MAX_RESPONSE_CHARS) {
                throw unavailable();
            }
            JsonNode envelope = objectMapper.readTree(response.body());
            if (!envelope.path("success").asBoolean(false) || !envelope.path("data").isObject()) {
                throw unavailable();
            }
            return envelope.path("data");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException error) {
            throw unavailable();
        }
    }

    private String writeJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException error) {
            throw unavailable();
        }
    }

    /** 公开基址只接受 HTTPS，HTTP 仅限本机联调；回跳地址一律不接受请求头推导。 */
    public static String checkedBase(String value) {
        try {
            if (value == null || value.isBlank()) {
                throw unavailable();
            }
            String base = trimEndSlashes(value);
            URI uri = URI.create(base);
            boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost());
            if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || !uri.getRawPath().isEmpty()
                    || !("https".equals(uri.getScheme()) || local && "http".equals(uri.getScheme()))) {
                throw unavailable();
            }
            return base;
        } catch (IllegalArgumentException error) {
            throw unavailable();
        }
    }

    private static String trimEndSlashes(String value) {
        String result = value.trim();
        while (result.length() > 1 && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isValueNode() ? value.asText() : null;
    }

    public static BizException unavailable() {
        return new BizException("统一登录暂不可用，请联系管理员确认本系统已在 Portal 登记接入");
    }

    private record PublicConfiguration(String apiBase, String callback, String configuredReturn, String returnBase,
                                       long expires) {
    }

}
