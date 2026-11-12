package com.cyt.center.module.portal.dal.postgresql.products;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.products.PortalProductDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 新品动态仓储（迁自 Portal 的 repositories/products.py）。
 *
 * 排序口径照 Portal 的 list_products：按创建时间倒序。读侧只回 published，可见性判定留 service。
 */
@Mapper
public interface PortalProductMapper extends BaseMapperX<PortalProductDO> {

    /** 管理端分页列新品：brand 精确匹配，q 对名称 / 介绍做模糊匹配，按创建时间倒序。 */
    default PageResult<PortalProductDO> selectPage(PageParam pageParam, String brand, String q) {
        LambdaQueryWrapperX<PortalProductDO> wrapper = new LambdaQueryWrapperX<PortalProductDO>()
                .eqIfPresent(PortalProductDO::getBrand, brand);
        if (q != null && !q.isBlank()) {
            wrapper.and(item -> item.like(PortalProductDO::getName, q)
                    .or().like(PortalProductDO::getDescription, q));
        }
        wrapper.orderByDesc(PortalProductDO::getCreatedAt);
        return selectPage(pageParam, wrapper);
    }

    /** 已发布的新品列表（可带品牌精确筛选），按创建时间倒序（Portal 读侧 GET /products 口径）。 */
    default List<PortalProductDO> selectPublished(String brand, int limit) {
        return selectListLimitN(new LambdaQueryWrapperX<PortalProductDO>()
                .eq(PortalProductDO::getStatus, PortalProductDO.STATUS_PUBLISHED)
                .eqIfPresent(PortalProductDO::getBrand, brand)
                .orderByDesc(PortalProductDO::getCreatedAt), limit);
    }

}
