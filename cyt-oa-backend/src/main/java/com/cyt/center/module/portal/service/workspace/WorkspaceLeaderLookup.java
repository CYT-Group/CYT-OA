package com.cyt.center.module.portal.service.workspace;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class WorkspaceLeaderLookup {
    private final HrmEmployeeService employees;
    public Long leader(Long user){var me=employees.getEmployeeByUserId(user);if(me==null||me.getLeaderEmployeeId()==null)return null;var leader=employees.getEmployee(me.getLeaderEmployeeId());return leader==null?null:leader.getUserId();}
}
