package com.cyt.center.module.portal.dal.postgresql.training;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 培训课时仓储（迁自 Portal 的 repositories/training.py）。
 */
@Mapper
public interface PortalTrainingLessonMapper extends BaseMapperX<PortalTrainingLessonDO> {

    /** 一门课的全部课时，按 sort_order, id 升序（课程详情与发布门禁共用） */
    default List<PortalTrainingLessonDO> selectByCourseId(Long courseId) {
        return selectList(new LambdaQueryWrapperX<PortalTrainingLessonDO>()
                .eq(PortalTrainingLessonDO::getCourseId, courseId)
                .last("ORDER BY sort_order ASC, id ASC"));
    }

    /** 一批课程各自的课时数 → {course_id: n}；没课时的课程不在结果里 */
    default Map<Long, Long> countByCourseIds(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<PortalTrainingLessonDO>()
                .select("course_id AS cid", "COUNT(*) AS cnt")
                .in("course_id", courseIds)
                .groupBy("course_id"));
        Map<Long, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(((Number) row.get("cid")).longValue(),
                ((Number) row.get("cnt")).longValue()));
        return result;
    }

    /** 一批课程各自各 kind 的课时数 → {course_id: {kind: n}}；没有该 kind 的键不出现（补零由应用层做） */
    default Map<Long, Map<String, Integer>> selectKindCountsByCourseIds(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<PortalTrainingLessonDO>()
                .select("course_id AS cid", "kind AS kind", "COUNT(*) AS cnt")
                .in("course_id", courseIds)
                .groupBy("course_id", "kind"));
        Map<Long, Map<String, Integer>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long cid = ((Number) row.get("cid")).longValue();
            String kind = (String) row.get("kind");
            int cnt = ((Number) row.get("cnt")).intValue();
            out.computeIfAbsent(cid, key -> new LinkedHashMap<>()).put(kind, cnt);
        }
        return out;
    }

    /** 一批课程各自的视频总时长（秒）→ {course_id: n}；duration_sec 只有视频课时首次心跳后才有值 */
    default Map<Long, Long> selectDurationSumByCourseIds(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<PortalTrainingLessonDO>()
                .select("course_id AS cid", "COALESCE(SUM(duration_sec), 0) AS total")
                .in("course_id", courseIds)
                .groupBy("course_id"));
        Map<Long, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(((Number) row.get("cid")).longValue(),
                ((Number) row.get("total")).longValue()));
        return result;
    }

    /** 该课程现有课时的最大 sort_order（无课时返回 0）；手建课时追加到末尾用 */
    default int maxSortOrder(Long courseId) {
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<PortalTrainingLessonDO>()
                .select("COALESCE(MAX(sort_order), 0) AS \"maxSort\"")
                .eq("course_id", courseId));
        Number maxSort = rows.isEmpty() ? null : (Number) rows.get(0).get("maxSort");
        return maxSort == null ? 0 : maxSort.intValue();
    }

    /** 按 source_prefix 批量取课时（导入幂等 upsert / 判型增量跳过） */
    default List<PortalTrainingLessonDO> selectBySourcePrefixes(Collection<String> sourcePrefixes) {
        return selectList(PortalTrainingLessonDO::getSourcePrefix, sourcePrefixes);
    }

}
