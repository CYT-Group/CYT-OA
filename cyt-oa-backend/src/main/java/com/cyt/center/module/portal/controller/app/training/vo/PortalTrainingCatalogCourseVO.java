package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * catalog 里的一门课（迁自 Portal 的 application/training.py catalog 的分组行）。
 */
@Schema(description = "门户 - 培训课程中心课程行 Response VO")
@Data
public class PortalTrainingCatalogCourseVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "封面图地址")
    private String coverImage;

    @Schema(description = "级别")
    private String level;

    @Schema(description = "讲师")
    private String instructor;

    @Schema(description = "课时数")
    private Integer lessonCount;

    @Schema(description = "视频总时长（秒）")
    private Long durationTotalSec;

    @Schema(description = "本人进度百分比（0-100）")
    private Integer progressPercent;

    @Schema(description = "是否已完成")
    private Boolean completed;

}
