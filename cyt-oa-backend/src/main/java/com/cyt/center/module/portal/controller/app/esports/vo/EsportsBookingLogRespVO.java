package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电竞室预约流水（迁自 Portal 的 _booking_row 的 logs）。
 */
@Schema(description = "门户 - 电竞室流水 Response VO")
@Data
public class EsportsBookingLogRespVO {

    private Long id;

    private String operation;

    private String actorName;

    private String remark;

    private LocalDateTime createdAt;

}
