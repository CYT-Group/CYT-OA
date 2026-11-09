package com.cyt.center.module.me.mapper;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.RhythmFollow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RhythmFollowMapper extends BaseMapperX<RhythmFollow> {

    default RhythmFollow selectByUserAndNode(Long userId, Long nodeId) {
        return selectOne(RhythmFollow::getUserId, userId, RhythmFollow::getNodeId, nodeId);
    }

    default int deleteByUserAndNode(Long userId, Long nodeId) {
        return delete(new LambdaQueryWrapperX<RhythmFollow>()
                .eq(RhythmFollow::getUserId, userId)
                .eq(RhythmFollow::getNodeId, nodeId));
    }

}
