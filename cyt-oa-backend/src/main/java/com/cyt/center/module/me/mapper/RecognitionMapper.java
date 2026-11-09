package com.cyt.center.module.me.mapper;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.Recognition;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RecognitionMapper extends BaseMapperX<Recognition> {

    default List<Recognition> selectListByTo(Long userId) {
        return selectList(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getToId, userId)
                .orderByDesc(Recognition::getCreatedAt)
                .orderByDesc(Recognition::getId));
    }

    default List<Recognition> selectListByToAndPeriod(Long userId, String period) {
        return selectList(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getToId, userId)
                .eqIfPresent(Recognition::getPeriod, period)
                .orderByDesc(Recognition::getCreatedAt));
    }

    default Long countByTo(Long userId) {
        return selectCount(new LambdaQueryWrapperX<Recognition>().eq(Recognition::getToId, userId));
    }

    /** 我送出过的认可，用来判断某个里程碑是否已经祝福过。 */
    default List<Recognition> selectListByFrom(Long fromId, String type) {
        return selectList(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getFromId, fromId)
                .eqIfPresent(Recognition::getType, type)
                .orderByDesc(Recognition::getCreatedAt));
    }

    default Recognition selectBlessing(Long fromId, Long toId, String milestoneType, String date) {
        return selectOne(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getFromId, fromId)
                .eq(Recognition::getToId, toId)
                .eq(Recognition::getType, Recognition.TYPE_BLESSING)
                .eq(Recognition::getScope, milestoneType)
                .eq(Recognition::getPeriod, date));
    }

    /** 某个周期里我送出的这类认可，看板点亮「赞 / 之星」用的就是它。 */
    default List<Recognition> selectListByFromAndPeriod(Long fromId, String type, String period) {
        return selectList(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getFromId, fromId)
                .eq(Recognition::getType, type)
                .eq(Recognition::getPeriod, period));
    }

    default Recognition selectByPeriod(Long fromId, Long toId, String type, String period) {
        return selectOne(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getFromId, fromId)
                .eq(Recognition::getToId, toId)
                .eq(Recognition::getType, type)
                .eq(Recognition::getPeriod, period));
    }

    /** 一个月只有一个本月之星：换人时先把上一次的提名撤掉，不留两条互相打脸的记录。 */
    default int deleteByFromAndPeriod(Long fromId, String type, String period) {
        return delete(new LambdaQueryWrapperX<Recognition>()
                .eq(Recognition::getFromId, fromId)
                .eq(Recognition::getType, type)
                .eq(Recognition::getPeriod, period));
    }

}
