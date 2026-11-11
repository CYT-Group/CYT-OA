package com.cyt.center.module.portal.dal.mysql.events;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.events.PortalEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 展会/活动日历仓储（迁自 Portal 的 repositories/events.py）。
 *
 * 排序口径照 Portal 的 list_events：按开始日期倒序。读侧只回 published，可见性判定留 service。
 */
@Mapper
public interface PortalEventMapper extends BaseMapperX<PortalEventDO> {

    /** 管理端分页列活动：q 对标题 / 地点做模糊匹配，按开始日期倒序。 */
    default PageResult<PortalEventDO> selectPage(PageParam pageParam, String q) {
        LambdaQueryWrapperX<PortalEventDO> wrapper = new LambdaQueryWrapperX<PortalEventDO>();
        if (q != null && !q.isBlank()) {
            wrapper.and(item -> item.like(PortalEventDO::getTitle, q)
                    .or().like(PortalEventDO::getLocation, q));
        }
        wrapper.orderByDesc(PortalEventDO::getStartDate);
        return selectPage(pageParam, wrapper);
    }

    /** 已发布的最近 N 场活动，按开始日期倒序（Portal 读侧 GET /events 的 limit 口径）。 */
    default List<PortalEventDO> selectPublished(int limit) {
        return selectListLimitN(new LambdaQueryWrapperX<PortalEventDO>()
                .eq(PortalEventDO::getStatus, PortalEventDO.STATUS_PUBLISHED)
                .orderByDesc(PortalEventDO::getStartDate), limit);
    }

}
