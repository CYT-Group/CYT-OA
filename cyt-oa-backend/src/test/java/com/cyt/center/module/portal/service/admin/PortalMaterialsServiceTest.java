package com.cyt.center.module.portal.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

class PortalMaterialsServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final PortalMaterialsService service =
            new PortalMaterialsService(jdbc, user, mock(PortalAudit.class), new ObjectMapper());

    private void setup(String status) {
        when(user.require())
                .thenReturn(new Principal(1L, "admin", "管理员", List.of("admin"), List.of("*:*:*")));
        when(jdbc.queryForList(contains("FROM portal_material_applies WHERE id"), eq(4L)))
                .thenReturn(
                        List.of(
                                new HashMap<>(
                                        Map.of(
                                                "id",
                                                4L,
                                                "status",
                                                status,
                                                "apply_type",
                                                "sample_borrow",
                                                "applicant_name",
                                                "同事"))));
        when(jdbc.queryForList(contains("FROM portal_material_apply_items WHERE apply_id"), eq(4L)))
                .thenReturn(
                        List.of(
                                new HashMap<>(
                                        Map.of(
                                                "id",
                                                5L,
                                                "material_id",
                                                6L,
                                                "quantity",
                                                3L,
                                                "approved_qty",
                                                2L,
                                                "returned_quantity",
                                                1L))));
        when(jdbc.queryForList(contains("SELECT id FROM portal_materials"), eq(6L)))
                .thenReturn(List.of(Map.of("id", 6L)));
    }

    @Test
    void approvalCannotIncreaseRequestedQuantity() {
        setup("pending");
        var error =
                assertThrows(
                        BizException.class,
                        () ->
                                service.action(
                                        4L,
                                        "approve",
                                        Map.of("items", List.of(Map.of("id", 5, "quantity", 4)))));
        assertTrue(error.getMessage().contains("审批数量"));
        verify(jdbc, never()).update(contains("stock_qty"), any(Object[].class));
    }

    @Test
    void returnCannotExceedApprovedBalance() {
        setup("borrowed");
        var error =
                assertThrows(
                        BizException.class,
                        () ->
                                service.action(
                                        4L,
                                        "return",
                                        Map.of("items", List.of(Map.of("id", 5, "quantity", 2)))));
        assertTrue(error.getMessage().contains("归还数量"));
    }

    @Test
    void handlerUsesCurrentSiteOwnerNotMaterialManagerCapability() {
        setup("pending");
        when(user.require())
                .thenReturn(
                        new Principal(
                                2L,
                                "manager",
                                "主管",
                                List.of("management"),
                                List.of("can_manage_materials")));
        when(jdbc.queryForObject(contains("owner_username"), eq(Long.class), eq(6L), eq("manager")))
                .thenReturn(0L);
        assertThrows(AuthException.class, () -> service.action(4L, "approve", Map.of()));
    }

    @Test
    void stateChangesAreNotReplayable() {
        setup("returned");
        assertThrows(BizException.class, () -> service.action(4L, "approve", Map.of()));
    }

    @Test
    void owningOneSiteCannotAuthorizeAnotherSiteInSameLegacyApply() {
        setup("pending");
        when(user.require())
                .thenReturn(
                        new Principal(
                                2L,
                                "manager",
                                "主管",
                                List.of("management"),
                                List.of("can_manage_materials")));
        when(jdbc.queryForList(contains("FROM portal_material_apply_items WHERE apply_id"), eq(4L)))
                .thenReturn(
                        List.of(
                                Map.of("id", 5L, "material_id", 6L),
                                Map.of("id", 7L, "material_id", 8L)));
        when(jdbc.queryForObject(contains("owner_username"), eq(Long.class), eq(6L), eq("manager")))
                .thenReturn(1L);
        when(jdbc.queryForObject(contains("owner_username"), eq(Long.class), eq(8L), eq("manager")))
                .thenReturn(0L);
        assertThrows(AuthException.class, () -> service.action(4L, "approve", Map.of()));
    }
}
