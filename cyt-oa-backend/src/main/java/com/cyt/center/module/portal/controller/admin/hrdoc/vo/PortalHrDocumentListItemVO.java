package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR 文档管理端列表摘要（对齐 Portal 的 _serialize_admin_list_item，不含正文）。
 */
@Schema(description = "管理后台 - HR 文档列表项 Response VO")
@Data
public class PortalHrDocumentListItemVO {

    private Long id;

    private String canonicalUrl;

    private String title;

    private Boolean requiresSignoff;

    private List<String> deptScope;

    private String version;

    private String draftTitle;

    private Boolean draftRequiresSignoff;

    private List<String> draftDeptScope;

    private String draftVersion;

    private String category;

    private String topic;

    private String status;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private String submittedBy;

    private LocalDateTime submittedAt;

    private String publishedBy;

    private LocalDateTime publishedAt;

    private LocalDateTime offlineAt;

    private String offlineBy;

    private String offlineReason;

    private LocalDateTime deletedAt;

    private String deletedBy;

    /** 旧 URL 兼容身份（slug 仅为历史兼容保留，新文档为 null） */
    private String legacySlug;

}
