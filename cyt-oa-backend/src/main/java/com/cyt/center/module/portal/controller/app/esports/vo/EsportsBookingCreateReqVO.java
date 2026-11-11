package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 电竞室预约请求（迁自 Portal 的 EsportsBookingCreate）。
 *
 * 申请人与审批人由服务器生成快照；名单请求可省略人数，旧请求可省略名单只给人数。
 */
@Schema(description = "门户 - 电竞室预约 Request VO")
@Data
public class EsportsBookingCreateReqVO {

    @Schema(description = "预约日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "booking_date 不能为空")
    private LocalDate bookingDate;

    @Schema(description = "开始时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "start_time 不能为空")
    private LocalTime startTime;

    @Schema(description = "结束时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "end_time 不能为空")
    private LocalTime endTime;

    @Schema(description = "预约标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "title 不能为空")
    @Size(max = 120, message = "title 最长 120 字")
    private String title;

    @Schema(description = "用途说明")
    @Size(max = 1000, message = "purpose 最长 1000 字")
    private String purpose;

    @Schema(description = "参加人数（省略名单时必须提供）")
    @Min(value = 1, message = "participant_count 须在 1-100 之间")
    @Max(value = 100, message = "participant_count 须在 1-100 之间")
    private Integer participantCount;

    @Schema(description = "参加人员账号（可省略人数）")
    @Size(min = 1, max = 100, message = "参加人员 1-100 人")
    private List<Long> participantUserIds;

}
