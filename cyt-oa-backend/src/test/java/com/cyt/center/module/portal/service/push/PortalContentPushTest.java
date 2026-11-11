package com.cyt.center.module.portal.service.push;

import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO;
import com.cyt.center.module.portal.dal.mysql.push.PortalContentPushRepository;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortalContentPushTest {
    @Test void explicitPublicationIntentWaitsForTransactionCommit() {
        var repo=mock(PortalContentPushRepository.class);var dispatcher=mock(PortalContentPushDispatcher.class);var executor=mock(Executor.class);var operators=mock(PortalOperatorResolver.class);
        var service=new PortalContentPushService();ReflectionTestUtils.setField(service,"repository",repo);ReflectionTestUtils.setField(service,"dispatcher",dispatcher);ReflectionTestUtils.setField(service,"executor",executor);ReflectionTestUtils.setField(service,"operators",operators);
        when(operators.resolve(1L)).thenReturn(new PortalOperator(1L,"admin","管理员",null,null));
        when(repo.recipients(null,null)).thenReturn(List.of("user"));when(repo.enqueue(anyString(),anyString(),anyString(),anyList(),anyString(),anyBoolean())).thenReturn(12L);
        var news=new PortalNewsDO();news.setId(3L);news.setTitle("制度发布");news.setStatus("published");
        TransactionSynchronizationManager.initSynchronization();
        try{
            var result=service.news(1L,news,null,false,false);assertEquals("queued",result.get("status"));verifyNoInteractions(executor,dispatcher);
            TransactionSynchronizationManager.getSynchronizations().forEach(s->s.afterCommit());verify(executor).execute(any(Runnable.class));verifyNoInteractions(dispatcher);
        }finally{TransactionSynchronizationManager.clearSynchronization();}
    }
    @Test void recentSuccessStopsDuplicateBeforeAnyNetworkCall() {
        var repo=mock(PortalContentPushRepository.class);var gateway=mock(WecomGatewayClient.class);
        when(repo.job(3L,true)).thenReturn(Map.of("status","queued","push_trigger","news","target_key","news:8","content","test","force_send",false));
        when(repo.lockGuard("news","news:8")).thenReturn(LocalDateTime.now());
        new PortalContentPushDispatcher(repo,gateway,new ObjectMapper()).dispatch(3L);
        verify(repo).finish(eq(3L),eq("cooldown"),eq(0),eq(0),anyString());verifyNoInteractions(gateway);
    }
}
