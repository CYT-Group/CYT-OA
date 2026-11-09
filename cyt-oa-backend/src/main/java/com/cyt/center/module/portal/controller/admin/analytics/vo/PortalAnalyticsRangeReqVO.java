package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 看板时间窗：只看 days。
 *
 * 窗口是「此刻往前推 N 天」，不是「N 个自然日」——第一天是不完整的一天，Portal 口径照搬。
 */
@Schema(description = "管理后台 - 门户浏览看板时间窗 Request VO")
@Data
public class PortalAnalyticsRangeReqVO {

    @Schema(description = "统计窗口天数，1..365", example = "30")
    @NotNull(message = "days 不能为空")
    @Min(value = 1, message = "days 不能小于 1")
    @Max(value = 365, message = "days 不能大于 365")
    private Integer days = 30;

}
