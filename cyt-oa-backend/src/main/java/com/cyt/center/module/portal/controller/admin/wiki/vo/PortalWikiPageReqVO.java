package com.cyt.center.module.portal.controller.admin.wiki.vo;

import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 知识库文档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalWikiPageReqVO extends PageParam {

    @Schema(description = "状态：draft / pending / published / rejected，all = 不限（控制台用）", example = "published")
    @Size(max = 20, message = "状态不能超过 20 字")
    private String status = "published";

    @Schema(description = "文本分类精确匹配", example = "finance")
    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    @Schema(description = "归属部门精确匹配", example = "行政人事部")
    @Size(max = 50, message = "部门不能超过 50 字")
    private String department;

    @Schema(description = "关键词，对标题与正文做模糊匹配")
    @Size(max = 100, message = "关键词不能超过 100 字")
    private String q;

}
