package com.cyt.center.module.portal.dal.dataobject.events;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 展会/活动日历（迁自 Portal 的 portal_events）。
 *
 * 无审批流：Portal 侧创建即 published，status 只有 published 一个取值（默认值），
 * 读侧按 published 过滤、未发布/不存在统一 404。没有作者列——Portal 的 events 不记创建人，
 * 写操作由 @SaCheckPermission(can_manage_events) 整端点闸。
 *
 * ALWAYS 的列是 PUT 全量覆盖里能显式清空的那批：description / location / end_date 都能清成 NULL。
 * start_date 是 NOT NULL，新建缺省取今天、更新传空保留原值，所以不标 ALWAYS。
 */
@Data
@TableName(value = "portal_events", autoResultMap = true)
public class PortalEventDO {

    public static final String STATUS_PUBLISHED = "published";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String location;

    private LocalDate startDate;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate endDate;

    private String eventType;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
