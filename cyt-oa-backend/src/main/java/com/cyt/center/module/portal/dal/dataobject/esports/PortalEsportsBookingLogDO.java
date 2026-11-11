package com.cyt.center.module.portal.dal.dataobject.esports;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电竞预约流水（迁自 Portal 的 portal_esports_booking_logs）。
 *
 * 提交 / 审批 / 取消，与主单同事务追加，业务不提供修改或删除入口。
 */
@Data
@TableName("portal_esports_booking_logs")
public class PortalEsportsBookingLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private String operation;

    private Long actorUserId;

    private String actorName;

    private String remark;

    private LocalDateTime createdAt;

}
