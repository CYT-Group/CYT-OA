package com.cyt.center.module.portal.service.news;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.exception.ErrorCode;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsApproveReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsPageReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO;

/**
 * 公司动态用例：状态机、可见性、审批与审计在这层（口径 docs/Portal管理面迁移契约.md §一「内容管理」）。
 *
 * 逐条对齐 Portal 的 application/news.py；层划分照那边——SQL 只在 dal/mysql/news，
 * 本层只串规则与事务。
 */
public interface PortalNewsService {

    /**
     * 按状态可见性规则分页列动态。
     *
     * published 全员可见；其余状态审阅者看全量、普通用户只看自己写的那几条。
     */
    PageResult<PortalNewsDO> page(Long userId, PortalNewsPageReqVO pageReqVO);

    /**
     * 取单条动态并套用可见性规则：非 published 只有审阅者或作者本人读得到，其余按「不存在」处理。
     *
     * 不可见与不存在回同一个错，是不给探测留口径（Portal 的 404 不 403）；两边文案各模块不同，
     * 所以把 ErrorCode 交给调用方——详情说「新闻不存在」，互动说「动态不存在」。
     */
    PortalNewsDO requireVisible(Long userId, Long id, ErrorCode notFound);

    /**
     * 新建动态：有全量可见能力的角色直接 published，其余进 pending 走审批；
     * 未发布又勾了推送的，把意图存进 pending_notify_* 等审核人消费。
     */
    PortalNewsDO create(Long userId, PortalNewsSaveReqVO createReqVO);

    /** 全量覆盖式改稿：作者本人或有全量编辑权者可改；非全量编辑权者改已发布稿会退回 pending 重走审批 */
    PortalNewsDO update(Long userId, Long id, PortalNewsSaveReqVO updateReqVO);

    /** 硬删一条动态，随后记审计（Portal 那边内容行只有删除留审计，编辑史不在审计里） */
    void delete(Long userId, Long id);

    /** 审批通过：置 published 并一次性消费作者的推送意图，审批流水进操作日志 */
    void approve(Long userId, Long id, PortalNewsApproveReqVO approveReqVO);

    /** 审批驳回：置 rejected 并清掉推送意图，要重推得作者重新勾 */
    void reject(Long userId, Long id, String comment);

}
