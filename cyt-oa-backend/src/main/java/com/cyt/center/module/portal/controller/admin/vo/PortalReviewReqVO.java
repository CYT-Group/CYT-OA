package com.cyt.center.module.portal.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审批意见请求体：动作（approve / reject）已在路径里，body 带编号和一句说明。
 *
 * 动态驳回、知识库审批共用——Portal 那边也是同一个 ApprovalComment。
 */
@Schema(description = "管理后台 - 审批意见 Request VO")
@Data
public class PortalReviewReqVO {

    @Schema(description = "被审批内容的编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "审批意见，会进操作日志的 comment 列", example = "配图涉未发布产品，替换后再提")
    @Size(max = 500, message = "审批意见不能超过 500 字")
    private String comment;

}
