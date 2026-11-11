package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * HR 文档管理端详情（DO 全字段 + 发布确认凭据 + 发布状态）。
 *
 * 继承 DO 拿全字段（含 draft_*），再补三个读侧计算字段；canonicalUrl 是 ID-first 稳定地址。
 */
@Schema(description = "管理后台 - HR 文档详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalHrDocumentRespVO extends PortalHrDocumentDO {
    @Schema(description = "旧路由兼容标识，新文档为空")
    private String legacySlug;

    @Schema(description = "最近一次正式企微推送，供原推送弹窗显示24小时冷却信息")
    private Map<String,Object> lastPush;

    @Schema(description = "稳定地址：/article/{id}")
    private String canonicalUrl;

    @Schema(description = "发布确认凭据（publish 时回传，防并发编辑）")
    private String revision;

    @Schema(description = "发布状态：publishedBefore / bodyChanged / currentVersion")
    private Map<String, Object> publicationState;

}
