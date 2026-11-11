package com.cyt.center.module.portal.controller.admin.hrdoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * HR 文档新建请求体（ID-first，不接收 slug；对齐 Portal 的 ArticleCreate）。
 */
@Schema(description = "管理后台 - HR 文档新建 Request VO")
@Data
public class PortalHrDocumentSaveReqVO {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "员工手册")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "一级分类：handbook/incentive/hr-sop/hr/policy", requiredMode = Schema.RequiredMode.REQUIRED, example = "handbook")
    @NotBlank(message = "分类不能为空")
    private String category;

    @Schema(description = "二级分类（制度中心 6 主题）；不传 = admin", example = "admin")
    private String topic = "admin";

    @Schema(description = "正文 HTML（与 blocks 二选一）", example = "<p>正文</p>")
    private String contentHtml = "";

    @Schema(description = "版本号", example = "v1.0")
    @Size(max = 16, message = "版本号不能超过 16 字")
    private String version = "v1.0";

    @Schema(description = "是否需要签收", example = "false")
    private Boolean requiresSignoff = false;

    @Schema(description = "部门访问限制（部门名数组）；null/空 = 全员", example = "[\"行政人事部\"]")
    private List<String> deptScope;

    @Schema(description = "引言（纯文本 ≤500 字）")
    private String intro = "";

    @Schema(description = "封面图地址（https:// 或 / 开头）；空 = 主题渐变")
    @Size(max = 500, message = "封面图不能超过 500 字")
    private String coverImage = "";

    @Schema(description = "主题：'' / violet|blue|orange|green|slate|rose / #RRGGBB")
    @Size(max = 32, message = "主题不能超过 32 字")
    private String theme = "";

    @Schema(description = "标签数组")
    private List<String> tags;

    @Schema(description = "启用目录", example = "true")
    private Boolean tocEnabled = true;

    @Schema(description = "目录位置：left/right", example = "left")
    private String tocPosition = "left";

    @Schema(description = "整屏封面配置（10 键整体）")
    private Map<String, Object> cover;

    @Schema(description = "下载 PDF 开关", example = "false")
    private Boolean downloadPdfEnabled = false;

    @Schema(description = "签收卡配置")
    private Map<String, Object> signoff;

}
