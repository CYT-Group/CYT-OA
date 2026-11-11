package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.Principal;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class WorkspaceReviewReportTest {
    final PortalWorkspaceStore store=mock(PortalWorkspaceStore.class);
    final WorkspaceIdentity identity=mock(WorkspaceIdentity.class);
    final WorkspaceReviewReportService service=new WorkspaceReviewReportService(store,identity);
    PortalWorkspaceStore.Row report(String status){return new PortalWorkspaceStore.Row(44,"review-report",null,7,null,null,"年终述职",status,Map.of("period","2026年终","summary","已完成年度工作","plan","下一年计划","problem","","comments",List.of()),2,"2026-11-11","2026-11-11");}
    @Test void privateDraftCannotBeReadByManagerOrUnrelatedEmployee(){
        when(identity.user()).thenReturn(2L);when(identity.manages(7L)).thenReturn(true);
        when(store.get("review-report",44,false)).thenReturn(report("draft"));
        assertThrows(AuthException.class,()->service.get(44));verify(store,never()).view(any());
    }
    @Test void onlyOwnerCanEditDraftAndSubmittedReportsCannotBeEdited(){
        when(identity.user()).thenReturn(2L);when(store.get("review-report",44,true)).thenReturn(report("draft"));
        assertThrows(AuthException.class,()->service.save(44L,Map.of()));
        when(identity.user()).thenReturn(7L);when(store.get("review-report",44,true)).thenReturn(report("submitted"));
        assertThrows(AuthException.class,()->service.save(44L,Map.of()));
        verify(store,never()).update(any(),anyInt(),anyString(),anyString(),anyMap(),any());
    }
    @Test void reportOwnerCannotCommentOnOwnReport(){
        when(identity.user()).thenReturn(7L);when(store.get("review-report",44,true)).thenReturn(report("submitted"));
        assertThrows(AuthException.class,()->service.comment(44,Map.of("reviewText","自评")));
        verify(store,never()).update(any(),anyInt(),anyString(),anyString(),anyMap(),any());
    }
    @Test void directManagerCanCommentAndNotifiesReportOwner(){
        var row=report("submitted");when(identity.user()).thenReturn(2L);when(identity.manages(7L)).thenReturn(true);
        when(identity.principal()).thenReturn(new Principal(2L,"manager","直属主管",List.of("manager"),List.of()));
        when(store.get("review-report",44,true)).thenReturn(row);when(store.get("review-report",44,false)).thenReturn(row);when(store.view(row)).thenReturn(new HashMap<>());
        var result=service.comment(44,Map.of("reviewText","重点工作已完成","grade","良好"));
        assertEquals(true,result.get("canComment"));assertEquals(false,result.get("editable"));
        verify(store).update(eq(row),eq(2),eq("年终述职"),eq("submitted"),argThat(data->((List<?>)data.get("comments")).size()==1),isNull());
        verify(identity).notify(eq(7L),eq("review-comment"),eq(44L),contains("年终述职"),eq("/me/review-reports?id=44"));
    }
    @Test void submissionRequiresActualSummaryAndTeamListFiltersSubmittedInSql(){
        assertThrows(BizException.class,()->WorkspaceReviewReportService.validate(Map.of("period","2026年终","summary",""),true));
        when(identity.team()).thenReturn(List.of(7L));service.list("team");
        verify(store).select(eq("review-report"),contains("status='submitted'"),eq(List.of(7L)));
    }
}
