package com.cyt.center.module.portal.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 知识库文档新建 / 更新请求体（PUT 全量覆盖，与 Portal 的 WikiCreate/WikiUpdate 同构）。
 *
 * status 只接受 draft：其余状态一律由角色与审批流决定，否则任何人都能 POST 出一篇 published。
 * 部门字段只用 department——Portal 前端历史上发过 dept，那层别名留在它自己那边适配。
 */
@Schema(description = "管理后台 - 知识库文档新建/更新 Request VO")
@Data
public class PortalWikiSaveReqVO {

    @Schema(description = "文档编号；只在更新时带，新建忽略", example = "3")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "报销流程说明")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "正文；只给外链时可以为空")
    private String content;

    @Schema(description = "文本分类", example = "finance")
    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    @Schema(description = "归属部门名（可见范围口径），不是 portal_categories 的行")
    @Size(max = 50, message = "部门不能超过 50 字")
    private String department;

    @Schema(description = "外链文档地址")
    @Size(max = 500, message = "文档地址不能超过 500 字")
    private String docUrl;

    @Schema(description = "标签数组")
    private List<String> tags;

    @Schema(description = "状态：只有 draft 生效（存草稿）；不传或传别的值都按角色派发", example = "draft")
    @Size(max = 20, message = "状态不能超过 20 字")
    private String status;

}
