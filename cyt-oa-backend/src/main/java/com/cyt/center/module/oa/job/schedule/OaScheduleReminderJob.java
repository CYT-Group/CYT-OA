package com.cyt.center.module.oa.job.schedule;

import cn.hutool.core.util.StrUtil;
import com.cyt.center.framework.quartz.core.handler.JobHandler;
import com.cyt.center.module.oa.service.schedule.OaScheduleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * OA 日程提醒 Job
 *
 * @author 芋道源码
 */
@Component
public class OaScheduleReminderJob implements JobHandler {

    @Resource
    private OaScheduleService scheduleService;

    @Overridepublic String execute(String param) {
        int count = scheduleService.sendScheduleReminders();
        return StrUtil.format("发送日程提醒 {} 条", count);
    }

}
