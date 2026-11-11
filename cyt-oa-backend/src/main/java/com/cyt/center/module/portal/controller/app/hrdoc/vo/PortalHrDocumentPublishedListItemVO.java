package com.cyt.center.module.portal.controller.app.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * HR 文档员工读侧列表摘要（对齐 Portal 的 /articles/published 摘要，不含正文与草稿）。
 */
@Schema(description = "门户 - HR 文档已发布列表项 Response VO")
@Data
public class PortalHrDocumentPublishedListItemVO {

    private Long id;

    private String canonicalUrl;

    private String legacySlug;

    private String title;

    private String category;

    private String topic;

    private String intro;

    private Boolean requiresSignoff;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    private String theme;

    private Map<String, Object> cover;

}
