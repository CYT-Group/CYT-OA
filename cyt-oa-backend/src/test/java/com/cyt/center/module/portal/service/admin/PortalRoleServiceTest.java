package com.cyt.center.module.portal.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.portal.service.support.PortalAudit;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

class PortalRoleServiceTest {
    @Test
    void roleManagementCannotBeDelegatedThroughMatrix() {
        CurrentUser user = mock(CurrentUser.class);
        when(user.require())
                .thenReturn(
                        new Principal(
                                2L,
                                "hr",
                                "HR",
                                List.of("hr"),
                                List.of("can_manage_roles", "can_enter_admin")));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        PortalRoleService roles = new PortalRoleService(jdbc, user, mock(PortalAudit.class));
        assertThrows(AuthException.class, roles::list);
        verifyNoInteractions(jdbc);
    }

    @Test
    void portalManagerCanEnterConsoleWithoutSystemAdmin() {
        Principal hr = new Principal(2L, "hr", "HR", List.of("hr"), List.of("can_enter_admin"));
        assertTrue(hr.canAccessConsole());
        assertFalse(hr.isAdmin());
        assertFalse(
                new Principal(3L, "staff", "员工", List.of("employee"), List.of())
                        .canAccessConsole());
    }
}
