package com.cyt.center.module.portal.dal.mysql.training;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingProgressDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 课程级进度仓储（迁自 Portal 的 repositories/training.py）。
 *
 * portal_training_progress 无唯一约束（Portal 原表如此，数据可能有重复），所以按
 * (username, training_id) 取行用 selectFirstOne（多条时取第一条），不用 selectOne（会抛多结果异常）。
 */
@Mapper
public interface PortalTrainingProgressMapper extends BaseMapperX<PortalTrainingProgressDO> {

    /** 某人在某课程的课程级进度行，不存在返回 null（可能有多条历史行，取第一条） */
    default PortalTrainingProgressDO selectByUsernameAndTrainingId(String username, Long trainingId) {
        return selectFirstOne(PortalTrainingProgressDO::getUsername, username,
                PortalTrainingProgressDO::getTrainingId, trainingId);
    }

    /** 某人的全部课程级进度记录（不做排序，legacy my-progress 用） */
    default List<PortalTrainingProgressDO> selectListByUsername(String username) {
        return selectList(PortalTrainingProgressDO::getUsername, username);
    }

    /** 开始学习人数（stats 口径：按 username 去重） */
    @Select("SELECT COUNT(DISTINCT username) FROM portal_training_progress")
    long countDistinctUsername();

    /** 完成次数（stats 口径：status='completed' 的进度条数） */
    default long countCompleted() {
        return selectCount(PortalTrainingProgressDO::getStatus, PortalTrainingProgressDO.STATUS_COMPLETED);
    }

}
