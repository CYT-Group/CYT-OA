package com.cyt.center.module.bpm.controller.admin.processinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - 撤销流程实例 Request VO")
@Data
public class BpmProcessInstanceCancelReqVO {

    @Schema(description = "流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "流程实例编号不能为空")
    private String id;

    @Schema(description = "撤销原因")
    private String reason;

}
