package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端课程行（列表 / 详情 / 新建 / 更新回包共用）。
 *
 * kindCounts 五键恒在（0 也给），顺序 video/pdf/article/cards/link——前端按它画形式徽标。
 */
@Schema(description = "管理后台 - 培训课程 Response VO")
@Data
public class PortalTrainingRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "级别：required / elective")
    private String level;

    @Schema(description = "时长（分钟）")
    private Integer durationMinutes;

    @Schema(description = "讲师")
    private String instructor;

    @Schema(description = "封面图地址")
    private String coverImage;

    @Schema(description = "状态：draft / published")
    private String status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "小类名（文本列镜像）")
    private String category;

    @Schema(description = "小类编号（事实源）")
    private Long categoryId;

    @Schema(description = "可见部门名，null = 全员")
    private List<String> deptScope;

    @Schema(description = "导入幂等键")
    private String sourcePrefix;

    @Schema(description = "创建人 username")
    private String authorUsername;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "课时数", example = "3")
    private Integer lessonCount;

    @Schema(description = "各形态课时数（五键恒在）")
    private Map<String, Integer> kindCounts;

}
