package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "公司动态 - 评论 Response VO")
@Data
public class PortalNewsCommentRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "评论人 username（企微 userid）")
    private String username;

    @Schema(description = "评论人姓名快照")
    private String displayName;

    @Schema(description = "评论人头像快照")
    private String avatarUrl;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "被回复的评论编号；null = 顶层")
    private Long parentId;

    @Schema(description = "评论时间")
    private LocalDateTime createdAt;

    @Schema(description = "当前登录人能不能删这条：作者本人或持评论治理能力。前端据此显隐删除按钮")
    private Boolean canDelete;

}
