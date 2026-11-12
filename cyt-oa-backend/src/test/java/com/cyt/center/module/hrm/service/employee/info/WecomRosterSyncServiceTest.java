package com.cyt.center.module.hrm.service.employee.info;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.service.EnterpriseDirectorySyncService;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WecomRosterSyncServiceTest {
    final WecomRosterSyncRepository records = mock(WecomRosterSyncRepository.class);
    final EnterpriseDirectorySyncService directory = mock(EnterpriseDirectorySyncService.class);
    final WecomHrSyncService hr = mock(WecomHrSyncService.class);
    final List<Runnable> tasks = new ArrayList<>();
    final Map<String, WecomRosterSyncService.Run> stored = new HashMap<>();
    final WecomRosterSyncService service = new WecomRosterSyncService(records, directory, hr, tasks::add);

    WecomRosterSyncServiceTest() {
        doAnswer(call -> { var run = call.getArgument(0, WecomRosterSyncService.Run.class); stored.put(run.id(), run); return null; }).when(records).insert(any());
        doAnswer(call -> { var run = call.getArgument(0, WecomRosterSyncService.Run.class); stored.put(run.id(), run); return null; }).when(records).save(any());
        when(records.find(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
    }

    WecomHrSyncService.Result outcome(int failed, int skipped) {
        var result = mock(WecomHrSyncService.Result.class);
        when(result.failed()).thenReturn(failed);when(result.skipped()).thenReturn(skipped);
        when(result.conflicts()).thenReturn(List.of());
        return result;
    }

    void directorySucceeds(int missing) {
        when(directory.syncNow("manual", "Tester#7")).thenReturn(new EnterpriseDirectorySyncService.DirectoryOutcome(
                new EnterpriseDirectorySyncService.SyncResult(2, 1, 3, List.of(), missing > 0 ? List.of("old-member") : List.of()),
                Set.of("RealUser", "AnotherUser")));
    }

    @Test void returnsImmediatelyThenCommitsDirectoryBeforeReadingOnlyVerifiedMembers() {
        directorySucceeds(0);var result = outcome(0,0);
        when(hr.sync(Set.of("RealUser", "AnotherUser"))).thenReturn(result);
        var run = service.start("manual", "Tester#7");
        assertEquals("running",run.status());assertEquals("directory",run.stage());
        verifyNoInteractions(directory,hr);
        tasks.removeFirst().run();
        var order=inOrder(directory,hr);
        order.verify(directory).syncNow("manual","Tester#7");
        order.verify(hr).sync(Set.of("RealUser", "AnotherUser"));
        var finished=service.status(run.id());
        assertEquals("success",finished.status());assertEquals("complete",finished.stage());
        assertSame(result,finished.result());assertEquals(2,finished.directory().created());
        assertNotNull(finished.finishedAt());
    }

    @Test void directoryFailureStopsHrAndPersistsFailureForLaterReads() {
        when(directory.syncNow(anyString(),anyString())).thenThrow(new BizException("通讯录不可读"));
        var run=service.start("manual","Tester#7");tasks.removeFirst().run();
        var failed=service.status(run.id());
        assertEquals("failed",failed.status());assertEquals("directory",failed.stage());
        assertTrue(failed.message().contains("通讯录不可读"));verifyNoInteractions(hr);
        assertNull(failed.result());
    }

    @Test void hrFailureKeepsCommittedDirectorySummaryAndIdentifiesItsStage() {
        directorySucceeds(0);when(hr.sync(anySet())).thenThrow(new BizException("人事应用未授权"));
        var run=service.start("manual","Tester#7");tasks.removeFirst().run();
        var failed=service.status(run.id());
        assertEquals("failed",failed.status());assertEquals("hr",failed.stage());
        assertNotNull(failed.directory());assertTrue(failed.message().contains("人事应用未授权"));
    }

    @Test void skippedOrFailedEmployeesProducePartialInsteadOfSuccessfulResult() {
        directorySucceeds(1);var result=outcome(1,1);when(hr.sync(anySet())).thenReturn(result);
        var run=service.start("manual","Tester#7");tasks.removeFirst().run();
        assertEquals("partial",service.status(run.id()).status());
        assertEquals(1,service.status(run.id()).result().failed());
    }

    @Test void rejectsConcurrentStartsAndReleasesGuardAfterCompletion() {
        directorySucceeds(0);var result=outcome(0,0);when(hr.sync(anySet())).thenReturn(result);
        var first=service.start("manual","Tester#7");
        assertThrows(BizException.class,()->service.start("manual","Tester#7"));
        tasks.removeFirst().run();
        var second=service.start("manual","Tester#7");assertNotEquals(first.id(),second.id());
    }

    @Test void executorRejectionPersistsFailureAndReleasesTheDatabaseSlot() {
        var rejected=new WecomRosterSyncService(records,directory,hr,task->{throw new RejectedExecutionException();});
        assertThrows(BizException.class,()->rejected.start("manual","Tester#7"));
        assertEquals("failed",stored.values().iterator().next().status());
        verifyNoInteractions(directory,hr);
    }

    @Test void startupSettlesInterruptedRunsAndUnknownRunIsRejected() {
        service.settleInterruptedRuns();verify(records).settleInterrupted();
        assertThrows(BizException.class,()->service.status("missing"));
        service.status(null);verify(records).latest();
    }

    @Test void aFailedPersistenceDuringQueueRejectionDoesNotLeaveTheProcessLocked() {
        var rejected=new WecomRosterSyncService(records,directory,hr,task->{throw new RejectedExecutionException();});
        doThrow(new IllegalStateException("database unavailable")).when(records).save(any());
        assertThrows(IllegalStateException.class,()->rejected.start("manual","Tester#7"));
        assertThrows(IllegalStateException.class,()->rejected.start("manual","Tester#7"));
        verify(records,times(2)).insert(any());
    }
}
