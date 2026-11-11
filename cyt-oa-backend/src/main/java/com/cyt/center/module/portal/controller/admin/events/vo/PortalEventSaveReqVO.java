package com.cyt.center.module.portal.controller.admin.events.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 展会/活动新建 / 更新请求体（PUT 全量覆盖，与 Portal 的 EventCreate/EventUpdate 同构）。
 *
 * 刻意不含 status：Portal 无审批流，创建即 published，状态不接受客户端指定。
 */
@Schema(description = "管理后台 - 展会日历新建/更新 Request VO")
@Data
public class PortalEventSaveReqVO {

    @Schema(description = "活动编号；只在更新时带，新建忽略", example = "8")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "春季展会")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "活动介绍")
    private String description;

    @Schema(description = "举办地点")
    @Size(max = 200, message = "地点不能超过 200 字")
    private String location;

    @Schema(description = "开始日期，新建缺省取今天")
    private LocalDate startDate;

    @Schema(description = "结束日期，可留空")
    private LocalDate endDate;

    @Schema(description = "活动类型，默认 exhibition", example = "exhibition")
    @Size(max = 50, message = "活动类型不能超过 50 字")
    private String eventType;

}
