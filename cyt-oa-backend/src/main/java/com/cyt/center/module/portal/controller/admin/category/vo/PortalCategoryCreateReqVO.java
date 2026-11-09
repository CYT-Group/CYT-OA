package com.cyt.center.module.portal.controller.admin.category.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - 通用分类新建 Request VO")
@Data
public class PortalCategoryCreateReqVO {

    @Schema(description = "业务归属 scope", requiredMode = Schema.RequiredMode.REQUIRED, example = "material.sample")
    @NotBlank(message = "scope 不能为空")
    @Size(max = 32, message = "scope 不能超过 32 字")
    private String scope;

    // name 上界 100 是列宽；物资 scope 另由注册表收到 32，所以这里放开、由领域层再判
    @Schema(description = "分类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "笔记本电脑")
    @NotBlank(message = "分类名不能为空")
    @Size(max = 100, message = "分类名不能超过 100 字")
    private String name;

    @Schema(description = "程序锚点 code（培训大类必填且建后不可改）", example = "tech")
    @Size(max = 32, message = "code 不能超过 32 字")
    private String code;

    @Schema(description = "父分类编号；平铺 scope 不允许传")
    private Long parentId;

    // 不传 = 该层当前最大值 + 1（排末尾）；上界防 int 溢出
    @Schema(description = "同层展示序", example = "1")
    @Min(value = 0, message = "sortOrder 不能小于 0")
    @Max(value = 1_000_000_000, message = "sortOrder 超出允许范围")
    private Integer sortOrder;

    @Schema(description = "各业务自己的开关与展示字段，键名按 scope 白名单校验")
    private Map<String, Object> attrs;

}
