package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 电竞室一个审批环节（迁自 Portal 的 EsportsApprovalStage）。
 */
@Schema(description = "门户 - 电竞室审批环节 Request VO")
@Data
public class EsportsStageReqVO {

    @Schema(description = "环节内候选人账号（环节内任一人通过即推进）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "每个审批环节须配置审批人")
    @Size(max = 20, message = "每个审批环节最多 20 人")
    private List<Long> approverUserIds;

}
