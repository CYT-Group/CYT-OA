package com.cyt.center.module.portal.dal.postgresql.news;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公司动态仓储。
 *
 * 可见性（谁能看到非 published 的稿）是业务规则，由 service 决定后把 authorUsername
 * 当参数传进来，本层只负责把它拼成 where 条件。
 */
@Mapper
public interface PortalNewsMapper extends BaseMapperX<PortalNewsDO> {

    /**
     * 分页列动态：q 对标题/正文/摘要做模糊匹配，按创建时间倒序。
     *
     * status / visibleTo 都在 where 上写死：非 published 的行只有作者和审阅者看得到，
     * 可见性判定留在 service，这一层只负责把结论拼成条件。
     *
     * @param status    null = 不限状态（审阅者的「全部」）
     * @param visibleTo 非 null 时 = 「这个人写的 或 已发布的」——普通人的「全部」靠它收敛，不会翻出别人的草稿
     */
    default PageResult<PortalNewsDO> selectPage(PageParam pageParam, String status, String category,
                                                String q, String visibleTo) {
        LambdaQueryWrapperX<PortalNewsDO> wrapper = new LambdaQueryWrapperX<PortalNewsDO>()
                .eqIfPresent(PortalNewsDO::getCategory, category);
        if (visibleTo != null) {
            if (status == null) {
                wrapper.and(item -> item.eq(PortalNewsDO::getStatus, PortalNewsDO.STATUS_PUBLISHED)
                        .or().eq(PortalNewsDO::getAuthorUsername, visibleTo));
            } else {
                wrapper.and(item -> item.eq(PortalNewsDO::getStatus, status)
                        .or().eq(PortalNewsDO::getAuthorUsername, visibleTo));
            }
        } else {
            wrapper.eqIfPresent(PortalNewsDO::getStatus, status);
        }
        if (q != null && !q.isBlank()) {
            wrapper.and(item -> item.like(PortalNewsDO::getTitle, q)
                    .or().like(PortalNewsDO::getContent, q)
                    .or().like(PortalNewsDO::getSummary, q));
        }
        wrapper.orderByDesc(PortalNewsDO::getCreatedAt);
        return selectPage(pageParam, wrapper);
    }

}
