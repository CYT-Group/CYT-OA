package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端直传预签名回包：浏览器拿到 upload_url 直接 PUT 文件到 NAS，文件不经后端。
 */
@Schema(description = "管理后台 - 培训课时直传预签名 Response VO")
@Data
public class PortalTrainingUploadUrlRespVO {

    @Schema(description = "浏览器直传 PUT 的预签名 URL")
    private String uploadUrl;

    @Schema(description = "生成的对象键（portal/{courseId}/{uuid12}.{ext}）")
    private String objectKey;

    @Schema(description = "签名有效期（秒）", example = "900")
    private Integer expiresIn;

}
