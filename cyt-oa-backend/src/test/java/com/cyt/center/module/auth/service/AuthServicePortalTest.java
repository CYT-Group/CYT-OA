package com.cyt.center.module.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.system.service.PrincipalService;
import com.cyt.center.module.system.service.SysUserService;

import org.junit.jupiter.api.Test;

import java.util.List;

class AuthServicePortalTest {
    @Test
    void autoLoginIssuesAdminDeviceForAuthorizedPortalRole() {
        var users = mock(SysUserService.class);
        var principals = mock(PrincipalService.class);
        when(principals.load(2L))
                .thenReturn(
                        new Principal(2L, "hr", "HR", List.of("hr"), List.of("can_enter_admin")));
        var service = new AuthService(new CenterProperties(), users, principals);
        try (var sessions = mockStatic(StpUtil.class)) {
            sessions.when(() -> StpUtil.createLoginSession(eq(2L), any(SaLoginParameter.class)))
                    .thenReturn("test-session");
            var login = service.issue(2L, "auto");
            assertEquals("admin", login.device());
            assertEquals("test-session", login.token());
            assertEquals(List.of("hr"), login.roles());
        }
    }

    @Test
    void staffCannotRequestAdminDeviceExplicitly() {
        var principals = mock(PrincipalService.class);
        when(principals.load(3L))
                .thenReturn(new Principal(3L, "staff", "员工", List.of("employee"), List.of()));
        var service =
                new AuthService(new CenterProperties(), mock(SysUserService.class), principals);
        assertThrows(AuthException.class, () -> service.issue(3L, "admin"));
    }
}
