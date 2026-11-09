package com.cyt.center.module.auth.model.vo;

import java.util.List;

/**
 * 当前登录身份，供前端渲染菜单与判断能否进入后台。
 */
public record MeVO(
        Long userId,
        String username,
        String name,
        List<String> roles,
        List<String> permissions,
        String device
) {
}
