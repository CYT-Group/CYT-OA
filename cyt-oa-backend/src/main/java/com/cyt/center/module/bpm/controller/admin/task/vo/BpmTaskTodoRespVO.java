package com.cyt.center.module.bpm.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 待办任务 Response VO")
@Data
public class BpmTaskTodoRespVO {

    @Schema(description = "任务编号")
    private Long id;

    @Schema(description = "流程实例编号")
    private Long instanceId;

    @Schema(description = "流程名")
    private String flowName;

    @Schema(description = "流程标识")
    private String flowCode;

    @Schema(description = "业务编号，例如请假申请编号")
    private String businessId;

    @Schema(description = "当前节点编码")
    private String nodeCode;

    @Schema(description = "当前节点名")
    private String nodeName;

    @Schema(description = "任务创建时间")
    private LocalDateTime createTime;

}
