package com.cyt.center.module.portal.dal.mysql.training;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonProgressDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课时进度仓储（迁自 Portal 的 repositories/training.py）。
 */
@Mapper
public interface PortalTrainingLessonProgressMapper extends BaseMapperX<PortalTrainingLessonProgressDO> {

    /** 某人在某课时的进度行（UNIQUE(username, lesson_id)），不存在返回 null */
    default PortalTrainingLessonProgressDO selectByUsernameAndLessonId(String username, Long lessonId) {
        return selectOne(PortalTrainingLessonProgressDO::getUsername, username,
                PortalTrainingLessonProgressDO::getLessonId, lessonId);
    }

    /** 某人在一批课时上的进度 → {lesson_id: 进度行}；课程详情一次 join 出全部课时的 my */
    default Map<Long, PortalTrainingLessonProgressDO> selectByUsernameAndLessonIds(String username,
                                                                                   Collection<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            return Map.of();
        }
        List<PortalTrainingLessonProgressDO> rows = selectList(
                new LambdaQueryWrapperX<PortalTrainingLessonProgressDO>()
                        .eq(PortalTrainingLessonProgressDO::getUsername, username)
                        .in(PortalTrainingLessonProgressDO::getLessonId, lessonIds));
        Map<Long, PortalTrainingLessonProgressDO> out = new LinkedHashMap<>();
        rows.forEach(row -> out.put(row.getLessonId(), row));
        return out;
    }

    /** 某人按课程统计已完成课时数 → {course_id: n}（catalog 折算进度百分比、课程级 completed 判定用） */
    @Select("<script>"
            + "SELECT l.course_id AS cid, COUNT(p.id) AS cnt "
            + "FROM portal_training_lesson_progress p "
            + "JOIN portal_training_lessons l ON p.lesson_id = l.id "
            + "WHERE p.username = #{username} AND p.status = 'completed' AND l.course_id IN "
            + "<foreach collection='courseIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> "
            + "GROUP BY l.course_id"
            + "</script>")
    List<Map<String, Object>> selectCompletedRows(@Param("username") String username,
                                                  @Param("courseIds") Collection<Long> courseIds);

    default Map<Long, Long> selectCompletedCountByCourseIds(String username, Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : selectCompletedRows(username, courseIds)) {
            result.put(((Number) row.get("cid")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        return result;
    }

}
