package com.cyt.center.module.portal.dal.postgresql.esports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsBookingLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 电竞预约流水仓储（迁自 Portal 的 repositories/esports.py 的 logs_for_bookings）。
 */
@Mapper
public interface PortalEsportsBookingLogMapper extends BaseMapperX<PortalEsportsBookingLogDO> {

    /** 批量读流水（按 id 排序，避免日历产生逐单查询）。 */
    default List<PortalEsportsBookingLogDO> selectByBookingIds(Collection<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<PortalEsportsBookingLogDO>()
                .in(PortalEsportsBookingLogDO::getBookingId, bookingIds)
                .orderByAsc(PortalEsportsBookingLogDO::getId));
    }

}
