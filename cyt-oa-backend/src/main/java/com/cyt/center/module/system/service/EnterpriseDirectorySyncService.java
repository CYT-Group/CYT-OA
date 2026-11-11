package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.WecomGatewayClient;
import com.cyt.center.module.system.mapper.SysDirectorySyncLogMapper;
import com.cyt.center.module.system.model.entity.SysDirectorySyncLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通讯录同步的调用编排：网络请求在事务之外完成，每次执行无论成败都留下一条运行记录。
 * 网关配额有限，同一时刻只允许一趟同步在跑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseDirectorySyncService {

    private static final int RUN_HISTORY = 20;

    private final WecomGatewayClient gateway;
    private final DirectorySnapshotWriter writer;
    private final SysDirectorySyncLogMapper syncLogs;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 翻全员全部门接近两分钟，放后台线程跑；页面只轮询运行记录，不吊着 HTTP 连接干等。 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "directory-sync");
        thread.setDaemon(true);
        return thread;
    });

    public record SyncResult(int created, int updated, int departments, List<String> conflicts, List<String> missing) {
    }

    /** status 取 running / success / failed；running 时结果列还没有值。 */
    public record SyncRun(Long id, String triggerType, String actor, String status, Integer created, Integer updated,
                          Integer departments, Integer conflicts, Integer missing, String message, Long durationMs,
                          LocalDateTime createdAt, LocalDateTime finishedAt) {
    }

    /** 先落一条「运行中」的记录再立刻返回，页面按这条记录轮询进度。 */
    public SyncRun start(String triggerType, String actor) {
        if (!running.compareAndSet(false, true)) {
            throw new BizException("已有一次通讯录同步正在执行，请等待结束后再试");
        }
        SysDirectorySyncLog row = new SysDirectorySyncLog();
        row.setTriggerType(triggerType);
        row.setActor(actor == null ? "" : actor);
        row.setOk(null);
        row.setCreatedAt(LocalDateTime.now());
        syncLogs.insert(row);
        long startedAt = System.nanoTime();
        try {
            executor.submit(() -> execute(row.getId(), triggerType, actor, startedAt));
        } catch (RuntimeException error) {
            finish(row.getId(), false, null, "同步未能启动：" + error.getClass().getSimpleName(), startedAt);
            running.set(false);
            log.error("企微通讯录同步启动失败 trigger={}, actor={}", triggerType, actor, error);
            throw new BizException("通讯录同步启动失败，具体原因见同步记录");
        }
        return toRun(row);
    }

    /** 最近一次运行：页面用它判断是否还在跑，也用它显示「上次同步」。 */
    public SyncRun latest() {
        SysDirectorySyncLog row = syncLogs.selectOne(new LambdaQueryWrapper<SysDirectorySyncLog>()
                .orderByDesc(SysDirectorySyncLog::getId).last("LIMIT 1"));
        return row == null ? null : toRun(row);
    }

    /** 进程重启会带走在跑的那一趟，库里留下的 running 行永远等不到收尾，开机时结算掉。 */
    @EventListener(ApplicationReadyEvent.class)
    public void settleInterruptedRuns() {
        SysDirectorySyncLog patch = new SysDirectorySyncLog();
        patch.setOk(false);
        patch.setMessage("服务重启，同步中断");
        patch.setFinishedAt(LocalDateTime.now());
        int settled = syncLogs.update(patch, new LambdaUpdateWrapper<SysDirectorySyncLog>()
                .isNull(SysDirectorySyncLog::getOk));
        if (settled > 0) {
            log.warn("结清 {} 条因服务重启中断的通讯录同步记录", settled);
        }
    }

    private void execute(Long runId, String triggerType, String actor, long startedAt) {
        try {
            SyncResult result = writer.apply(gateway.directory());
            finish(runId, true, result, result.missing().isEmpty() ? ""
                    : "已移除 " + result.missing().size() + " 个企微未返回的账号及关联员工档案", startedAt);
            log.info("企微通讯录同步完成 trigger={}, actor={}, created={}, updated={}, departments={}, "
                            + "conflicts={}, missing={}", triggerType, actor, result.created(), result.updated(),
                    result.departments(), result.conflicts().size(), result.missing().size());
        } catch (BizException error) {
            finish(runId, false, null, error.getMessage(), startedAt);
            log.warn("企微通讯录同步未成功 trigger={}, actor={}, message={}", triggerType, actor, error.getMessage());
        } catch (RuntimeException error) {
            finish(runId, false, null, "同步异常：" + error.getClass().getSimpleName(), startedAt);
            log.error("企微通讯录同步出现未预期异常 trigger={}, actor={}", triggerType, actor, error);
        } finally {
            running.set(false);
        }
    }

    public List<SyncRun> runs() {
        Page<SysDirectorySyncLog> page = syncLogs.selectPage(new Page<>(1, RUN_HISTORY),
                new LambdaQueryWrapper<SysDirectorySyncLog>().orderByDesc(SysDirectorySyncLog::getId));
        return page.getRecords().stream().map(this::toRun).toList();
    }

    private void finish(Long runId, boolean ok, SyncResult result, String message, long startedAt) {
        SysDirectorySyncLog row = new SysDirectorySyncLog();
        row.setId(runId);
        row.setOk(ok);
        row.setUserCreated(result == null ? 0 : result.created());
        row.setUserUpdated(result == null ? 0 : result.updated());
        row.setDepartments(result == null ? 0 : result.departments());
        row.setConflicts(result == null ? 0 : result.conflicts().size());
        row.setMissing(result == null ? 0 : result.missing().size());
        row.setMessage(truncate(message));
        row.setDurationMs(Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        row.setFinishedAt(LocalDateTime.now());
        syncLogs.updateById(row);
    }

    private SyncRun toRun(SysDirectorySyncLog row) {
        String status = row.getOk() == null ? "running" : Boolean.TRUE.equals(row.getOk()) ? "success" : "failed";
        return new SyncRun(row.getId(), row.getTriggerType(), row.getActor(), status, row.getUserCreated(),
                row.getUserUpdated(), row.getDepartments(), row.getConflicts(), row.getMissing(), row.getMessage(),
                row.getDurationMs(), row.getCreatedAt(), row.getFinishedAt());
    }

    private static String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message.length() <= 255 ? message : message.substring(0, 255);
    }

}
