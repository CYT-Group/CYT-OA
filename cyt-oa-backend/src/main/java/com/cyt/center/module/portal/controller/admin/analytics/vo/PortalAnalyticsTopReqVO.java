package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 看板排行类查询：时间窗 + Top N。
 *
 * limit 上限取 100，与 Portal 的 popular-pages / active-users 一致；
 * 浏览明细那条上限更宽（200），所以它另用一个 VO，不在这儿放宽。
 */
@Schema(description = "管理后台 - 门户浏览看板排行 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalAnalyticsTopReqVO extends PortalAnalyticsRangeReqVO {

    @Schema(description = "取前 N 条，1..100", example = "20")
    @NotNull(message = "limit 不能为空")
    @Min(value = 1, message = "limit 不能小于 1")
    @Max(value = 100, message = "limit 不能大于 100")
    private Integer limit = 20;

}
