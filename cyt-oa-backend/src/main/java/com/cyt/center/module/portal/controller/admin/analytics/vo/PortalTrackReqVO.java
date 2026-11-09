package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 页面浏览埋点上报请求体（Portal 展示端每次进页面打一次）。
 *
 * 刻意不收 username / display_name / department：这三列是服务端按当前登录人快照写死的，
 * 让前端传就等于允许伪造统计归属。user-agent 也不在这里，取自请求头。
 */
@Schema(description = "门户 - 页面浏览埋点上报 Request VO")
@Data
public class PortalTrackReqVO {

    @Schema(description = "页面地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "/news-detail?id=1")
    @NotNull(message = "page_url 不能为空")
    @Size(max = 500, message = "页面地址不能超过 500 字")
    private String pageUrl;

    @Schema(description = "页面标题")
    @Size(max = 300, message = "页面标题不能超过 300 字")
    private String pageTitle;

    @Schema(description = "来源地址")
    @Size(max = 500, message = "来源地址不能超过 500 字")
    private String referrer;

}
