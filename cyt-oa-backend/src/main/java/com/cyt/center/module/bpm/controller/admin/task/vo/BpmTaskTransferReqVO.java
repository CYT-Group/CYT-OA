package com.cyt.center.module.bpm.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 任务转办 Request VO")
@Data
public class BpmTaskTransferReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "转办目标用户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转办目标用户不能为空")
    private Long targetUserId;

}
