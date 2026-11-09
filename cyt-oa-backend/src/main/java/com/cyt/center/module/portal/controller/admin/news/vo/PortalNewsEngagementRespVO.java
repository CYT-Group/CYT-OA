package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 详情页一次拉走的互动数据（Portal 把点赞数 / 我点过没 / 评论列表合并成一个请求，少一次往返）。
 *
 * liked 在 Portal 的 wire 里叫 i_liked，B9 让 Portal 改读 OA 时在它那边补别名。
 */
@Schema(description = "公司动态 - 互动数据 Response VO")
@Data
public class PortalNewsEngagementRespVO {

    @Schema(description = "点赞总数")
    private Long likeCount;

    @Schema(description = "当前登录人是否已点赞")
    private Boolean liked;

    @Schema(description = "未删除的评论数")
    private Integer commentCount;

    @Schema(description = "未删除的评论，时间正序；楼中楼靠 parentId 由前端组装")
    private List<PortalNewsCommentRespVO> comments;

}
