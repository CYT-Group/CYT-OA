package com.cyt.center.module.portal.controller.admin.news.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 动态审批通过请求体。Portal 的 ApproveNewsRequest 同款：审核人可以覆盖作者在提交时留下的推送意图。
 *
 * notifyWecom 三态是有意为之：null = 跟随作者意图，true = 强制推，false = 强制不推（即便作者勾过）。
 * 字段名用 notify 而不是 pending_notify，是为了和 {@code portal_news.pending_notify_*} 两列
 * 的「已持久化待消费」区分开——这里传的是本次审批的即时决定。
 */
@Schema(description = "管理后台 - 公司动态审批通过 Request VO")
@Data
public class PortalNewsApproveReqVO {

    @Schema(description = "动态编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "审批意见", example = "内容合规，可发")
    @Size(max = 500, message = "审批意见不能超过 500 字")
    private String comment;

    @Schema(description = "推送企微：null = 跟随作者提交的意图，true = 强制推，false = 强制不推")
    private Boolean notifyWecom;

    @Schema(description = "推送部门范围，空 = 全员")
    private List<String> notifyDeptScope;

}
