package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 课程中心 catalog 里的一个分组（迁自 Portal 的 application/training.py catalog）：
 * category 是小类名，组内课程按 (sort_order, id) 排。
 */
@Schema(description = "门户 - 培训课程中心分组 Response VO")
@Data
public class PortalTrainingCatalogRespVO {

    @Schema(description = "小类名")
    private String category;

    @Schema(description = "该小类下的可见课程")
    private java.util.List<PortalTrainingCatalogCourseVO> courses;

}
