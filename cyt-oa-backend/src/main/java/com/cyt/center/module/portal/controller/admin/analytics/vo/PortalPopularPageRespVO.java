package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门页面排行行。
 *
 * 按 page_url 分组，标题取组内 MAX(page_title)：同一 URL 的标题历史上有过改动的，
 * 这里给的是字典序最大的那个，不是「最新标题」——Portal 口径如此，照搬不改，
 * 否则两边的排行会对不上。
 */
@Schema(description = "管理后台 - 门户热门页面 Response VO")
@Data
public class PortalPopularPageRespVO {

    @Schema(description = "页面地址", example = "/news-detail")
    private String pageUrl;

    @Schema(description = "页面标题")
    private String pageTitle;

    @Schema(description = "浏览量")
    private Long pv;

    @Schema(description = "去重访问人数")
    private Long uv;

}
