package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端直传预签名入参（迁自 Portal 的 schemas.TrainingUploadUrlIn）。
 *
 * kind 只认 video|pdf、扩展名与 kind 的匹配在服务层 uploadKey 校验（422），这里只保证课程编号非空。
 */
@Schema(description = "管理后台 - 培训课时直传预签名 Request VO")
@Data
public class PortalTrainingUploadUrlReqVO {

    @Schema(description = "课程编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    @NotNull(message = "课程编号不能为空")
    private Long courseId;

    @Schema(description = "上传文件名（只取扩展名，原名不进对象键）", requiredMode = Schema.RequiredMode.REQUIRED, example = "lesson1.mp4")
    private String filename;

    @Schema(description = "形态：video / pdf", requiredMode = Schema.RequiredMode.REQUIRED, example = "video")
    private String kind;

}
