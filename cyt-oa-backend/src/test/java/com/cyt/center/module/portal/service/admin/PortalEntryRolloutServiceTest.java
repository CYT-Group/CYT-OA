package com.cyt.center.module.portal.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.portal.dal.postgresql.rollout.PortalEntryRolloutRepository;
import com.cyt.center.module.portal.service.support.PortalAudit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

class PortalEntryRolloutServiceTest {
    final PortalEntryRolloutRepository repository = mock(PortalEntryRolloutRepository.class);
    final CurrentUser users = mock(CurrentUser.class);
    final PortalAudit audit = mock(PortalAudit.class);
    final PortalEntryRolloutService service =
            new PortalEntryRolloutService(repository, users, audit);

    @BeforeEach
    void setup() {
        when(users.require())
                .thenReturn(new Principal(1L, "admin", "管理员", List.of("admin"), List.of()));
        when(repository.read(anyBoolean())).thenReturn(row(Map.of(), 0));
    }

    private PortalEntryRolloutRepository.Settings row(Map<String, Object> entries, long revision) {
        return new PortalEntryRolloutRepository.Settings(entries, revision, null, null);
    }

    @Test
    void existingMenusRemainEnabledWithoutConfiguration() {
        var result = service.entries();
        assertEquals(22, result.entries().size());
        assertTrue(result.entries().containsKey("portal-voice"));
        assertTrue(result.entries().containsKey("portal-workspace-rules"));
        assertTrue(result.entries().values().stream().allMatch(Boolean.TRUE::equals));
        verify(repository, never()).initialize();
    }

    @Test
    void managersReadButCannotChangeGlobalRolloutEvenWithDelegatedRoleCapability() {
        when(users.require())
                .thenReturn(
                        new Principal(
                                2L,
                                "hr",
                                "HR",
                                List.of("hr"),
                                List.of("can_enter_admin", "can_manage_roles")));
        assertEquals(22, service.entries().entries().size());
        assertThrows(AuthException.class, service::settings);
        assertThrows(AuthException.class, () -> service.save(0L, Map.of("portal-ai", false)));
        verify(repository, never()).initialize();
        verifyNoInteractions(audit);
    }

    @Test
    void employeesCannotReadManagementRollout() {
        when(users.require())
                .thenReturn(new Principal(3L, "staff", "员工", List.of("employee"), List.of()));
        assertThrows(AuthException.class, service::entries);
        verifyNoInteractions(repository);
    }

    @Test
    void partialSavePreservesOtherFeaturesAndAuditsOnlyChangedEntries() {
        when(repository.read(true)).thenReturn(row(Map.of("portal-news", false), 4));
        when(repository.read(false))
                .thenReturn(row(Map.of("portal-news", false, "portal-ai", false), 5));
        when(repository.update(anyMap(), eq(4L), eq("admin"))).thenReturn(true);
        var saved = service.save(4L, Map.of("portal-ai", false));
        assertFalse(saved.entries().get("portal-ai"));
        assertFalse(saved.entries().get("portal-news"));
        assertTrue(saved.entries().get("portal-hr-documents"));
        verify(repository)
                .update(
                        argThat(
                                map ->
                                        map.size() == 22
                                                && !map.get("portal-news")
                                                && !map.get("portal-ai")
                                                && map.get("portal-signoff")),
                        eq(4L),
                        eq("admin"));
        verify(audit)
                .record(
                        eq(1L),
                        eq("portal_entry_rollout"),
                        eq("portal_entry_rollout"),
                        eq(1),
                        contains("portal-ai"),
                        contains("false"),
                        anyString());
    }

    @Test
    void administratorCannotHideTheRecoveryEntrancesOrUnknownFeatures() {
        for (String key : List.of("portal-hub", "portal-rollout", "invented-menu"))
            assertThrows(BizException.class, () -> service.save(0L, Map.of(key, false)));
        verifyNoInteractions(repository, audit);
    }

    @Test
    void nonBooleanPayloadCannotAccidentallyEnableFeatures() {
        assertThrows(BizException.class, () -> service.save(0L, Map.of("portal-ai", "false")));
        assertThrows(
                BizException.class,
                () -> service.save(0L, Collections.singletonMap("portal-ai", null)));
        verifyNoInteractions(repository, audit);
    }

    @Test
    void staleRevisionCannotOverwriteAnotherAdministratorsChanges() {
        when(repository.read(true)).thenReturn(row(Map.of("portal-news", false), 2));
        assertThrows(BizException.class, () -> service.save(1L, Map.of("portal-ai", false)));
        verify(repository, never()).update(anyMap(), anyLong(), anyString());
        verifyNoInteractions(audit);
    }

    @Test
    void conditionalUpdateFailureIsReportedAndNotAudited() {
        when(repository.update(anyMap(), eq(0L), eq("admin"))).thenReturn(false);
        assertThrows(BizException.class, () -> service.save(0L, Map.of("portal-ai", false)));
        verifyNoInteractions(audit);
    }

    @Test
    void unchangedSaveDoesNotCreateAuditNoiseOrChangeRevision() {
        assertEquals(0, service.save(0L, Map.of("portal-ai", true)).revision());
        verify(repository, never()).update(anyMap(), anyLong(), anyString());
        verifyNoInteractions(audit);
    }
}
