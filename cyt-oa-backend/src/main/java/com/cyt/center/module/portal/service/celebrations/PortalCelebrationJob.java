package com.cyt.center.module.portal.service.celebrations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 切流时显式启用，防止新旧两套调度同时给员工发送祝福。 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix="cyt.portal.celebrations",name="scheduler-enabled",havingValue="true")
public class PortalCelebrationJob {
    private final PortalCelebrationService service;
    @Scheduled(cron="0 */15 * * * *",zone="Asia/Shanghai")
    public void run() {
        try { service.runDue(); }
        catch(RuntimeException ex) { log.warn("庆祝调度未完成，请查看 OA 庆祝日历执行记录"); }
    }
}
