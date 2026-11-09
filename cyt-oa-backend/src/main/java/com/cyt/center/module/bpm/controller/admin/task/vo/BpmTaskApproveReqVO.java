package com.cyt.center.module.bpm.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 审批通过 Request VO")
@Data
public class BpmTaskApproveReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "审批意见")
    private String message;

    @Schema(description = "审批表单变量")
    private Map<String, Object> variables;

}
