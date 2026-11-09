package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 动态列表行：不含正文。
 *
 * 列表刻意不返回 content（MEDIUMTEXT 富文本，一页 20 条能把响应撑到几百 KB），
 * 详情接口才带。Portal 的列表 wire 同样省略正文。
 */
@Schema(description = "管理后台 - 公司动态 Response VO（列表行）")
@Data
public class PortalNewsRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "文本分类")
    private String category;

    @Schema(description = "封面图地址")
    private String coverImage;

    @Schema(description = "稿型")
    private String templateType;

    @Schema(description = "模板数据块")
    private Map<String, Object> blocks;

    @Schema(description = "作者展示名快照")
    private String author;

    @Schema(description = "作者 username（企微 userid）")
    private String authorUsername;

    @Schema(description = "状态：draft / pending / published / rejected")
    private String status;

    @Schema(description = "精选（前端兼作置顶）")
    private Boolean isFeatured;

    @Schema(description = "发布时间的快照，未发布为 null")
    private LocalDateTime publishedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
