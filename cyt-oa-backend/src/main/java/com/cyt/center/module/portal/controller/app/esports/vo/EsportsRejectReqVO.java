package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 电竞室驳回请求体（迁自 Portal 的 EsportsReject）。驳回必须留原因。
 */
@Schema(description = "门户 - 电竞室驳回 Request VO")
@Data
public class EsportsRejectReqVO {

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "remark 不能为空")
    @Size(max = 1000, message = "remark 最长 1000 字")
    private String remark;

    @Schema(description = "期望审批环节")
    @Min(value = 0, message = "expected_stage_index 须在 0-9 之间")
    @Max(value = 9, message = "expected_stage_index 须在 0-9 之间")
    private Integer expectedStageIndex;

}
