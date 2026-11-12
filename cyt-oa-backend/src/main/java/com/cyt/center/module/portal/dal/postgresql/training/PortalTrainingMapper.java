package com.cyt.center.module.portal.dal.postgresql.training;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训课程仓储（迁自 Portal 的 repositories/training.py）。
 *
 * 可见性（dept_scope 命中）是业务规则，由 service 决定后过滤，本层只负责拼 where 条件；
 * 引用计数 / 改名级联是分类模块的意图，入口在 PortalCategoryServiceImpl，本层只提供 countByCategoryId /
 * countByCategoryIds / updateCategoryMirror 三个原语。
 */
@Mapper
public interface PortalTrainingMapper extends BaseMapperX<PortalTrainingDO> {

    /**
     * 管理端分页列课程（含 draft）：status / category（文本列精确匹配）/ q（标题+简介模糊）筛选，
     * 按创建时间倒序（口径 docs/Portal管理面迁移契约.md §四 批次 3 管理端课程列表）。
     */
    default PageResult<PortalTrainingDO> selectPage(PageParam pageParam, String status, String category, String q) {
        LambdaQueryWrapperX<PortalTrainingDO> wrapper = new LambdaQueryWrapperX<PortalTrainingDO>()
                .eqIfPresent(PortalTrainingDO::getStatus, status)
                .eqIfPresent(PortalTrainingDO::getCategory, category);
        if (q != null && !q.isBlank()) {
            wrapper.and(item -> item.like(PortalTrainingDO::getTitle, q)
                    .or().like(PortalTrainingDO::getDescription, q));
        }
        wrapper.orderByDesc(PortalTrainingDO::getCreatedAt);
        return selectPage(pageParam, wrapper);
    }

    /** 某分类（小类）下的课程数，含 draft（培训小类的引用口径：草稿也挂着分类） */
    default long countByCategoryId(Long categoryId) {
        return selectCount(PortalTrainingDO::getCategoryId, categoryId);
    }

    /** 一批分类各自挂了多少课程（含 draft）→ {category_id: n}；管理面板的小类课程数用 */
    default Map<Long, Long> countByCategoryIds(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<PortalTrainingDO>()
                .select("category_id AS cid", "COUNT(*) AS cnt")
                .in("category_id", categoryIds)
                .groupBy("category_id"));
        Map<Long, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(((Number) row.get("cid")).longValue(),
                ((Number) row.get("cnt")).longValue()));
        return result;
    }

    /** 改小类名 → 批量刷课程表的过渡期文本镜像列（S3a 停写文本列后删）；返回影响行数 */
    default int updateCategoryMirror(Long categoryId, String newName) {
        return update(null, new LambdaUpdateWrapper<PortalTrainingDO>()
                .set(PortalTrainingDO::getCategory, newName)
                .eq(PortalTrainingDO::getCategoryId, categoryId));
    }

    /** 全量 published 课程列表（员工端 catalog / 老列表的候选集） */
    default List<PortalTrainingDO> selectPublished() {
        return selectList(PortalTrainingDO::getStatus, PortalTrainingDO.STATUS_PUBLISHED);
    }

    /** 已发布课程数（stats 口径） */
    default long countPublished() {
        return selectCount(PortalTrainingDO::getStatus, PortalTrainingDO.STATUS_PUBLISHED);
    }

    /** 按 source_prefix 批量取课（导入幂等 upsert） */
    default List<PortalTrainingDO> selectBySourcePrefixes(Collection<String> sourcePrefixes) {
        return selectList(PortalTrainingDO::getSourcePrefix, sourcePrefixes);
    }

}
