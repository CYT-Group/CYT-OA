package com.cyt.center.module.portal.controller.admin.products.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 新品动态响应体：管理面与读侧共用一份。
 *
 * 读侧 Portal 的 GET /products 列表不含 status / isFeatured / createdAt，这里多给是给管理面
 * 与详情用，多出来的字段对 Portal 展示端无影响。
 */
@Schema(description = "管理后台 - 新品动态 Response VO")
@Data
public class PortalProductRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "介绍")
    private String description;

    @Schema(description = "图片地址")
    private String imageUrl;

    @Schema(description = "详情链接")
    private String linkUrl;

    @Schema(description = "发布日期")
    private LocalDate launchDate;

    @Schema(description = "精选")
    private Boolean isFeatured;

    @Schema(description = "状态：published")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
