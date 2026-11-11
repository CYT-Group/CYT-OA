package com.cyt.center.module.portal.dal.mysql.esports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsBookingDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 电竞室预约仓储（迁自 Portal 的 repositories/esports.py 的 list_bookings / overlapping_booking / booking_by_id）。
 *
 * 范围过滤（mine / review / all）是应用层规则（按审批快照判定），本层只提供日期范围 + 状态 + 字面搜索的基础查询，
 * 由 service 拿到步骤快照后再收敛可见性。
 */
@Mapper
public interface PortalEsportsBookingMapper extends BaseMapperX<PortalEsportsBookingDO> {

    /** 主库按 id 读预约。 */
    default PortalEsportsBookingDO selectBookingById(Long id) {
        return selectById(id);
    }

    /** 持锁读预约（写操作第二步，在 settings 锁之后）。 */
    default PortalEsportsBookingDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<PortalEsportsBookingDO>()
                .eq(PortalEsportsBookingDO::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 日期范围 + 状态 + 字面搜索的基础查询（不区分 scope）。
     *
     * q 按字面处理：转义 \ % _ 后 LIKE，MySQL 默认反斜杠转义（对齐 Portal 的 ilike escape="\\"）。
     */
    default List<PortalEsportsBookingDO> selectList(LocalDate dateFrom, LocalDate dateTo, String status, String q) {
        LambdaQueryWrapper<PortalEsportsBookingDO> wrapper = new LambdaQueryWrapper<PortalEsportsBookingDO>()
                .between(PortalEsportsBookingDO::getBookingDate, dateFrom, dateTo);
        if (status != null && !status.isBlank()) {
            wrapper.eq(PortalEsportsBookingDO::getStatus, status);
        }
        if (q != null && !q.isBlank()) {
            String literal = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.and(w -> w.like(PortalEsportsBookingDO::getTitle, literal)
                    .or().like(PortalEsportsBookingDO::getApplicantName, literal));
        }
        wrapper.orderByAsc(PortalEsportsBookingDO::getBookingDate)
                .orderByAsc(PortalEsportsBookingDO::getStartTime)
                .orderByAsc(PortalEsportsBookingDO::getId);
        return selectList(wrapper);
    }

    /**
     * 半开区间重叠检测：pending 和 approved 均占位整间房。
     * 现有 start_time < 新 end_time 且现有 end_time > 新 start_time 即冲突；excludeId 用于审批通过时排除自身。
     */
    default PortalEsportsBookingDO selectOverlapping(LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                                                     Long excludeId) {
        LambdaQueryWrapper<PortalEsportsBookingDO> wrapper = new LambdaQueryWrapper<PortalEsportsBookingDO>()
                .eq(PortalEsportsBookingDO::getBookingDate, bookingDate)
                .in(PortalEsportsBookingDO::getStatus, "pending", "approved")
                .lt(PortalEsportsBookingDO::getStartTime, endTime)
                .gt(PortalEsportsBookingDO::getEndTime, startTime)
                .last("LIMIT 1");
        if (excludeId != null) {
            wrapper.ne(PortalEsportsBookingDO::getId, excludeId);
        }
        return selectOne(wrapper);
    }

}
