package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 看板总览：窗口内 PV/UV + 今日 PV/UV + 被访问过的页面数。
 *
 * UV 按 username 去重且不含匿名访问，所以「UV 大于总人数」不可能出现；
 * 匿名访问只进 PV。
 */
@Schema(description = "管理后台 - 门户浏览看板总览 Response VO")
@Data
public class PortalAnalyticsSummaryRespVO {

    @Schema(description = "统计窗口天数", example = "30")
    private Integer days;

    @Schema(description = "窗口内浏览量")
    private Long totalPv;

    @Schema(description = "窗口内去重访问人数")
    private Long totalUv;

    @Schema(description = "今日浏览量（零点起，按服务器时区）")
    private Long todayPv;

    @Schema(description = "今日去重访问人数")
    private Long todayUv;

    @Schema(description = "窗口内被访问过的页面数")
    private Long activePages;

}
