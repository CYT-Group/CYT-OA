package com.cyt.center.module.portal.controller.admin.products.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新品动态新建 / 更新请求体（PUT 全量覆盖，与 Portal 的 ProductCreate/ProductUpdate 同构）。
 *
 * 刻意不含 status：Portal 无审批流，创建即 published，状态不接受客户端指定。
 */
@Schema(description = "管理后台 - 新品动态新建/更新 Request VO")
@Data
public class PortalProductSaveReqVO {

    @Schema(description = "新品编号；只在更新时带，新建忽略", example = "2")
    private Long id;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "新品 X")
    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称不能超过 200 字")
    private String name;

    @Schema(description = "品牌")
    @Size(max = 50, message = "品牌不能超过 50 字")
    private String brand;

    @Schema(description = "分类")
    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    @Schema(description = "介绍")
    private String description;

    @Schema(description = "图片地址")
    @Size(max = 500, message = "图片地址不能超过 500 字")
    private String imageUrl;

    @Schema(description = "详情链接")
    @Size(max = 500, message = "详情链接不能超过 500 字")
    private String linkUrl;

    @Schema(description = "发布日期，可留空")
    private LocalDate launchDate;

    @Schema(description = "精选（前端兼作置顶展示）")
    private Boolean isFeatured;

}
