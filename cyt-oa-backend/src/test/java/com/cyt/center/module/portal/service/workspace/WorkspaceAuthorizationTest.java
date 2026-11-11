package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.mapper.NotificationMapper;
import com.cyt.center.module.me.mapper.VoiceMapper;
import com.cyt.center.module.me.service.MeTeamService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class WorkspaceAuthorizationTest {
    @Test void hrDedicatedAccountsCannotUsePersonalWorkspace(){
        var current=mock(CurrentUser.class);
        when(current.require()).thenReturn(new Principal(7L,"hr","人事",List.of("hr","employee"),List.of()));
        var identity=new WorkspaceIdentity(current,mock(HrmEmployeeService.class),mock(MeTeamService.class),mock(JdbcTemplate.class),mock(NotificationMapper.class));
        assertThrows(AuthException.class,identity::personal);
        when(current.require()).thenReturn(new Principal(7L,"admin","管理员",List.of("admin"),List.of("*:*:*")));
        assertDoesNotThrow(identity::personal);
    }
    @Test void nonAdminCannotReadCeoMailboxEvenWithVoiceManagementPermission(){
        var identity=mock(WorkspaceIdentity.class);var jdbc=mock(JdbcTemplate.class);
        var service=new WorkspaceVoiceService(mock(PortalWorkspaceStore.class),identity,jdbc,mock(VoiceMapper.class));
        when(identity.admin()).thenReturn(false);
        assertThrows(AuthException.class,()->service.list("ceo",true));
        verify(identity).require("can_manage_voice");verifyNoInteractions(jdbc);
    }
    @Test void personalCardsAreBoundToLoginAndOnlySuccessfulPastDeliveries(){
        var identity=mock(WorkspaceIdentity.class);var jdbc=mock(JdbcTemplate.class);
        when(identity.principal()).thenReturn(new Principal(7L,"employee-7","本人",List.of("employee"),List.of()));
        when(jdbc.queryForList(anyString(),eq("employee-7"))).thenReturn(List.of(new HashMap<>(Map.of("id",1L,"token","private-token","title","生日快乐","recipient_name","本人","sent_at","2026-11-11 09:00:00"))));
        var cards=new WorkspacePersonalService(identity,jdbc).celebrations();
        assertEquals("1",cards.getFirst().get("id"));assertEquals(true,cards.getFirst().get("sentOk"));
        assertFalse(cards.getFirst().containsKey("token"));assertFalse(cards.getFirst().containsKey("recipientUsername"));
        verify(identity).personal();
        verify(jdbc).queryForList(contains("recipient_username=? AND sent_ok=1 AND sent_at IS NOT NULL AND sent_at<=CURRENT_TIMESTAMP"),eq("employee-7"));
    }
    @Test void employeeMaterialRecordsAlwaysUseOwnApplicantUsername(){
        var identity=mock(WorkspaceIdentity.class);var jdbc=mock(JdbcTemplate.class);
        when(identity.principal()).thenReturn(new Principal(7L,"employee-7","本人",List.of("employee"),List.of()));
        var service=new WorkspacePersonalService(identity,jdbc);
        assertTrue(service.materialRecords("sample",false).isEmpty());
        verify(jdbc).queryForList(contains("a.apply_type=? AND a.applicant_username=?"),eq("sample_borrow"),eq("employee-7"));
        verify(jdbc,never()).queryForList(contains("SELECT * FROM portal_material_applies"),any(Object[].class));
    }
    @Test void employeeCannotReadMaterialsApprovalRecordsWithoutSiteBinding(){
        var identity=mock(WorkspaceIdentity.class);var jdbc=mock(JdbcTemplate.class);
        when(identity.principal()).thenReturn(new Principal(7L,"employee-7","本人",List.of("employee"),List.of()));
        when(jdbc.queryForObject(anyString(),eq(Long.class),eq("employee-7"))).thenReturn(0L);
        assertThrows(AuthException.class,()->new WorkspacePersonalService(identity,jdbc).materialRecords("all",true));
        verify(jdbc,never()).queryForList(anyString(),any(Object[].class));
    }
}
