package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 点赞 / 取消点赞的回包：只回最新计数与当前人状态，前端就地更新按钮，不用再拉一次互动数据。
 */
@Schema(description = "公司动态 - 点赞状态 Response VO")
@Data
public class PortalNewsLikeStateRespVO {

    @Schema(description = "点赞总数")
    private Long likeCount;

    @Schema(description = "当前登录人是否已点赞")
    private Boolean liked;

}
