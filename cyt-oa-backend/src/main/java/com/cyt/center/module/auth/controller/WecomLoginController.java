package com.cyt.center.module.auth.controller;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.PortalSsoClient;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.auth.model.vo.LoginVO;
import com.cyt.center.module.auth.service.AuthService;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 企微授权统一回 Portal，本系统只接受绑定当前浏览器与 PKCE 的一次性员工码。
 * 账号必须先由通讯录同步存在才允许登录，姓名、邮箱、URL 里的 userid 都不能用来认领身份。
 */
@Slf4j
@RestController
@RequestMapping("/api/wecom")
@RequiredArgsConstructor
public class WecomLoginController {

    private static final String PENDING = "cyt.center.portal.pending";
    private static final String HANDOFF = "cyt.center.wecom.handoff";
    private static final String GENERATION = "cyt.center.portal.generation";
    private static final long PENDING_TTL_MILLIS = 300_000;
    private static final long HANDOFF_TTL_MILLIS = 60_000;
    private static final String FRONT_SCOPE = "front";
    private static final String ADMIN_SCOPE = "admin";
    /** 前台是嵌在企微里的个人中心，后台是控制台登录页，两个入口各自回跳。 */
    private static final String FRONT_PATH = "/personal";
    private static final String ADMIN_PATH = "/login";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PortalSsoClient portal;
    private final SysUserService users;
    private final AuthService authService;

    private record Pending(String nonce, String verifier, long issued, String returnBase, boolean popup,
                           boolean admin) {
    }

    private record Handoff(String code, Long userId, String userid, long issued, String nonce, boolean admin) {
    }

    public record ExchangeRequest(String code) {
    }

    @GetMapping("/available")
    public Result<Boolean> available() {
        return Result.ok(portal.available());
    }

    /** 登录页据此决定用哪个域名加载企微二维码，二维码回调必须与后端配置逐字一致。 */
    @GetMapping("/configuration")
    public Result<Map<String, Object>> configuration() {
        if (!portal.available()) {
            return Result.ok(Map.of("available", false, "publicBaseUrl", "", "portalBaseUrl", "",
                    "oauthCallbackUri", "", "statePrefix", "",
                    "message", "统一登录尚未就绪，请联系管理员在 Portal 登记本系统接入后重新扫码。"));
        }
        return Result.ok(Map.of("available", true, "publicBaseUrl", portal.returnBase(),
                "portalBaseUrl", portal.portalBase(), "oauthCallbackUri", portal.callbackUri(),
                "statePrefix", portalStatePrefix()));
    }

    /** 二维码面板参数：由 Portal 生成，前端只原样拼进官方登录组件，不参与任何身份判断。 */
    @GetMapping("/panel")
    public Result<Map<String, String>> panel(@RequestParam(value = "scope", defaultValue = "front") String scope,
                                             HttpSession session) {
        return Result.ok(begin(session, false, scope).params());
    }

