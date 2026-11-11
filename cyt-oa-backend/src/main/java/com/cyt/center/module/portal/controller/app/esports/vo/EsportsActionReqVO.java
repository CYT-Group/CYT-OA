package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 电竞室通过 / 取消的请求体（迁自 Portal 的 EsportsAction）。
 */
@Schema(description = "门户 - 电竞室审批动作 Request VO")
@Data
public class EsportsActionReqVO {

    @Schema(description = "备注")
    @Size(max = 1000, message = "remark 最长 1000 字")
    private String remark;

    @Schema(description = "期望审批环节（多环节时前端带上，服务端校验是否已变化）")
    @Min(value = 0, message = "expected_stage_index 须在 0-9 之间")
    @Max(value = 9, message = "expected_stage_index 须在 0-9 之间")
    private Integer expectedStageIndex;

}
