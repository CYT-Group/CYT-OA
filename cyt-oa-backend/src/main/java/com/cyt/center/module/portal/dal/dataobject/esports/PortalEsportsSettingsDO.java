package com.cyt.center.module.portal.dal.dataobject.esports;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 电竞室开放规则（迁自 Portal 的 portal_esports_settings）。
 *
 * 单行表（id 恒 = 1，CHECK 锁单例）。审批人只影响新单，历史预约持有审批人快照；
 * approver_user_id 是旧单人列，保留为第一环节第一人的投影，新流程以 approval_stages 有序环节数组为准。
 * JSON 列（open_weekdays / approval_stages）走 JacksonTypeHandler，autoResultMap 开启。
 */
@Data
@TableName(value = "portal_esports_settings", autoResultMap = true)
public class PortalEsportsSettingsDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Boolean isOpen;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> openWeekdays;

    private LocalTime openStartTime;

    private LocalTime openEndTime;

    private Integer advanceDays;

    private Integer maxDurationMinutes;

    private Long approverUserId;

    private String approvalMode;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> approvalStages;

    private String updatedBy;

    private LocalDateTime updatedAt;

}
