package com.cyt.center.module.portal.dal.postgresql.news;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsCommentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 动态评论仓储。
 *
 * 列表只返回未软删的行、按时间正序（楼中楼靠 parentId 让前端组装，服务端不返回树）。
 * 软删行留在库里，只对外表现为评论数变少。
 */
@Mapper
public interface PortalNewsCommentMapper extends BaseMapperX<PortalNewsCommentDO> {

    default List<PortalNewsCommentDO> selectListByNewsId(Long newsId) {
        return selectList(new LambdaQueryWrapperX<PortalNewsCommentDO>()
                .eq(PortalNewsCommentDO::getNewsId, newsId)
                .eq(PortalNewsCommentDO::getIsDeleted, false)
                .orderByAsc(PortalNewsCommentDO::getCreatedAt));
    }

}
