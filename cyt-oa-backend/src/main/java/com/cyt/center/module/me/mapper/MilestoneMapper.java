package com.cyt.center.module.me.mapper;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.Milestone;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MilestoneMapper extends BaseMapperX<Milestone> {

    default List<Milestone> selectListByUser(Long userId, int limit) {
        return selectListLimitN(new LambdaQueryWrapperX<Milestone>()
                .eq(Milestone::getUserId, userId)
                .orderByAsc(Milestone::getMilestoneDate), limit);
    }

    default Milestone selectByTypeAndDate(Long userId, String type, LocalDate date) {
        return selectOne(Milestone::getUserId, userId, Milestone::getType, type,
                Milestone::getMilestoneDate, date);
    }

    default Long countByUser(Long userId) {
        return selectCount(new LambdaQueryWrapperX<Milestone>().eq(Milestone::getUserId, userId));
    }

    /** 派生类型只保留档案当前算出的那一天；date 为空说明档案里没这项了，整类清掉。 */
    default int deleteDerivedNotDate(Long userId, String type, LocalDate date) {
        LambdaQueryWrapperX<Milestone> wrapper = new LambdaQueryWrapperX<Milestone>()
                .eq(Milestone::getUserId, userId)
                .eq(Milestone::getType, type);
        if (date != null) {
            wrapper.ne(Milestone::getMilestoneDate, date);
        }
        return delete(wrapper);
    }

}
