package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 动态新建 / 更新请求体（同一个 VO，PUT 是全量覆盖，与 Portal 的 NewsCreate/NewsUpdate 同构）。
 *
 * 刻意不含 status 与作者：状态只由审批流推进（reviewer 建 = published，其余 = pending），
 * 作者取当前登录人，两者都不接受客户端指定——否则任何人都能 PUT 出一条 published。
 */
@Schema(description = "管理后台 - 公司动态新建/更新 Request VO")
@Data
public class PortalNewsSaveReqVO {

    @Schema(description = "动态编号；只在更新时带，新建忽略", example = "18")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "春季新品发布会落幕")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "摘要，列表页展示；留空时前端回退正文首段")
    private String summary;

    @Schema(description = "富文本正文，模板化稿件可空（内容在 blocks 里）")
    private String content;

    @Schema(description = "文本分类，与 portal_categories 无关", example = "company")
    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    @Schema(description = "封面图地址")
    @Size(max = 255, message = "封面地址不能超过 255 字")
    private String coverImage;

    @Schema(description = "稿型，默认 simple", example = "simple")
    @Size(max = 32, message = "稿型不能超过 32 字")
    private String templateType;

    @Schema(description = "模板数据块，前端会读 blocks.dept 等字段")
    private Map<String, Object> blocks;

    @Schema(description = "精选（前端兼作置顶）")
    private Boolean isFeatured;

    @Schema(description = "发布时推送企微；只有稿子真进 published 才生效，pending 稿存成待确认意图")
    private Boolean notifyWecom;

    @Schema(description = "推送部门范围，空 = 全员")
    private List<String> notifyDeptScope;

}
