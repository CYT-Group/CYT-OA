package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 电竞室开放规则更新请求（迁自 Portal 的 EsportsSettingsUpdate）。
 *
 * 完整开放规则，不接受客户端伪造能力或审计字段；旧请求可不传 approval_stages（保留单人投影）。
 */
@Schema(description = "门户 - 电竞室开放规则更新 Request VO")
@Data
public class EsportsSettingsUpdateReqVO {

    @Schema(description = "是否开放预约", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "is_open 不能为空")
    private Boolean isOpen;

    @Schema(description = "开放星期（ISO 1-7）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "open_weekdays 不能为空")
    @Size(max = 7, message = "开放星期最多 7 个")
    private List<Integer> openWeekdays;

    @Schema(description = "每日开放开始时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "open_start_time 不能为空")
    private LocalTime openStartTime;

    @Schema(description = "每日开放结束时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "open_end_time 不能为空")
    private LocalTime openEndTime;

    @Schema(description = "可提前预约天数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "advance_days 不能为空")
    @Min(value = 0, message = "advance_days 须在 0-90 之间")
    @Max(value = 90, message = "advance_days 须在 0-90 之间")
    private Integer advanceDays;

    @Schema(description = "单次最长预约分钟", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "max_duration_minutes 不能为空")
    @Min(value = 1, message = "max_duration_minutes 须在 1-1440 之间")
    @Max(value = 1440, message = "max_duration_minutes 须在 1-1440 之间")
    private Integer maxDurationMinutes;

    @Schema(description = "旧单人审批人（新流程为第一环节第一人投影）")
    private Long approverUserId;

    @Schema(description = "审批模式：any（任一）/ sequential（顺序）", example = "any")
    private String approvalMode;

    @Schema(description = "有序审批环节（新请求显式传入时为准，最多 10 环节）")
    @Size(max = 10, message = "审批环节最多 10 个")
    private List<EsportsStageReqVO> approvalStages;

}
