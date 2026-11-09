package com.cyt.center.module.portal.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库列表行：不含正文。
 *
 * Portal 的列表 wire 没有 status，这里加上是管理面要的——审批队列要在行上判状态，
 * 否则每行都得再打一次详情。多出来的字段对 Portal 展示端无影响（B9 忽略即可）。
 */
@Schema(description = "管理后台 - 知识库文档 Response VO（列表行）")
@Data
public class PortalWikiRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "文本分类")
    private String category;

    @Schema(description = "归属部门名")
    private String department;

    @Schema(description = "标签数组")
    private List<String> tags;

    @Schema(description = "外链文档地址")
    private String docUrl;

    @Schema(description = "作者 username（企微 userid）")
    private String authorUsername;

    @Schema(description = "状态：draft / pending / published / rejected")
    private String status;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
