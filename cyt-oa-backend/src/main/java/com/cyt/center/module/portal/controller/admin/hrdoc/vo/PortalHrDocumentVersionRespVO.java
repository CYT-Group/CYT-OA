package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR 文档发布版本快照响应（列表摘要 + 详情含正文）。
 */
@Schema(description = "管理后台 - HR 文档发布版本 Response VO")
@Data
public class PortalHrDocumentVersionRespVO {

    private Long id;

    private Long documentId;

    private String version;

    private String title;

    private LocalDateTime publishedAt;

    private String publishedBy;

    private String approvedBy;

    private String submittedBy;

    private String legacySlug;

    /** 详情口径才回；列表为 null */
    private String contentHtml;

    /** 详情口径才回；块格式版本非 NULL */
    private List<Object> contentBlocks;

}
