package com.cyt.center.module.portal.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 通用分类同层拖拽排序 Request VO")
@Data
public class PortalCategoryReorderReqVO {

    @Schema(description = "业务归属 scope", requiredMode = Schema.RequiredMode.REQUIRED, example = "training.course")
    @NotEmpty(message = "scope 不能为空")
    @Size(max = 32, message = "scope 不能超过 32 字")
    private String scope;

    @Schema(description = "父分类编号；null = 该 scope 的顶层（大类或未归类小类）")
    private Long parentId;

    @Schema(description = "按新顺序排列的分类编号，写入 sortOrder = 1..n", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "orderedIds 不能为空")
    private List<Long> orderedIds;

}
