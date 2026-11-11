package com.cyt.center.module.portal.controller.admin.products.vo;

import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 新品动态分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalProductPageReqVO extends PageParam {

    @Schema(description = "品牌精确匹配", example = "品牌A")
    @Size(max = 50, message = "品牌不能超过 50 字")
    private String brand;

    @Schema(description = "关键词，对名称 / 介绍做模糊匹配")
    @Size(max = 100, message = "关键词不能超过 100 字")
    private String q;

}
