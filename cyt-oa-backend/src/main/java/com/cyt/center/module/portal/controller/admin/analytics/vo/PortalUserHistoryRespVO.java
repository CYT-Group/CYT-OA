package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 某个人的浏览明细行（按浏览时间倒序）。
 */
@Schema(description = "管理后台 - 门户用户浏览轨迹 Response VO")
@Data
public class PortalUserHistoryRespVO {

    @Schema(description = "页面地址")
    private String pageUrl;

    @Schema(description = "页面标题")
    private String pageTitle;

    @Schema(description = "浏览时间")
    private LocalDateTime viewedAt;

}
