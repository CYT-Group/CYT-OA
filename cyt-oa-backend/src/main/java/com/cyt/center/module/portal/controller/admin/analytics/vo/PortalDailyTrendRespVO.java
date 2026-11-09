package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 每日趋势行。
 *
 * 稀疏序列：窗口内没人访问的日期不补零，直接缺席；窗口第一天是不完整的一天
 * （从「此刻往前推 N 天」起算，不是从那天零点），前端画折线时别把它当整天。
 */
@Schema(description = "管理后台 - 门户每日浏览趋势 Response VO")
@Data
public class PortalDailyTrendRespVO {

    @Schema(description = "日期", example = "2026-11-05")
    private String date;

    @Schema(description = "当日浏览量")
    private Long pv;

    @Schema(description = "当日去重访问人数")
    private Long uv;

}
