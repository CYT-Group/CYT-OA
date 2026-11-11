package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课时进度心跳回包（迁自 Portal 的 application/training.py report_progress）：
 * course_completed 表示本次心跳后该课程是否整体完成。
 */
@Schema(description = "门户 - 培训课时进度心跳 Response VO")
@Data
public class PortalTrainingProgressRespVO {

    @Schema(description = "课时编号")
    private Long lessonId;

    @Schema(description = "状态：started / completed")
    private String status;

    @Schema(description = "断点位置")
    private Integer position;

    @Schema(description = "进度百分比")
    private Integer percent;

    @Schema(description = "已回报时长（秒）")
    private Integer durationSec;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "课程是否整体完成")
    private Boolean courseCompleted;

}
