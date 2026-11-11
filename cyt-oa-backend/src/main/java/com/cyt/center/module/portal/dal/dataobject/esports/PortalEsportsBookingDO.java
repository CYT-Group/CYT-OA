package com.cyt.center.module.portal.dal.dataobject.esports;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 电竞室独占预约单（迁自 Portal 的 portal_esports_bookings）。
 *
 * pending 和 approved 均占位整间房。participants 为 NULL 表示旧单只记人数、名单未知。
 * ALWAYS 的列是流程里能显式清空/回写的：participants（新单可无名单）、current_stage_index（流程结束置 NULL）、
 * review_remark / reviewed_at / cancelled_at（审批/取消后回写或保持 NULL）。
 */
@Data
@TableName(value = "portal_esports_bookings", autoResultMap = true)
public class PortalEsportsBookingDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate bookingDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String title;

    private String purpose;

    private Integer participantCount;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Map<String, Object>> participants;

    private Long applicantUserId;

    private String applicantName;

    private Long approverUserId;

    private String approverName;

    private String approvalMode;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer currentStageIndex;

    private String status;

    private Long reviewedByUserId;

    private String reviewedByName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reviewRemark;

    private LocalDateTime reviewedAt;

    private Long cancelledByUserId;

    private LocalDateTime cancelledAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
