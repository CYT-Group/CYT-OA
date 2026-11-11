package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR 文档草稿版本快照响应（列表只回元信息 + 摘要，详情才带完整正文）。
 */
@Schema(description = "管理后台 - HR 文档草稿版本 Response VO")
@Data
public class PortalHrDocumentDraftVersionRespVO {

    private Long id;

    private String title;

    private String savedBy;

    private LocalDateTime createdAt;

    private String format;

    /** 列表口径的展示摘要（块数 / 字数），后端算好，列表端免拉正文 */
    private String summary;

    /** 详情口径才回 */
    private String contentHtml;

    /** 详情口径才回；块格式快照非 NULL */
    private List<Object> contentBlocks;

}
