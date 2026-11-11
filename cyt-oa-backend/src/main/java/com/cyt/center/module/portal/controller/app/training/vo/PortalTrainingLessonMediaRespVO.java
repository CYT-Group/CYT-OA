package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 课时媒体预签名回包（对齐 Portal 的 application/training.lesson_media 返回形状）：
 * - video/pdf/html 型 article → {url, expiresIn}（article 带 pdf_key 时另给 pdfUrl）
 * - cards / 图片序列型 article → {pages, expiresIn}
 * - link → {url}
 */
@Schema(description = "门户 - 培训课时媒体预签名 Response VO")
@Data
public class PortalTrainingLessonMediaRespVO {

    @Schema(description = "直读 URL（video / pdf / html 型 article / link）")
    private String url;

    @Schema(description = "html 型 article 附带的 PDF 直读 URL，无则 null")
    private String pdfUrl;

    @Schema(description = "图片直读 URL 数组（cards / 图片序列型 article）")
    private List<String> pages;

    @Schema(description = "签名有效期（秒）", example = "3600")
    private Integer expiresIn;

}
