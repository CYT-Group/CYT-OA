package com.cyt.center.module.oa.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 请假申请同步审批状态 Request VO")
@Data
public class OaLeaveApplySyncReqVO {

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "申请编号不能为空")
    private Long id;

}
