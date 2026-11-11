package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程详情 / 老列表行（迁自 Portal 的 routers/training.py _training_row 超集）：
 * 详情带 lesson_count 与 lessons；老列表（GET /page）不带 lessons。
 */
@Schema(description = "门户 - 培训课程详情/列表 Response VO")
@Data
public class PortalTrainingCourseRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "老列：归属部门（不再写）")
    private String department;

    @Schema(description = "级别")
    private String level;

    @Schema(description = "时长（分钟）")
    private Integer durationMinutes;

    @Schema(description = "讲师")
    private String instructor;

    @Schema(description = "老列：文档外链（不再写）")
    private String docUrl;

    @Schema(description = "老列：视频外链（不再写）")
    private String videoUrl;

    @Schema(description = "封面图地址")
    private String coverImage;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "小类名")
    private String category;

    @Schema(description = "可见部门名")
    private List<String> deptScope;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "导入幂等键")
    private String sourcePrefix;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "课时数（详情返回；列表为 null）")
    private Integer lessonCount;

    @Schema(description = "课时列表（详情返回；列表为 null）")
    private List<PortalTrainingLessonRespVO> lessons;

}
