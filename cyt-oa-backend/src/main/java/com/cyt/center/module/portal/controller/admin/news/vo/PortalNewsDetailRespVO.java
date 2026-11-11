package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 公司动态 Response VO（详情：列表行 + 正文）")
@Data
@EqualsAndHashCode(callSuper = true)
public class PortalNewsDetailRespVO extends PortalNewsRespVO {
    @Schema(description = "作者提交的待审核企微推送意图")
    private Boolean pendingNotifyWecom;

    @Schema(description = "作者选择的推送部门范围；null或空为全员")
    private java.util.List<String> pendingNotifyDeptScope;

    @Schema(description = "富文本正文")
    private String content;

}
