package com.cyt.center.core.security;

import cn.dev33.satoken.stp.StpUtil;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 统一解析「这次请求是谁」。
 * 优先 Sa-Token 令牌；只有本机 profile 显式打开 dev-identity 时才承认 X-User-Id 头，
 * 这条兼容通道是给尚未接登录页的个人中心留的，线上必须关闭。
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    public static final String DEVICE_FRONT = "front";
    public static final String DEVICE_ADMIN = "admin";

    private static final String CACHE_KEY = "cyt:current-principal";
    private static final String DEV_HEADER = "X-User-Id";

    private final CenterProperties properties;
    private final PrincipalRepository principalRepository;

    /** 未登录返回 null，不抛异常：概览一类的聚合接口允许匿名降级。 */
    public Principal resolve() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object cached = attributes.getAttribute(CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
        if (cached instanceof Principal principal) {
            return principal;
        }
        Principal loaded = loadFromToken();
        if (loaded == null) {
            loaded = loadFromDevHeader(attributes);
        }
        if (loaded != null) {
            attributes.setAttribute(CACHE_KEY, loaded, RequestAttributes.SCOPE_REQUEST);
        }
        return loaded;
    }

    public Principal require() {
        Principal principal = resolve();
        if (principal == null) {
            throw AuthException.unauthorized("请先登录");
        }
        return principal;
    }

    public Long requireUserId() {
        return require().userId();
    }

    public void requirePermission(String permission) {
        Principal principal = require();
        if (!principal.has(permission)) {
            throw AuthException.forbidden("没有该操作权限：" + permission);
        }
    }

    public void requireAdminConsole() {
        require();
        if (!DEVICE_ADMIN.equals(deviceType())) {
            throw AuthException.forbidden("请使用管理控制台登录后再操作");
        }
    }

    /** 当前令牌所属入口设备；未登录时为 null。 */
    public String deviceType() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginDeviceType() : null;
        } catch (IllegalStateException error) {
            return null;
        }
    }

    private Principal loadFromToken() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : principalRepository.load(Long.parseLong(loginId.toString()));
    }

    private Principal loadFromDevHeader(RequestAttributes attributes) {
        if (!properties.getAuth().isDevIdentityEnabled()) {
            return null;
        }
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        String header = servletAttributes.getRequest().getHeader(DEV_HEADER);
        if (header == null || !header.matches("\\d{1,19}")) {
            return null;
        }
        return principalRepository.load(Long.parseLong(header));
    }

}
