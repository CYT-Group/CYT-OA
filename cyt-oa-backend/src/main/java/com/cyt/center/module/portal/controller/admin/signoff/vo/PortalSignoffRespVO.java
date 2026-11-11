package com.cyt.center.module.portal.controller.admin.signoff.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 制度签收响应 VO。
 */
@Schema(description = "门户 - 制度签收 Response VO")
@Data
public class PortalSignoffRespVO {

    private Long id;

    private String document;

    private Long documentId;

    private String version;

    private LocalDateTime signedAt;

}
