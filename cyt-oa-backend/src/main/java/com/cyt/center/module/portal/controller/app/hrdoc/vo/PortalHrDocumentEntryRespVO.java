package com.cyt.center.module.portal.controller.app.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * HR 文档固定入口解析结果（对齐 Portal 的 /article-entries）。
 */
@Schema(description = "门户 - HR 文档固定入口 Response VO")
@Data
public class PortalHrDocumentEntryRespVO {

    private String entryKey;

    private Long id;

    private String canonicalUrl;

    private String title;

    private String category;

    private String intro;

    private Boolean requiresSignoff;

    private String legacySlug;

}
