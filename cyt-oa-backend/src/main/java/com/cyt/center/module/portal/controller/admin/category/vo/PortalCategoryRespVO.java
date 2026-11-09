package com.cyt.center.module.portal.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 通用分类 Response VO（公共行）")
@Data
public class PortalCategoryRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "业务归属 scope", example = "material.sample")
    private String scope;

    @Schema(description = "分类名")
    private String name;

    @Schema(description = "程序锚点 code")
    private String code;

    @Schema(description = "父分类编号")
    private Long parentId;

    @Schema(description = "1 = 顶层，2 = 子级")
    private Integer depth;

    @Schema(description = "同层展示序")
    private Integer sortOrder;

    // attrs 恒为对象（列 NOT NULL default {}），前端不用防 null
    @Schema(description = "各业务自己的开关与展示字段")
    private Map<String, Object> attrs;

}
