package com.cyt.center.module.portal.dal.postgresql.hrdoc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 文档主表仓储（迁自 Portal 的 repositories/hr_documents.py）。
 *
 * 可见性（dept_scope 命中 / SOP 权限）是业务规则，由 service 决定后过滤，本层只拼 where；
 * 版本快照 / 草稿版本 / 别名 / 入口绑定各自走各自的 mapper。
 */
@Mapper
public interface PortalHrDocumentMapper extends BaseMapperX<PortalHrDocumentDO> {

    /**
     * 管理端分页列文档：deleted 切回收站（默认 false 列正常文档）；status 支持伪状态 offline（已下架，
     * 用 offline_at 非空判，不写 status 列）；category 精确；q 按标题 + 草稿标题模糊。
     * 排序对齐 Portal 的 list_docs：category → slug → id。
     */
    default PageResult<PortalHrDocumentDO> selectPage(PageParam pageParam, String category, String status,
                                                      boolean deleted, String q) {
        LambdaQueryWrapperX<PortalHrDocumentDO> wrapper = new LambdaQueryWrapperX<>();
        if (deleted) {
            wrapper.isNotNull(PortalHrDocumentDO::getDeletedAt);
        } else {
            wrapper.isNull(PortalHrDocumentDO::getDeletedAt);
        }
        wrapper.eqIfPresent(PortalHrDocumentDO::getCategory, category);
        if ("offline".equals(status)) {
            wrapper.isNotNull(PortalHrDocumentDO::getOfflineAt);
        } else {
            wrapper.eqIfPresent(PortalHrDocumentDO::getStatus, status);
        }
        if (q != null && !q.isBlank()) {
            wrapper.and(w -> w.like(PortalHrDocumentDO::getTitle, q)
                    .or().like(PortalHrDocumentDO::getDraftTitle, q));
        }
        wrapper.orderByAsc(PortalHrDocumentDO::getCategory)
                .orderByAsc(PortalHrDocumentDO::getSlug)
                .orderByAsc(PortalHrDocumentDO::getId);
        return selectPage(pageParam, wrapper);
    }

    /** 员工端可见的已发布文档（published_at 非空 且 未下架 且 未删除），category 可选 */
    default List<PortalHrDocumentDO> selectPublished(String category) {
        LambdaQueryWrapper<PortalHrDocumentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(PortalHrDocumentDO::getPublishedAt)
                .isNull(PortalHrDocumentDO::getOfflineAt)
                .isNull(PortalHrDocumentDO::getDeletedAt);
        if (category != null && !category.isBlank()) {
            wrapper.eq(PortalHrDocumentDO::getCategory, category);
        }
        wrapper.orderByAsc(PortalHrDocumentDO::getSlug)
                .orderByAsc(PortalHrDocumentDO::getId);
        return selectList(wrapper);
    }

    /** 按旧 slug 查（主表兜底；别名表才是旧 URL 的长期解析入口） */
    default PortalHrDocumentDO selectBySlug(String slug) {
        return selectOne(PortalHrDocumentDO::getSlug, slug);
    }

    /** slug 占用检查：排除自身（编辑已存在文档时避免「自己占自己」） */
    default PortalHrDocumentDO selectBySlugExcludingId(String slug, Long excludeId) {
        LambdaQueryWrapper<PortalHrDocumentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHrDocumentDO::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(PortalHrDocumentDO::getId, excludeId);
        }
        return selectOne(wrapper);
    }

}
