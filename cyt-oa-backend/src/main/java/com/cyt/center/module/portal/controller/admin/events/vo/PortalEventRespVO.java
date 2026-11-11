package com.cyt.center.module.portal.controller.admin.events.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 展会/活动响应体：管理面与读侧共用一份。
 *
 * 读侧 Portal 的 GET /events 列表不含 status / createdAt，这里多给是给管理面与详情用，
 * 多出来的字段对 Portal 展示端无影响。
 */
@Schema(description = "管理后台 - 展会日历 Response VO")
@Data
public class PortalEventRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "活动介绍")
    private String description;

    @Schema(description = "举办地点")
    private String location;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "活动类型")
    private String eventType;

    @Schema(description = "状态：published")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