    /** 企微内置浏览器走整页授权，桌面端走快捷登录面板或扫码。 */
    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam(value = "popup", defaultValue = "false") boolean popup,
                                      @RequestParam(value = "scope", defaultValue = "front") String scope,
                                      HttpServletRequest request, HttpSession session) {
        Map<String, String> params = new LinkedHashMap<>(begin(session, popup, scope).params());
        boolean insideWecom = isInsideWecom(request);
        String base = "https://login.work.weixin.qq.com/wwlogin/sso/login";
        if (insideWecom) {
            base = "https://open.weixin.qq.com/connect/oauth2/authorize";
            params.remove("login_type");
            params.remove("redirect_type");
            params.remove("panel_size");
            params.remove("lang");
            params.put("response_type", "code");
            params.put("scope", "snsapi_base");
        }
        String query = params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return redirect(base + "?" + query + (insideWecom ? "#wechat_redirect" : ""));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(value = "code", defaultValue = "") String code,
                                         @RequestParam(value = "state", defaultValue = "") String state,
                                         HttpServletRequest request, HttpSession session) {
        String loginPage = portal.returnBaseQuietly() + FRONT_PATH;
        try {
            Pending pending;
            synchronized (session) {
                Object value = session.getAttribute(PENDING);
                if (!(value instanceof Pending current) || !current.nonce().equals(state)
                        || System.currentTimeMillis() - current.issued() > PENDING_TTL_MILLIS
                        || !portal.matchesCodeFormat(code)) {
                    throw new BizException("统一登录校验失败或已过期，请重新扫码");
                }
                pending = current;
                session.removeAttribute(PENDING);
            }
            loginPage = pending.returnBase() + (pending.admin() ? ADMIN_PATH : FRONT_PATH);
            String userid = portal.exchange(code, pending.verifier(), pending.nonce());
            SysUser user = users.byWecomUserid(userid);
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                throw new BizException("该员工尚未同步或账号已停用，请先执行通讯录同步");
            }
            synchronized (session) {
                // 兑换期间二维码刷新过，旧的一次身份不能覆盖新事务。
                if (!pending.nonce().equals(session.getAttribute(GENERATION))) {
                    throw new BizException("二维码已刷新，请使用最新二维码");
                }
                request.changeSessionId();
                String handoff = UUID.randomUUID().toString().replace("-", "");
                session.setAttribute(HANDOFF, new Handoff(handoff, user.getId(), userid, System.currentTimeMillis(),
                        pending.nonce(), pending.admin()));
                return redirect(loginPage + "?" + (pending.popup() ? "wecom_popup=1&" : "") + "wecom_code=" + handoff);
            }
        } catch (BizException error) {
            // 失败原因只写日志，页面统一拿 wecom_error 提示重新扫码，不回显任何身份细节。
            log.warn("企微登录未完成：{}", error.getMessage());
            return redirect(loginPage + "?wecom_error=login_failed");
        }
    }

    /** 前端拿 wecom_code 换本系统令牌；这一步之后浏览器只持有自己的会话，不再认识 Portal。 */
    @PostMapping("/exchange")
    public Result<LoginVO> exchange(@RequestBody ExchangeRequest request, HttpSession session) {
        synchronized (session) {
            Object value = session.getAttribute(HANDOFF);
            if (!(value instanceof Handoff current) || !current.code().equals(request.code())
                    || !current.nonce().equals(session.getAttribute(GENERATION))
                    || System.currentTimeMillis() - current.issued() > HANDOFF_TTL_MILLIS) {
                throw new BizException("统一登录已过期，请重新扫码");
            }
            session.removeAttribute(HANDOFF);
            SysUser user = users.requireById(current.userId());
            if (!current.userid().equals(user.getWecomUserid())) {
                throw new BizException("该员工账号不可用，请联系管理员");
            }
            return Result.ok(authService.issue(user.getId(),
                    current.admin() ? ADMIN_SCOPE : FRONT_SCOPE));
        }
    }

    private PortalSsoClient.Authorization begin(HttpSession session, boolean popup, String scope) {
        if (!FRONT_SCOPE.equals(scope) && !ADMIN_SCOPE.equals(scope)) {
            throw new BizException("登录入口无效");
        }
        portal.requireEnabled();
        synchronized (session) {
            session.removeAttribute(PENDING);
            session.removeAttribute(HANDOFF);
            String nonce = UUID.randomUUID().toString().replace("-", "");
            session.setAttribute(GENERATION, nonce);
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            String challenge;
            try {
                challenge = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            } catch (NoSuchAlgorithmException error) {
                throw new IllegalStateException(error);
            }
            PortalSsoClient.Authorization authorization = portal.authorize(nonce, challenge);
            session.setAttribute(PENDING, new Pending(nonce, verifier, System.currentTimeMillis(),
                    authorization.returnBase(), popup, ADMIN_SCOPE.equals(scope)));
            return authorization;
        }
    }

    private String portalStatePrefix() {
        return portal.statePrefix();
    }

    private static boolean isInsideWecom(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && userAgent.toLowerCase(Locale.ROOT).contains("wxwork");
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(303).header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer").location(URI.create(url)).build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
