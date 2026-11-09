package com.cyt.center.module.portal.dal.mysql.wiki;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库仓储。
 */
@Mapper
public interface PortalWikiMapper extends BaseMapperX<PortalWikiDO> {

    /**
     * 分页列文档：category / department 是精确匹配（与 Portal 一致，不是模糊），
     * q 只模糊匹配标题与正文，按创建时间倒序。
     *
     * status / visibleTo 都在 where 上写死，可见性判定留在 service：非 published 的行只有作者和审阅者看得到。
     *
     * @param status    null = 不限状态（审阅者的「全部」）
     * @param visibleTo 非 null 时 = 「这个人写的 或 已发布的」——普通人的「全部」靠它收敛，不会翻出别人的草稿
     */
    default PageResult<PortalWikiDO> selectPage(PageParam pageParam, String status, String category,
                                                String department, String q, String visibleTo) {
        LambdaQueryWrapperX<PortalWikiDO> wrapper = new LambdaQueryWrapperX<PortalWikiDO>()
                .eqIfPresent(PortalWikiDO::getCategory, category)
                .eqIfPresent(PortalWikiDO::getDepartment, department);
        if (visibleTo != null) {
            if (status == null) {
                wrapper.and(item -> item.eq(PortalWikiDO::getStatus, PortalWikiDO.STATUS_PUBLISHED)
                        .or().eq(PortalWikiDO::getAuthorUsername, visibleTo));
            } else {
                wrapper.and(item -> item.eq(PortalWikiDO::getStatus, status)
                        .or().eq(PortalWikiDO::getAuthorUsername, visibleTo));
            }
        } else {
            wrapper.eqIfPresent(PortalWikiDO::getStatus, status);
        }
        if (q != null && !q.isBlank()) {
            wrapper.and(item -> item.like(PortalWikiDO::getTitle, q)
                    .or().like(PortalWikiDO::getContent, q));
        }
        wrapper.orderByDesc(PortalWikiDO::getCreatedAt);
        return selectPage(pageParam, wrapper);
    }

    /**
     * 详情自增浏览量。
     *
     * 用 setSql 做原子自增，不读改写：Portal 的口径是无去重、无频控，同一人反复打开就一直加。
     */
    default int increaseViewCount(Long id) {
        return update(null, new LambdaUpdateWrapper<PortalWikiDO>()
                .setSql("view_count = view_count + 1")
                .eq(PortalWikiDO::getId, id));
    }

}
