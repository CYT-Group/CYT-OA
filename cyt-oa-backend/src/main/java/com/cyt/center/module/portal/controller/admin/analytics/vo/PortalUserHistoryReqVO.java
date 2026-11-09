package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 查某个人的浏览轨迹。
 *
 * username 是企微 userid 明文串，不是 sys_user 编号：埋点行按名存快照，
 * 账号注销后历史仍然查得到（口径 docs/Portal管理面迁移契约.md §一「身份与权限」）。
 */
@Schema(description = "管理后台 - 门户用户浏览轨迹 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalUserHistoryReqVO extends PortalAnalyticsRangeReqVO {

    @Schema(description = "账号（企微 userid）", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhangsan")
    @NotBlank(message = "username 不能为空")
    @Size(max = 50, message = "username 不能超过 50 字")
    private String username;

    @Schema(description = "取前 N 条，1..200", example = "50")
    @NotNull(message = "limit 不能为空")
    @Min(value = 1, message = "limit 不能小于 1")
    @Max(value = 200, message = "limit 不能大于 200")
    private Integer limit = 50;

}
