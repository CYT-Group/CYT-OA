package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 培训整体统计回包（对齐 Portal 的 repositories/training.training_stats 返回键）。
 */
@Schema(description = "管理后台 - 培训统计 Response VO")
@Data
public class PortalTrainingStatsRespVO {

    @Schema(description = "已发布课程数")
    private Long totalCourses;

    @Schema(description = "开始学习人数（按 username 去重）")
    private Long usersStarted;

    @Schema(description = "完成次数（status=completed 的进度条数）")
    private Long completions;

}
