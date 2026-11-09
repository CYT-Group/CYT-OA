package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "公司动态 - 发表评论 Request VO")
@Data
public class PortalNewsCommentReqVO {

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "发布会的样品我想看")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过 1000 字")
    private String content;

    @Schema(description = "被回复的评论编号；不传 = 顶层评论", example = "1")
    private Long parentId;

}
