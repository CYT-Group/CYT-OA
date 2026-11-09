package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 提醒偏好。主键即用户编号，一个员工一行，四类开关各对应 notifications.category。
 */
@Data
@TableName("notify_preferences")
public class NotifyPreference {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private Boolean reportDue;

    private Boolean overdue;

    private Boolean milestone;

    private Boolean corpNews;

}
