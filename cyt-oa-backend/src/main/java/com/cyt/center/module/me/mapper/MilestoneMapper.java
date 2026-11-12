package com.cyt.center.module.me.mapper;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.Milestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MilestoneMapper extends BaseMapperX<Milestone> {

    /** 在派生提醒事务的第一次读取前锁定稳定的用户身份，空里程碑集合也能串行生成。 */
    @Select("SELECT id FROM sys_user WHERE id = #{userId} FOR UPDATE")
    Long lockUserForSync(@Param("userId") Long userId);

    default List<Milestone> selectListByUser(Long userId, int limit) {
        return selectListLimitN(new LambdaQueryWrapperX<Milestone>()
                .eq(Milestone::getUserId, userId)
                .orderByAsc(Milestone::getMilestoneDate), limit);
    }

    default Milestone selectByTypeAndDate(Long userId, String type, LocalDate date) {
        // 兼容历史并发产生的重复记录：按稳定身份复用最早一条，不删除历史或要求清库。
        return selectFirstOne(new LambdaQueryWrapperX<Milestone>()
                .eq(Milestone::getUserId, userId)
                .eq(Milestone::getType, type)
                .eq(Milestone::getMilestoneDate, date)
                .orderByAsc(Milestone::getId));
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
