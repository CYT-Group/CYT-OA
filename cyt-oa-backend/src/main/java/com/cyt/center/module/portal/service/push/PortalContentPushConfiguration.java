package com.cyt.center.module.portal.service.push;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PortalContentPushConfiguration {
    @Bean(name="portalPushExecutor")
    public ThreadPoolTaskExecutor executor(){var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(2);executor.setMaxPoolSize(2);executor.setQueueCapacity(100);executor.setThreadNamePrefix("portal-push-");executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(35);return executor;}
}
