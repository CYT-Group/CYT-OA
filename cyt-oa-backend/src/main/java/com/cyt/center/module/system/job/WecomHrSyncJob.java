package com.cyt.center.module.system.job;

import com.cyt.center.module.hrm.service.employee.info.WecomHrSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "cyt.directory", name = "hr-sync-enabled", havingValue = "true", matchIfMissing = true)
public class WecomHrSyncJob {
    private final WecomHrSyncService service;
    @Scheduled(cron = "${cyt.directory.hr-sync-cron:0 40 6 * * *}", zone = "Asia/Shanghai")
    public void sync() {
        try { service.sync(); }
        catch (RuntimeException error) { log.warn("企微花名册自动同步未完成，请通过员工档案页重试并检查网关授权"); }
    }
}
