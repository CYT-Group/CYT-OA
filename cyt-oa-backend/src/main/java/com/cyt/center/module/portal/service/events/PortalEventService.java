package com.cyt.center.module.portal.service.events;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventPageReqVO;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.events.PortalEventDO;

import java.util.List;

/**
 * 展会/活动日历用例（逐条对齐 Portal 的 application/events.py）。
 *
 * 无审批流：创建即 published；SQL 只在 dal/mysql/events，本层只串规则与事务。
 */
public interface PortalEventService {

    /** 管理端分页列活动（含全部状态，管理面不分状态） */
    PageResult<PortalEventDO> page(PortalEventPageReqVO pageReqVO);

    /** 管理端取单场活动（不看状态，编辑表单用），不存在 404 */
    PortalEventDO get(Long id);

    /** 新建活动：start_date 缺省取今天，status 恒 published */
    PortalEventDO create(PortalEventSaveReqVO createReqVO);

    /** 全量覆盖改活动：start_date 传空保留原值，可清空 description / location / end_date */
    PortalEventDO update(Long id, PortalEventSaveReqVO updateReqVO);

    /** 删除活动，随后记审计（Portal 只有删除留审计） */
    void delete(Long userId, Long id);

    /** 读侧：已发布的最近 N 场，按开始日期倒序（Portal 的 GET /events） */
    List<PortalEventDO> listPublished(Integer limit);

    /** 读侧：单场详情；不存在或未发布统一 404 */
    PortalEventDO getPublished(Long id);

}
