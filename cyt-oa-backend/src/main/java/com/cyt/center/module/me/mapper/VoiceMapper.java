package com.cyt.center.module.me.mapper;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.Voice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VoiceMapper extends BaseMapperX<Voice> {

    default List<Voice> selectListByUser(Long userId) {
        return selectList(new LambdaQueryWrapperX<Voice>()
                .eq(Voice::getUserId, userId)
                .orderByDesc(Voice::getCreatedAt)
                .orderByDesc(Voice::getId));
    }

    default Long countByUser(Long userId) {
        return selectCount(new LambdaQueryWrapperX<Voice>().eq(Voice::getUserId, userId));
    }

}
