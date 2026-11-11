package com.cyt.center.module.hrm.service.workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class HrmOffboardAccountJob {
    private final JdbcTemplate jdbc;
    private final HrmWorkspaceService service;
    private final TransactionTemplate transaction;

    @Scheduled(cron="0 */5 * * * *",zone="Asia/Shanghai")
    public void run() {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='hrm_employment_document'",Integer.class)==0)return;
        var rows=jdbc.queryForList("SELECT d.id,d.employee_id,d.creator FROM hrm_employment_document d "
                + "JOIN hrm_employee e ON e.id=d.employee_id AND e.deleted=0 WHERE d.kind='offboard' "
                + "AND d.account_disabled=0 AND d.effective_date<=DATE(CONVERT_TZ(UTC_TIMESTAMP(),'+00:00','+08:00')) ORDER BY d.id LIMIT 100");
        for(var row:rows) try {
            transaction.executeWithoutResult(s->service.disableAccount(((Number)row.get("id")).longValue(),
                    ((Number)row.get("employee_id")).longValue(),((Number)row.get("creator")).longValue()));
        } catch(Exception ex) { log.warn("离职账号停用待重试，文书编号 {}",row.get("id")); }
    }
}
