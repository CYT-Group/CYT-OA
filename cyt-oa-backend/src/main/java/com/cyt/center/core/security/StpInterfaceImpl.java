package com.cyt.center.core.security;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 让 Sa-Token 的注解式校验（@SaCheckPermission 等）与 CurrentUser 使用同一份数据，
 * 权限只在 sys_role -> sys_menu 里维护，不再出现第二套来源。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final PrincipalRepository principalRepository;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Principal principal = principal(loginId);
        return principal == null ? List.of() : principal.permissions();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Principal principal = principal(loginId);
        return principal == null ? List.of() : principal.roles();
    }

    private Principal principal(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return principalRepository.load(Long.parseLong(loginId.toString()));
        } catch (NumberFormatException error) {
            return null;
        }
    }

}
