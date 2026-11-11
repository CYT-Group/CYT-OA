package com.cyt.center.module.portal.controller.app.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * HR 文档员工读侧详情（对齐 Portal 的 _serialize_doc include_draft=false：只回已发布快照，不含草稿）。
 */
@Schema(description = "门户 - HR 文档已发布详情 Response VO")
@Data
public class PortalHrDocumentPublishedRespVO {

    private Long id;

    private String canonicalUrl;

    /** 旧 URL 兼容身份（新文档为 null） */
    private String legacySlug;

    private String title;

    private String category;

    private String topic;

    private String version;

    private Boolean requiresSignoff;

    private List<String> deptScope;

    private String contentHtml;

    private List<Object> contentBlocks;

    private LocalDateTime publishedAt;

    private String intro;

    private String coverImage;

    private String theme;

    private List<String> tags;

    private Boolean tocEnabled;

    private String tocPosition;

    private Map<String, Object> cover;

    private Boolean downloadPdfEnabled;

    private Map<String, Object> signoff;

    private LocalDateTime deletedAt;

    private String deletedBy;

}
