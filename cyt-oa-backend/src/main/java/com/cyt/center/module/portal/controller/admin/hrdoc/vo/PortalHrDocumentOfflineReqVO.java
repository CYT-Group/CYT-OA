package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * HR 文档下架请求体（对齐 Portal 的 OfflineRequest）。
 */
@Schema(description = "管理后台 - HR 文档下架 Request VO")
@Data
public class PortalHrDocumentOfflineReqVO {

    @Schema(description = "下架原因（选填）")
    @Size(max = 200, message = "下架原因不能超过 200 字")
    private String reason;

}
