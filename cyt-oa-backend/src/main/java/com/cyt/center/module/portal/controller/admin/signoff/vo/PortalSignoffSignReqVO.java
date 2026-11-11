package com.cyt.center.module.portal.controller.admin.signoff.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 制度签收请求体（对齐 Portal 的 SignRequest）。
 */
@Schema(description = "门户 - 制度签收 Request VO")
@Data
public class PortalSignoffSignReqVO {

    @Schema(description = "文档标识（slug 或 legacy key）")
    @Size(min = 1, max = 64, message = "document 长度需在 1-64 之间")
    private String document;

    @Schema(description = "稳定文档 ID（与 document 二选一）")
    private Long documentId;

    @Schema(description = "签收版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "v2.0")
    @Size(min = 1, max = 16, message = "版本号长度需在 1-16 之间")
    private String version;

}
