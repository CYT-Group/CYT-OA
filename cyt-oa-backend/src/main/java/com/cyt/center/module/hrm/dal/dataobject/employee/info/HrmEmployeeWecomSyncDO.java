package com.cyt.center.module.hrm.dal.dataobject.employee.info;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("hrm_employee_wecom_sync")
public class HrmEmployeeWecomSyncDO {
    @TableId(value = "employee_id", type = IdType.INPUT)
    private Long employeeId;
    private LocalDateTime birthday;
    private LocalDateTime entryTime;
    private LocalDateTime syncedAt;
}
