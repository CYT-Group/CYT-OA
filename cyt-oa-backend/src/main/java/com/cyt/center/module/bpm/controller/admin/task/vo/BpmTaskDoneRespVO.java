package com.cyt.center.module.bpm.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 已办任务 Response VO")
@Data
public class BpmTaskDoneRespVO {

    @Schema(description = "历史任务编号")
    private Long id;

    @Schema(description = "流程实例编号")
    private Long instanceId;

    @Schema(description = "流程名")
    private String flowName;

    @Schema(description = "流程标识")
    private String flowCode;

    @Schema(description = "业务编号")
    private String businessId;

    @Schema(description = "办理节点编码")
    private String nodeCode;

    @Schema(description = "办理节点名")
    private String nodeName;

    @Schema(description = "目标节点编码，如退回目标")
    private String targetNodeCode;

    @Schema(description = "办理动作：PASS 通过 / REJECT 退回 / NONE 无动作")
    private String skipType;

    @Schema(description = "办理时流程状态：2 通过 / 8 完成 / 9 退回 / 6 撤销")
    private String flowStatus;

    @Schema(description = "审批意见")
    private String message;

    @Schema(description = "办理时间")
    private LocalDateTime createTime;

}
