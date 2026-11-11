package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HR 文档更新请求体（PATCH 语义，None/null = 这次不改；对齐 Portal 的 ArticleUpdate）。
 *
 * blocks 与 contentHtml 互斥（同传 422）；分类/主题直接改正式列（发布前立即按新分类归档）。
 */
@Schema(description = "管理后台 - HR 文档更新 Request VO")
@Data
public class PortalHrDocumentUpdateReqVO {

    @Schema(description = "文档编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18")
    private Long id;

    @Schema(description = "标题（null = 不改）")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "一级分类（null = 不改）")
    private String category;

    @Schema(description = "二级分类（null = 不改）")
    private String topic;

    @Schema(description = "整篇 HTML 草稿（与 blocks 互斥）")
    private String contentHtml;

    @Schema(description = "块格式草稿（与 contentHtml 互斥）")
    private List<Object> blocks;

    @Schema(description = "草稿目标版本号")
    @Size(max = 16, message = "版本号不能超过 16 字")
    private String draftVersion;

    @Schema(description = "是否需要签收")
    private Boolean requiresSignoff;

    @Schema(description = "部门访问限制（空数组 = 全员）")
    private List<String> deptScope;

    @Schema(description = "引言")
    private String intro;

    @Schema(description = "封面图地址")
    @Size(max = 500, message = "封面图不能超过 500 字")
    private String coverImage;

    @Schema(description = "主题")
    @Size(max = 32, message = "主题不能超过 32 字")
    private String theme;

    @Schema(description = "标签数组")
    private List<String> tags;

    @Schema(description = "启用目录")
    private Boolean tocEnabled;

    @Schema(description = "目录位置")
    private String tocPosition;

    @Schema(description = "整屏封面配置（整体替换）")
    private Map<String, Object> cover;

    @Schema(description = "下载 PDF 开关")
    private Boolean downloadPdfEnabled;

    @Schema(description = "签收卡配置（整体替换）")
    private Map<String, Object> signoff;

}
