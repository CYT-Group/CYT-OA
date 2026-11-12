package com.cyt.center.module.me.service;

import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.mapper.*;
import com.cyt.center.module.me.model.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MeReminderServiceTest {
    final NotificationMapper notifications = mock(NotificationMapper.class);
    final NotifyPreferenceMapper preferences = mock(NotifyPreferenceMapper.class);
    final MilestoneMapper milestones = mock(MilestoneMapper.class);
    final HrmEmployeeService employees = mock(HrmEmployeeService.class);
    final MeRhythmService rhythm = mock(MeRhythmService.class);
    final MeReminderService service = new MeReminderService(notifications, preferences, milestones,
            null, null, employees, rhythm);

    void birthdayOnly() {
        var preference = new NotifyPreference();
        preference.setReportDue(false); preference.setOverdue(false);
        preference.setMilestone(true); preference.setCorpNews(false);
        when(preferences.selectById(7L)).thenReturn(preference);
        when(rhythm.upcomingFollowedNodes(eq(7L), any())).thenReturn(List.of());
        when(employees.getEmployeeByUserId(7L)).thenReturn(new HrmEmployeeDO().setId(70L).setUserId(7L)
                .setBirthday(LocalDate.now().withYear(1990).atStartOfDay()));
    }

    @Test void existingSameDayHistoryIsReusedAndLockPrecedesAllDerivedReads() {
        birthdayOnly();
        var oldest = new Milestone(); oldest.setId(10L);
        when(milestones.selectByTypeAndDate(7L, "birthday", LocalDate.now())).thenReturn(oldest);
        service.sync(7L);
        var ordered = inOrder(milestones, preferences);
        ordered.verify(milestones).lockUserForSync(7L);
        ordered.verify(preferences).selectById(7L);
        verify(milestones, never()).insert(any(Milestone.class));
        verify(milestones, never()).deleteById(anyLong());
        verify(milestones).deleteDerivedNotDate(7L, "birthday", LocalDate.now());
    }

    @Test void concurrentSummaryAndOverviewReuseOneMilestoneOnlyAfterFirstTransactionCommits() throws Exception {
        birthdayOnly();
        var storedMilestones = new CopyOnWriteArrayList<Milestone>();
        var storedNotifications = new CopyOnWriteArrayList<Notification>();
        when(milestones.selectByTypeAndDate(anyLong(), anyString(), any())).thenAnswer(call -> storedMilestones.stream()
                .filter(row -> Objects.equals(row.getUserId(), call.getArgument(0))
                        && Objects.equals(row.getType(), call.getArgument(1))
                        && Objects.equals(row.getMilestoneDate(), call.getArgument(2))).findFirst().orElse(null));
        when(milestones.insert(any(Milestone.class))).thenAnswer(call -> { storedMilestones.add(call.getArgument(0)); return 1; });
        when(notifications.selectByDedupKey(anyLong(), anyString())).thenAnswer(call -> storedNotifications.stream()
                .filter(row -> Objects.equals(row.getUserId(), call.getArgument(0))
                        && Objects.equals(row.getDedupKey(), call.getArgument(1))).findFirst().orElse(null));
        when(notifications.insert(any(Notification.class))).thenAnswer(call -> { storedNotifications.add(call.getArgument(0)); return 1; });

        // 模拟数据库行锁：与事务完成绑定，而不是在 Java 方法返回时释放。
        var accountLock = new Semaphore(1);
        var secondLockAttempted = new CountDownLatch(1);
        var lockCalls = new AtomicInteger();
        when(milestones.lockUserForSync(7L)).thenAnswer(call -> {
            if (lockCalls.incrementAndGet() == 2) secondLockAttempted.countDown();
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(accountLock.tryAcquire(5, TimeUnit.SECONDS));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { accountLock.release(); }
            });
            return 7L;
        });
        var firstCommitStarted = new CountDownLatch(1);
        var allowFirstCommit = new CountDownLatch(1);
        var firstCommit = new AtomicBoolean(true);
        var manager = new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) { }
            @Override protected void doCommit(DefaultTransactionStatus status) {
                if (!firstCommit.compareAndSet(true, false)) return;
                firstCommitStarted.countDown();
                try { assertTrue(allowFirstCommit.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            }
            @Override protected void doRollback(DefaultTransactionStatus status) { }
        };
        var transaction = new TransactionTemplate(manager);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> transaction.executeWithoutResult(status -> service.sync(7L)));
            assertTrue(firstCommitStarted.await(5, TimeUnit.SECONDS));
            var second = pool.submit(() -> transaction.executeWithoutResult(status -> service.sync(7L)));
            assertTrue(secondLockAttempted.await(5, TimeUnit.SECONDS));
            assertFalse(second.isDone(), "第一趟 Java 方法已返回，提交前第二趟仍必须等待行锁");
            verify(preferences, times(1)).selectById(7L);
            allowFirstCommit.countDown();
            first.get(5, TimeUnit.SECONDS); second.get(5, TimeUnit.SECONDS);
            assertEquals(1, storedMilestones.size());
            assertEquals(1, storedNotifications.size());
            assertEquals(1, accountLock.availablePermits());
        } finally { allowFirstCommit.countDown(); pool.shutdownNow(); }
    }

    @Test void accountLockAndDerivedWritesShareTheSameTransactionBoundary() throws Exception {
        var annotation = MeReminderService.class.getMethod("sync", Long.class).getAnnotation(Transactional.class);
        assertNotNull(annotation);
        assertTrue(List.of(annotation.rollbackFor()).contains(Exception.class));
    }
}
