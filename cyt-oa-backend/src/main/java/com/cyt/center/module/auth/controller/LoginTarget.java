package com.cyt.center.module.auth.controller;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** 登录后只回到 OA 站内页面，地址栏不能改变认证或重定向的目的站。 */
final class LoginTarget {
    private LoginTarget() {
    }

    static String safe(String value) {
        if (value == null || value.length() > 2048 || !value.startsWith("/") || value.startsWith("//")
                || value.contains("\\") || value.contains("#") || value.chars().anyMatch(c -> c <= 32 || c == 127)) {
            return "/";
        }
        try {
            URI uri = URI.create(value);
            String path = uri.getRawPath();
            // OA 路由使用正常路径；所有筛选参数保留在 query，路径不需要嵌套编码。
            if (uri.isAbsolute() || uri.getRawAuthority() != null || path.contains("%")) return "/";
            String normalized = path.toLowerCase(Locale.ROOT);
            if (normalized.matches("^/(?:login|api|admin-api)(?:/.*)?$")
                    || normalized.matches(".*(?:^|/)\\.{1,2}(?:/|$).*")) return "/";
            String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
            if (decoded.contains("\\") || decoded.chars().anyMatch(c -> c < 32 || c == 127)) return "/";
            return value;
        } catch (IllegalArgumentException error) {
            return "/";
        }
    }
}
