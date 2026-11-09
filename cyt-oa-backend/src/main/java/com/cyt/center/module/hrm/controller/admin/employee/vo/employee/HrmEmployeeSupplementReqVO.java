package com.cyt.center.module.hrm.controller.admin.employee.vo.employee;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/** 只接收本地人事字段，企微身份及部门不进入可写请求。 */
public record HrmEmployeeSupplementReqVO(
        @NotNull Long id,
        @Size(max = 64) String jobNumber,
        Long leaderEmployeeId,
        LocalDateTime entryTime,
        @PastOrPresent LocalDateTime birthday,
        @Min(0) @Max(4) Integer entryStatus,
        @Min(1) @Max(2) Integer type,
        @Min(1) @Max(8) Integer status,
        @Size(max = 255) String postLevel,
        @Size(max = 64) String workCity,
        @Size(max = 255) String workAddress,
        @Size(max = 500) String remark) {
}
