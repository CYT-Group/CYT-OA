package com.cyt.center.module.portal.controller.app.esports.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 电竞室开放规则响应（迁自 Portal 的 _settings_row）。
 *
 * can_manage / can_view_all / can_review 由服务端按当前登录人能力计算，不下发到请求里。
 */
@Schema(description = "门户 - 电竞室开放规则 Response VO")
@Data
public class EsportsSettingsRespVO {

    private Boolean isOpen;

    private List<Integer> openWeekdays;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openEndTime;

    private Integer advanceDays;

    private Integer maxDurationMinutes;

    private Long approverUserId;

    private String approverName;

    private Boolean approverActive;

    private String timezone;

    private Boolean canManage;

    private Boolean canViewAll;

    private String approvalMode;

    private List<EsportsApproverStageRespVO> approvalStages;

    private Boolean canReview;

    private LocalDateTime updatedAt;

}
