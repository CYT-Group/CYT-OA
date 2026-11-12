package com.cyt.center.module.system.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.module.system.mapper.SysDirectorySyncLogMapper;
import com.cyt.center.module.system.model.entity.SysDirectorySyncLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EnterpriseDirectorySyncServiceTest {
    final WecomGatewayClient gateway=mock(WecomGatewayClient.class);
    final DirectorySnapshotWriter writer=mock(DirectorySnapshotWriter.class);
    final SysDirectorySyncLogMapper logs=mock(SysDirectorySyncLogMapper.class);
    final EnterpriseDirectorySyncService service=new EnterpriseDirectorySyncService(gateway,writer,logs);
    final AtomicLong ids=new AtomicLong();
    final EnterpriseDirectorySyncService.SyncResult result=new EnterpriseDirectorySyncService.SyncResult(1,0,1,List.of(),List.of("former"));
    JsonNode snapshot;

    @BeforeEach void setup() throws Exception {
        snapshot=new ObjectMapper().readTree("{\"users\":[{\"userid\":\"Alice\"}],\"departments\":[{\"id\":1}],\"counts\":{\"users\":1,\"departments\":1}}");
        when(logs.insert(any(SysDirectorySyncLog.class))).thenAnswer(inv->{((SysDirectorySyncLog)inv.getArgument(0)).setId(ids.incrementAndGet());return 1;});
        when(gateway.directory()).thenReturn(snapshot);when(writer.apply(snapshot)).thenReturn(result);
    }
    @AfterEach void stopExecutor(){service.close();}

    @Test void synchronousPipelineGetsActualReturnedUseridsAndARecordedSuccess(){
        var outcome=service.syncNow("roster","operator#7");
        assertEquals(result,outcome.result());assertEquals(Set.of("Alice"),outcome.presentUserids());
        assertThrows(UnsupportedOperationException.class,()->outcome.presentUserids().add("invented"));
        var inserted=ArgumentCaptor.forClass(SysDirectorySyncLog.class);verify(logs).insert(inserted.capture());
        assertEquals("roster",inserted.getValue().getTriggerType());assertEquals("operator#7",inserted.getValue().getActor());
        var finished=ArgumentCaptor.forClass(SysDirectorySyncLog.class);verify(logs).updateById(finished.capture());
        assertTrue(finished.getValue().getOk());assertEquals(1,finished.getValue().getMissing());
        assertTrue(finished.getValue().getMessage().contains("已保留历史账号"));assertFalse(finished.getValue().getMessage().contains("已移除"));
        verify(writer).apply(snapshot);
    }

    @Test void invalidSnapshotFailureIsLoggedAndDoesNotKeepTheGuardLocked(){
        when(writer.apply(snapshot)).thenThrow(new BizException("企微通讯录结构不完整，未执行同步"));
        for(int i=0;i<2;i++)assertTrue(assertThrows(BizException.class,()->service.syncNow("roster","operator")).getMessage().contains("结构不完整"));
        var finished=ArgumentCaptor.forClass(SysDirectorySyncLog.class);verify(logs,times(2)).updateById(finished.capture());
        assertTrue(finished.getAllValues().stream().allMatch(row->Boolean.FALSE.equals(row.getOk())));
        verify(gateway,times(2)).directory();verify(writer,times(2)).apply(snapshot);
    }

    @Test void upstreamFailureNeverAppliesASnapshotAndAllowsRetry(){
        when(gateway.directory()).thenThrow(new BizException("网关读取失败"));
        assertThrows(BizException.class,()->service.syncNow("roster","operator"));
        assertThrows(BizException.class,()->service.syncNow("roster","operator"));
        verifyNoInteractions(writer);verify(logs,times(2)).updateById(argThat((SysDirectorySyncLog row)->Boolean.FALSE.equals(row.getOk())));
    }

    @Test void synchronousRunBlocksBothLegacyStartAndAnotherSynchronousRun() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(gateway.directory()).thenAnswer(inv->{entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test timeout");return snapshot;});
        var pending=CompletableFuture.supplyAsync(()->service.syncNow("roster","operator"));
        try{
            assertTrue(entered.await(2,TimeUnit.SECONDS));
            assertTrue(assertThrows(BizException.class,()->service.start("manual","operator")).getMessage().contains("正在执行"));
            assertThrows(BizException.class,()->service.syncNow("roster","operator"));
            verify(logs,times(1)).insert(any(SysDirectorySyncLog.class));
        }finally{release.countDown();}
        assertEquals(Set.of("Alice"),pending.get(3,TimeUnit.SECONDS).presentUserids());
        assertDoesNotThrow(()->service.syncNow("roster","operator"));
    }

    @Test void legacyAsyncStartBlocksTheSynchronousPipelineToo() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(gateway.directory()).thenAnswer(inv->{entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test timeout");return snapshot;});
        var run=service.start("manual","operator");assertEquals("running",run.status());
        try{
            assertTrue(entered.await(2,TimeUnit.SECONDS));
            assertThrows(BizException.class,()->service.syncNow("roster","operator"));
            verify(logs,times(1)).insert(any(SysDirectorySyncLog.class));
        }finally{release.countDown();}
        verify(logs,timeout(2000)).updateById(argThat((SysDirectorySyncLog row)->Boolean.TRUE.equals(row.getOk())));
    }

    @Test void logCreationFailureReleasesTheGuardBeforeRetry(){
        when(logs.insert(any(SysDirectorySyncLog.class))).thenThrow(new IllegalStateException("log store unavailable"))
                .thenAnswer(inv->{((SysDirectorySyncLog)inv.getArgument(0)).setId(ids.incrementAndGet());return 1;});
        assertThrows(BizException.class,()->service.syncNow("roster","operator"));
        assertEquals(result,service.syncNow("roster","operator").result());
        verify(gateway,times(1)).directory();verify(writer,times(1)).apply(snapshot);
    }
}
