package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.mysql.hrdoc.PortalHrEditorRepository;
import com.cyt.center.module.portal.dal.mysql.push.PortalContentPushRepository;
import com.cyt.center.module.portal.service.push.PortalContentPushDispatcher;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortalHrEditorCompatibilityServiceTest {
    final PortalHrDocumentService documents=mock(PortalHrDocumentService.class);
    final PortalHrEditorRepository editor=mock(PortalHrEditorRepository.class);
    final PortalContentPushDispatcher dispatcher=mock(PortalContentPushDispatcher.class);
    final PortalContentPushRepository jobs=mock(PortalContentPushRepository.class);
    final PortalOperatorResolver operators=mock(PortalOperatorResolver.class);
    final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    final PortalHrEditorCompatibilityService service=new PortalHrEditorCompatibilityService(documents,editor,dispatcher,jobs,operators,transactions);
    PortalHrDocumentDO doc;
    @BeforeEach void setup(){
        doc=new PortalHrDocumentDO();doc.setId(7L);doc.setTitle("已发布标题");doc.setVersion("v1.0");doc.setRequiresSignoff(false);doc.setStatus("draft");doc.setPublishedAt(LocalDateTime.now());doc.setDraftTitle("未发布改稿");doc.setDraftVersion("v2.0");doc.setDraftRequiresSignoff(true);
        when(documents.getDetail(7L)).thenReturn(new PortalHrDocumentService.Detail(doc,"rev",Map.of()));
        when(operators.resolve(99L)).thenReturn(new PortalOperator(99L,"hr","人事",null,null));
        when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(jobs.enqueue(anyString(),anyString(),anyString(),anyList(),eq("hr"),anyBoolean())).thenReturn(12L);
        when(jobs.job(12L,false)).thenReturn(Map.of("id",12L,"status","sent","sent_count",1,"failed_count",0,"target_count",1));
    }
    @Test void previewSendsOnlyToActorWithoutChangingDocument(){
        when(jobs.recipients(null,List.of("hr"))).thenReturn(List.of("wecom-hr"));
        var result=service.test(99L,7L,"预览标题","v3.0",false);
        verify(jobs).enqueue(eq("hr_doc_test"),eq("article:7:test:hr"),argThat(message->message.contains("预览标题")&&message.contains("v3.0")&&!message.contains("需要签收")),eq(List.of("wecom-hr")),eq("hr"),eq(true));
        verify(documents,only()).getDetail(7L);verify(dispatcher).dispatch(12L);assertEquals(1,result.get("sent"));
        assertEquals("未发布改稿",doc.getDraftTitle());assertEquals("draft",doc.getStatus());verify(transactions).commit(any());
    }
    @Test void onlineDraftPushUsesPublishedSnapshotAndExplicitAllIsNotCapped(){
        List<String> employees=new ArrayList<>();for(int i=0;i<100;i++)employees.add("u"+i);when(jobs.recipients(null,null)).thenReturn(employees);
        service.notify(99L,7L,"all",null,null,false,null);
        verify(jobs).enqueue(eq("hr_doc"),eq("article:7:v1.0"),argThat(message->message.contains("已发布标题")&&!message.contains("未发布改稿")&&!message.contains("需要签收")),eq(employees),eq("hr"),eq(false));
    }
    @Test void implicitScopeCannotQuietlyBroadcastToMoreThanFifty(){
        when(jobs.recipients(null,null)).thenReturn(java.util.stream.IntStream.range(0,51).mapToObj(i->"u"+i).toList());
        assertThrows(BizException.class,()->service.notify(99L,7L,"scope",null,null,false,null));
        verify(jobs,never()).enqueue(anyString(),anyString(),anyString(),anyList(),anyString(),anyBoolean());verifyNoInteractions(dispatcher);
    }
    @Test void offlineDocumentsAndInactiveSelectionsAreRejected(){
        doc.setOfflineAt(LocalDateTime.now());assertThrows(BizException.class,()->service.notify(99L,7L,"all",null,null,false,null));
        doc.setOfflineAt(null);when(editor.allActive(List.of("departed"))).thenReturn(false);
        assertThrows(BizException.class,()->service.notify(99L,7L,"users",null,List.of("departed"),false,null));verifyNoInteractions(dispatcher);
    }
    @Test void gatewayFailureIsReturnedAsFailureRatherThanQueuedSuccess(){
        when(jobs.recipients(null,List.of("hr"))).thenReturn(List.of("wecom-hr"));
        when(jobs.job(12L,false)).thenReturn(Map.of("id",12L,"status","failed","sent_count",0,"failed_count",1,"target_count",1,"error_msg","网关不可用"));
        var result=service.test(99L,7L,null,null,null);
        assertEquals(false,result.get("success"));assertEquals(1,result.get("failed"));assertEquals(List.of("网关不可用"),result.get("errors"));
    }
}
