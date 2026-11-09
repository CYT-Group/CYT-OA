package com.cyt.center.module.me.mapper;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.RhythmNode;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RhythmNodeMapper extends BaseMapperX<RhythmNode> {

    /** 与 [begin, end] 有时间交集的节点：开始日不晚于区间末，结束日不早于区间始。 */
    default List<RhythmNode> selectListInRange(LocalDate begin, LocalDate end, String kind) {
        return selectList(new LambdaQueryWrapperX<RhythmNode>()
                .eqIfPresent(RhythmNode::getKind, kind)
                .le(RhythmNode::getStartDate, end)
                .ge(RhythmNode::getEndDate, begin)
                .orderByAsc(RhythmNode::getStartDate));
    }

}
