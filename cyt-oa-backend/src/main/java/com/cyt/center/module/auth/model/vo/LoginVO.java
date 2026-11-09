package com.cyt.center.module.auth.model.vo;

import java.util.List;

/**
 * 登录结果。令牌同时用于 Cookie 与请求头，前端任选其一。
 */
public record LoginVO(
        String token,
        String device,
        Long userId,
        String name,
        List<String> roles,
        List<String> permissions
) {
}
