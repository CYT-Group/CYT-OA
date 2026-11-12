package com.cyt.center.module.portal.dal.postgresql.category;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.category.PortalCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用分类：portal_categories 全部 SQL 的唯一住所（含跨表引用计数与改名级联的入口）。
 *
 * ordering 口径与 Portal 一致：按 (depth, parent_id 空值优先, sort_order, name) 平铺返回，
 * 前端自己组树；用 (parent_id IS NOT NULL) ASC 将根分类排在前面。
 */
@Mapper
public interface PortalCategoryMapper extends BaseMapperX<PortalCategoryDO> {

    String SCOPE_COLUMN = "scope";

    default List<PortalCategoryDO> selectListByScope(String scope, boolean includeArchived) {
        LambdaQueryWrapperX<PortalCategoryDO> wrapper = new LambdaQueryWrapperX<PortalCategoryDO>()
                .eq(PortalCategoryDO::getScope, scope);
        if (!includeArchived) {
            wrapper.isNull(PortalCategoryDO::getArchivedAt);
        }
        wrapper.last("ORDER BY depth ASC, (parent_id IS NOT NULL) ASC, sort_order ASC, name ASC");
        return selectList(wrapper);
    }

    /** 按主键取行并加行锁：改名级联 / 归档摘子行要与并发写互斥 */
    default PortalCategoryDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(PortalCategoryDO::getId, id);
    }

    default List<PortalCategoryDO> selectListByIds(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapperX<PortalCategoryDO>().in(PortalCategoryDO::getId, ids));
    }

    /** 同 scope 的在役同名行（唯一索引保证最多一条） */
    default PortalCategoryDO selectActiveByScopeAndName(String scope, String name) {
        return selectOne(new LambdaQueryWrapperX<PortalCategoryDO>()
                .eq(PortalCategoryDO::getScope, scope)
                .eq(PortalCategoryDO::getName, name)
                .isNull(PortalCategoryDO::getArchivedAt));
    }

    /** 同 scope 同名行，含归档：在役优先，其次最近归档的那条 */
    default PortalCategoryDO selectAnyByScopeAndName(String scope, String name) {
        LambdaQueryWrapperX<PortalCategoryDO> wrapper = new LambdaQueryWrapperX<PortalCategoryDO>()
                .eq(PortalCategoryDO::getScope, scope)
                .eq(PortalCategoryDO::getName, name);
        wrapper.last("ORDER BY (archived_at IS NULL) DESC, archived_at DESC LIMIT 1");
        return selectFirstOne(wrapper);
    }

    /** 同 scope 同 code 的行，含归档（code 是永久锚点，恢复时也不许撞） */
    default PortalCategoryDO selectByScopeAndCode(String scope, String code) {
        return selectOne(new LambdaQueryWrapperX<PortalCategoryDO>()
                .eq(PortalCategoryDO::getScope, scope)
                .eq(PortalCategoryDO::getCode, code));
    }

    default Long selectChildrenCount(Long parentId, boolean activeOnly) {
        LambdaQueryWrapperX<PortalCategoryDO> wrapper = new LambdaQueryWrapperX<PortalCategoryDO>()
                .eq(PortalCategoryDO::getParentId, parentId);
        if (activeOnly) {
            wrapper.isNull(PortalCategoryDO::getArchivedAt);
        }
        return selectCount(wrapper);
    }

    /** 整个 scope 一次算出 {parentId: 在役子行数}，管理列表按行取会变成 N+1 */
    default Map<Long, Long> selectChildrenCountMap(String scope) {
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<PortalCategoryDO>()
                .select("parent_id AS \"parentKey\"", "COUNT(*) AS cnt")
                .eq(SCOPE_COLUMN, scope)
                .isNotNull("parent_id")
                .isNull("archived_at")
                .groupBy("parent_id"));
        Map<Long, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(((Number) row.get("parentKey")).longValue(),
                ((Number) row.get("cnt")).longValue()));
        return result;
    }

    /**
     * 同层（同 scope、同父、同 depth）当前最大 sort_order，空层 0。
     *
     * 必须带上 depth：两层形状下大类与未归类小类同为 parent_id IS NULL，只看父分不开层。
     */
    default int selectMaxSortOrder(String scope, Long parentId, Integer depth) {
        QueryWrapper<PortalCategoryDO> wrapper = new QueryWrapper<PortalCategoryDO>()
                .select("COALESCE(MAX(sort_order), 0) AS \"maxSort\"")
                .eq(SCOPE_COLUMN, scope)
                .eq("depth", depth);
        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }
        // 聚合查询不带 GROUP BY 恒返回一行；别名刻意不含下划线，避开 Map 键名的大小写口径分歧
        List<Map<String, Object>> rows = selectMaps(wrapper);
        Number maxSort = rows.isEmpty() ? null : (Number) rows.get(0).get("maxSort");
        return maxSort == null ? 0 : maxSort.intValue();
    }

    /**
     * 归档大类：其下全部子行（含归档子行）parent_id 置空 = 变未归类，返回条数。
     *
     * 归档子行也摘：否则它日后恢复会挂回一个已归档的父，员工端永远看不到。
     */
    default int detachChildren(Long parentId) {
        return update(null, new LambdaUpdateWrapper<PortalCategoryDO>()
                .set(PortalCategoryDO::getParentId, null)
                .eq(PortalCategoryDO::getParentId, parentId));
    }

}
