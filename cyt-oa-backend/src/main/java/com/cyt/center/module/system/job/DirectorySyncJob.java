package com.cyt.center.module.system.job;

import com.cyt.center.module.system.service.EnterpriseDirectorySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每天一次的通讯录兜底同步；离职与转岗主要仍靠这里，手动同步只用于立即生效的场景。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cyt.directory", name = "sync-enabled", havingValue = "true")
public class DirectorySyncJob {

    private final EnterpriseDirectorySyncService directorySync;

    /** 定时任务同样只负责发起；结果由后台线程写进同步记录，这里不占用调度线程等待。 */
    @Scheduled(cron = "${cyt.directory.sync-cron}")
    public void sync() {
        try {
            log.info("定时通讯录同步已发起 runId={}", directorySync.start("schedule", "system").id());
        } catch (RuntimeException error) {
            log.warn("定时通讯录同步未发起：{}", error.getMessage());
        }
    }

}
