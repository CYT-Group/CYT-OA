package com.cyt.center.module.portal.controller.admin.wiki.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库详情：列表行 + 正文。
 *
 * 取详情会自增浏览量（Portal 口径：无去重、无频控），所以只在点开时调用。
 */
@Schema(description = "管理后台 - 知识库文档 Response VO（详情，含正文）")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalWikiDetailRespVO extends PortalWikiRespVO {

    @Schema(description = "正文")
    private String content;

}
