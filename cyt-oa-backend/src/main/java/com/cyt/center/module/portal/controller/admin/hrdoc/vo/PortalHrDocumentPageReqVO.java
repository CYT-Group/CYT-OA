package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * HR 文档管理端分页查询条件。
 *
 * deleted 切回收站（默认 false 列正常文档）；status 支持伪状态 offline（已下架，用 offline_at 判）。
 */
@Schema(description = "管理后台 - HR 文档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalHrDocumentPageReqVO extends PageParam {

    @Schema(description = "一级分类：handbook/incentive/hr-sop/hr/policy", example = "handbook")
    @Size(max = 32, message = "分类不能超过 32 字")
    private String category;

    @Schema(description = "状态：draft/pending/published/rejected 或伪状态 offline；不传 = 全部", example = "published")
    @Size(max = 20, message = "状态不能超过 20 字")
    private String status;

    @Schema(description = "true = 回收站（只列已逻辑删除）；默认 false", example = "false")
    private Boolean deleted;

    @Schema(description = "关键词，对标题 / 草稿标题做模糊匹配")
    @Size(max = 100, message = "关键词不能超过 100 字")
    private String q;

}
