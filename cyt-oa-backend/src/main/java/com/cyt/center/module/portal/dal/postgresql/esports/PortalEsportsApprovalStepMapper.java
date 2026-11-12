package com.cyt.center.module.portal.dal.postgresql.esports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsApprovalStepDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 电竞预约审批快照仓储（迁自 Portal 的 repositories/esports.py 的 steps_for_bookings）。
 *
 * 按环节和节点顺序批量读快照；动作时沿设置→预约→节点锁序处理。
 */
@Mapper
public interface PortalEsportsApprovalStepMapper extends BaseMapperX<PortalEsportsApprovalStepDO> {

    /** 批量读快照（按 booking_id、stage_index、id 排序）。 */
    default List<PortalEsportsApprovalStepDO> selectByBookingIds(Collection<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<PortalEsportsApprovalStepDO>()
                .in(PortalEsportsApprovalStepDO::getBookingId, bookingIds)
                .orderByAsc(PortalEsportsApprovalStepDO::getBookingId)
                .orderByAsc(PortalEsportsApprovalStepDO::getStageIndex)
                .orderByAsc(PortalEsportsApprovalStepDO::getId));
    }

    /** 持锁批量读快照（写操作第三步，在 settings、booking 锁之后）。 */
    default List<PortalEsportsApprovalStepDO> selectByBookingIdsForUpdate(Collection<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<PortalEsportsApprovalStepDO>()
                .in(PortalEsportsApprovalStepDO::getBookingId, bookingIds)
                .orderByAsc(PortalEsportsApprovalStepDO::getBookingId)
                .orderByAsc(PortalEsportsApprovalStepDO::getStageIndex)
                .orderByAsc(PortalEsportsApprovalStepDO::getId)
                .last("FOR UPDATE"));
    }

}
