package com.cyt.center.core.security;

import java.util.List;

/**
 * 当前请求的登录身份。姓名与部门来自企微通讯录，角色与权限在本地维护。
 */
public record Principal(Long userId, String username, String name, List<String> roles, List<String> permissions) {

    public boolean has(String permission) {
        return permissions.contains("*:*:*") || permissions.contains(permission);
    }

    public boolean isAdmin() {
        return roles.contains("admin");
    }

    public boolean canAccessConsole() {
        return isAdmin() || has("can_enter_admin");
    }

}
