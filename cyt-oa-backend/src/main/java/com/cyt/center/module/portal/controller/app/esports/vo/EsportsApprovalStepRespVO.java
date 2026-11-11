package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电竞室预约审批步骤快照（迁自 Portal 的 _booking_row 的 approval_steps）。
 *
 * acted_by_* / acted_at / remark 只在当事人（private）视角返回，公共日历隐藏。
 */
@Schema(description = "门户 - 电竞室审批步骤 Response VO")
@Data
public class EsportsApprovalStepRespVO {

    private Long id;

    private Integer stageIndex;

    private Long approverUserId;

    private String approverName;

    private String status;

    private Long actedByUserId;

    private String actedByName;

    private LocalDateTime actedAt;

    private String remark;

}
