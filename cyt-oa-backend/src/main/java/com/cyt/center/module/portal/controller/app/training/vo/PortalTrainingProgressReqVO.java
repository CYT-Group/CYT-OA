package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 课时进度心跳入参（迁自 Portal 的 schemas.TrainingLessonProgressIn）：
 * percent 必填，0-100（只增不减在服务层）；position 是断点（视频秒 / PDF 页 / 卡片索引）。
 */
@Schema(description = "门户 - 培训课时进度心跳 Request VO")
@Data
public class PortalTrainingProgressReqVO {

    @Schema(description = "进度百分比（0-100）", requiredMode = Schema.RequiredMode.REQUIRED, example = "90")
    @NotNull(message = "percent 不能为空")
    private Integer percent;

    @Schema(description = "断点位置（视频秒 / PDF 页 / 卡片索引），允许回退", example = "120")
    private Integer position;

    @Schema(description = "媒体总时长（秒）", example = "600")
    private Integer durationSec;

}
