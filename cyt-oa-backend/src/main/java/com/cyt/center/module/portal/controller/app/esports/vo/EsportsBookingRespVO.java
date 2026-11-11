package com.cyt.center.module.portal.controller.app.esports.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 电竞室预约响应（迁自 Portal 的 _booking_row）。
 *
 * purpose / participants / review_remark / steps 的 acted_* / remark / logs 是 private 字段，
 * 公共日历视角（scope=calendar 或非参与人）下由服务层置 null。
 */
@Schema(description = "门户 - 电竞室预约 Response VO")
@Data
public class EsportsBookingRespVO {

    private Long id;

    private LocalDate bookingDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String title;

    private String purpose;

    private Integer participantCount;

    private List<Map<String, Object>> participants;

    private Long applicantUserId;

    private String applicantName;

    private Long approverUserId;

    private String approverName;

    private String status;

    private String reviewedByName;

    private String approvalMode;

    private Integer currentStageIndex;

    private List<Long> currentApproverUserIds;

    private List<EsportsApprovalStepRespVO> approvalSteps;

    private String reviewRemark;

    private LocalDateTime reviewedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isMine;

    private Boolean canReview;

    private Boolean canCancel;

    private List<EsportsBookingLogRespVO> logs;

}
