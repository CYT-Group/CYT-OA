package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 公司动态 Response VO（详情：列表行 + 正文）")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalNewsDetailRespVO extends PortalNewsRespVO {

    @Schema(description = "富文本正文")
    private String content;

}
