package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 大类入口页卡片（迁自 Portal 的 application/training.py employee_groups）。
 *
 * key = 大类 code（slug）；key=null 是「未分类」伪大类兜底（那些没挂到任何在役大类的小类课程）。
 * course_count=0 的大类不返回。
 */
@Schema(description = "门户 - 培训大类卡片 Response VO")
@Data
public class PortalTrainingGroupRespVO {

    @Schema(description = "大类 key（= code）；null = 未分类伪大类")
    private String key;

    @Schema(description = "大类名")
    private String name;

    @Schema(description = "图标（emoji）")
    private String icon;

    @Schema(description = "副标题")
    private String description;

    @Schema(description = "有可见课程的小类数")
    private Integer categoryCount;

    @Schema(description = "可见课程数")
    private Integer courseCount;

    @Schema(description = "有可见课程的小类名（组内按 sort_order 再按名）")
    private List<String> categories;

}
