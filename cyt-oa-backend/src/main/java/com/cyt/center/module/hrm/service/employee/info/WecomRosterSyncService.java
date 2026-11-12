package com.cyt.center.module.hrm.service.employee.info;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.service.EnterpriseDirectorySyncService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** 通讯录身份确认提交后才补人事资料；每阶段的结果可跨页面刷新读取。 */
@Service
@Slf4j
public class WecomRosterSyncService {
    public record Directory(int created, int updated, int departments, int conflicts, int missing) { }
    public record Run(String id, String status, String stage, String actor, String message,
                      Directory directory, WecomHrSyncService.Result result,
                      LocalDateTime createdAt, LocalDateTime finishedAt) { }

    private final WecomRosterSyncRepository records;
    private final EnterpriseDirectorySyncService directory;
    private final WecomHrSyncService hr;
    private final Executor executor;
    private final AtomicBoolean running = new AtomicBoolean();

    @Autowired
    public WecomRosterSyncService(WecomRosterSyncRepository records,
                                 EnterpriseDirectorySyncService directory, WecomHrSyncService hr) {
        this(records, directory, hr, Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "wecom-roster-sync");
            thread.setDaemon(true);
            return thread;
        }));
    }

    WecomRosterSyncService(WecomRosterSyncRepository records, EnterpriseDirectorySyncService directory,
                          WecomHrSyncService hr, Executor executor) {
        this.records = records;
        this.directory = directory;
        this.hr = hr;
        this.executor = executor;
    }

    public Run start(String trigger, String actor) {
        if (!running.compareAndSet(false, true)) throw new BizException("已有一次花名册同步正在执行");
        Run run = new Run(UUID.randomUUID().toString(), "running", "directory", truncate(actor, 160),
                "正在同步通讯录并关联员工档案", null, null, LocalDateTime.now(), null);
        try {
            records.insert(run);
        } catch (RuntimeException error) {
            running.set(false);
            if (error instanceof DuplicateKeyException) throw new BizException("已有一次花名册同步正在执行");
            throw error;
        }
        try { executor.execute(() -> execute(run, trigger)); }
        catch (RuntimeException error) {
            try { records.save(finish(run, "failed", "同步未能启动，请稍后重试", null)); }
            finally { running.set(false); }
            throw new BizException("花名册同步未能启动，请稍后重试");
        }
        return run;
    }

    public Run status(String id) {
        if (id == null || id.isBlank()) return records.latest();
        Run run = records.find(id);
        if (run == null) throw new BizException("同步记录不存在");
        return run;
    }

    private void execute(Run initial, String trigger) {
        Run run = initial;
        try {
            var outcome = directory.syncNow(trigger, run.actor());
            var result = outcome.result();
            Directory summary = new Directory(result.created(), result.updated(), result.departments(),
                    result.conflicts().size(), result.missing().size());
            run = new Run(run.id(), "running", "hr", run.actor(), "通讯录已完成，正在读取人事资料",
                    summary, null, run.createdAt(), null);
            records.save(run);
            var hrResult = hr.sync(outcome.presentUserids());
            boolean partial = hrResult.failed() > 0 || hrResult.skipped() > 0
                    || !hrResult.conflicts().isEmpty() || summary.conflicts() > 0 || summary.missing() > 0;
            records.save(finish(run, partial ? "partial" : "success", partial
                    ? "同步部分完成；失败、跳过及未确认字段已保留原值，请查看明细"
                    : "通讯录与人事资料同步完成", hrResult));
        } catch (BizException error) {
            records.save(finish(run, "failed", ("directory".equals(run.stage()) ? "通讯录阶段未完成："
                    : "人事资料阶段未完成：") + truncate(error.getMessage(), 400), null));
            log.warn("花名册同步未完成 run={}, stage={}", run.id(), run.stage());
        } catch (RuntimeException error) {
            records.save(finish(run, "failed", "同步异常，已完成的步骤保留，请联系管理员查看运行记录", null));
            log.error("花名册同步异常 run={}, stage={}, type={}", run.id(), run.stage(), error.getClass().getSimpleName());
        } finally { running.set(false); }
    }

    private Run finish(Run run, String status, String message, WecomHrSyncService.Result result) {
        return new Run(run.id(), status, "failed".equals(status) ? run.stage() : "complete", run.actor(),
                truncate(message, 500), run.directory(), result, run.createdAt(), LocalDateTime.now());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void settleInterruptedRuns() { records.settleInterrupted(); }

    @PreDestroy
    public void close() { if (executor instanceof ExecutorService service) service.shutdownNow(); }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
