package com.cyt.center.module.bpm.controller.admin.processinstance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 我发起的流程实例 Response VO")
@Data
public class BpmProcessInstanceRespVO {

    @Schema(description = "流程实例编号")
    private Long id;

    @Schema(description = "流程名")
    private String flowName;

    @Schema(description = "流程标识")
    private String flowCode;

    @Schema(description = "业务编号")
    private String businessId;

    @Schema(description = "流程状态：1 审批中 / 2 通过 / 8 完成 / 4 终止 / 6 撤销 / 9 退回")
    private String flowStatus;

    @Schema(description = "当前节点名")
    private String nodeName;

    @Schema(description = "发起时间")
    private LocalDateTime createTime;

}
