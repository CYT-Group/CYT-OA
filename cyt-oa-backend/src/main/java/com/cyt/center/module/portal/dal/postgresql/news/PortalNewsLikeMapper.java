package com.cyt.center.module.portal.dal.postgresql.news;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsLikeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 动态点赞仓储。
 *
 * 这里不判「是否点过」——幂等交给 portal_news_like 的 UNIQUE(news_id, username)，
 * service 层把插入时撞出来的 DuplicateKeyException 当成功吞掉，并发下计数才不会翻倍。
 */
@Mapper
public interface PortalNewsLikeMapper extends BaseMapperX<PortalNewsLikeDO> {

    default List<PortalNewsLikeDO> selectListByNewsId(Long newsId) {
        return selectList(new LambdaQueryWrapperX<PortalNewsLikeDO>()
                .eq(PortalNewsLikeDO::getNewsId, newsId));
    }

    default Long selectCountByNewsId(Long newsId) {
        return selectCount(PortalNewsLikeDO::getNewsId, newsId);
    }

    default PortalNewsLikeDO selectByNewsIdAndUsername(Long newsId, String username) {
        return selectOne(PortalNewsLikeDO::getNewsId, newsId, PortalNewsLikeDO::getUsername, username);
    }

    /** 取消点赞不校验行是否存在：没点过也返回 200，计数不变 */
    default int deleteByNewsIdAndUsername(Long newsId, String username) {
        return delete(new LambdaQueryWrapperX<PortalNewsLikeDO>()
                .eq(PortalNewsLikeDO::getNewsId, newsId)
                .eq(PortalNewsLikeDO::getUsername, username));
    }

}
