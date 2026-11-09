package com.cyt.center.module.portal.service.wiki;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiPageReqVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO;

/**
 * 知识库用例：状态机、可见性、浏览计数与审批审计。
 *
 * 逐条对齐 Portal 的 application/wiki.py（那边层划分同 news，SQL 只在 dal/mysql/wiki）。
 * 与公司动态同一套状态机，差别两处：没有 published_at 列，也没有推送意图。
 */
public interface PortalWikiService {

    /**
     * 按状态可见性规则分页列文档。
     *
     * published 全员可见；其余状态审阅者看全量、普通用户只看自己写的（草稿/待审互不可见）。
     */
    PageResult<PortalWikiDO> page(Long userId, PortalWikiPageReqVO pageReqVO);

    /**
     * 取单篇文档详情：套用可见性规则并自增浏览量。
     *
     * 不可见与不存在回同一个错（Portal 的 404 不 403，防探测）。
     */
    PortalWikiDO getForRead(Long userId, Long id);

    /**
     * 新建文档：显式 draft 存草稿；审阅者默认 published；其余角色默认 pending 走审批。
     *
     * 状态只有这一条派发路径——请求体里传 published 不会绕过审批，所以入参 status 只认 draft。
     */
    PortalWikiDO create(Long userId, PortalWikiSaveReqVO createReqVO);

    /** 全量覆盖式改文档：作者本人或有全量编辑权者可改；非全量编辑权者改已发布文档退回 pending 重走审批 */
    PortalWikiDO update(Long userId, Long id, PortalWikiSaveReqVO updateReqVO);

    /** 审批：approve → published / reject → rejected，动作进操作日志（Portal 的 portal_approvals 流水已下线） */
    void review(Long userId, Long id, boolean approve, String comment);

    /** 硬删一篇文档，随后记审计 */
    void delete(Long userId, Long id);

}
