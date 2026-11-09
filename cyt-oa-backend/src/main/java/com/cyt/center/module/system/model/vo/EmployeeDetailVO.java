package com.cyt.center.module.system.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** 通讯录公开工作资料；敏感信息只能从 HR 档案接口读取。 */
@Data
public class EmployeeDetailVO {
    private Long userId;
    private Long employeeId;
    private String wecomUserid;
    private String name;
    private String position;
    private String mobile;
    private String email;
    private String avatar;
    private Integer wecomStatus;
    private Boolean enabled;
    private LocalDateTime directorySyncedAt;
    private String mainDeptName;
    private List<String> deptNames;
    private String leaderName;
    private String managerName;
    private String jobNumber;
    private Integer entryStatus;
}
