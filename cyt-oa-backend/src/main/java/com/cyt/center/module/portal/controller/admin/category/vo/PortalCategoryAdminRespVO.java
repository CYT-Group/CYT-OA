package com.cyt.center.module.portal.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 通用分类 Response VO（管理行：公共行 + 归档信息 + 计数）")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalCategoryAdminRespVO extends PortalCategoryRespVO {

    @Schema(description = "归档时间；null = 在役")
    private LocalDateTime archivedAt;

    @Schema(description = "归档人 username")
    private String archivedBy;

    @Schema(description = "创建人 username")
    private String createdBy;

    @Schema(description = "被业务表引用的数量")
    private Long refCount;

    @Schema(description = "在役子分类数")
    private Long childrenCount;

}
