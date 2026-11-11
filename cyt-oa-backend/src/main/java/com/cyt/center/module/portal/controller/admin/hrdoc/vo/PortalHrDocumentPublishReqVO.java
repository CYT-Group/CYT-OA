package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * HR 文档发布确认凭据（对齐 Portal 的 PublicationRequest）。
 */
@Schema(description = "管理后台 - HR 文档发布 Request VO")
@Data
public class PortalHrDocumentPublishReqVO {
    @Schema(description = "仅显式勾选后在发布成功后推送企微，默认不推送")
    private Boolean notifyWecom;

    @Schema(description = "推送部门范围；不传时跟随已发布文档的适用范围")
    private java.util.List<String> notifyDeptScope;

    @Schema(description = "发布确认凭据（编辑后回传；不传 = 不校验并发）", example = "sha256hex")
    @Size(min = 64, max = 64, message = "expected_revision 必须是 64 位 hex")
    private String expectedRevision;

    @Schema(description = "发布目标版本号（不传 = 用草稿版本/当前版本）")
    @Size(max = 16, message = "版本号不能超过 16 字")
    private String version;

}
