package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * HR 文档 slug 占用检查响应（对齐 Portal 的 check_slug_available）。
 */
@Schema(description = "管理后台 - HR 文档 slug 占用检查 Response VO")
@Data
public class PortalHrDocumentSlugCheckRespVO {

    private Boolean available;

    /** invalid / taken / self */
    private String reason;

    private String message;

    /** reason=taken 时给占用方摘要 */
    private Map<String, Object> owner;

    @Schema(description = "占用人信息（taken 时）")
    @Data
    public static class Owner {
        private String title;
        private String status;
        private LocalDateTime publishedAt;
        private LocalDateTime offlineAt;
        private LocalDateTime deletedAt;
    }

}
