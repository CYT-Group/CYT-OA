package com.cyt.center.module.me.mapper;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.me.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapperX<Notification> {

    default PageResult<Notification> selectPageForUser(Long userId, String category, Boolean unreadOnly,
                                                       PageParam pageParam) {
        LambdaQueryWrapperX<Notification> wrapper = new LambdaQueryWrapperX<Notification>()
                .eq(Notification::getUserId, userId)
                .eqIfPresent(Notification::getCategory, category);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(Notification::getReadFlag, false);
        }
        wrapper.orderByDesc(Notification::getCreatedAt).orderByDesc(Notification::getId);
        return selectPage(pageParam, wrapper);
    }

    default List<Notification> selectRecent(Long userId, int limit) {
        return selectListLimitN(new LambdaQueryWrapperX<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .orderByDesc(Notification::getId), limit);
    }

    default Long selectUnreadCount(Long userId) {
        return selectCount(new LambdaQueryWrapperX<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, false));
    }

    default Notification selectByDedupKey(Long userId, String dedupKey) {
        return selectOne(Notification::getUserId, userId, Notification::getDedupKey, dedupKey);
    }

    default int markAllRead(Long userId, String category) {
        Notification patch = new Notification();
        patch.setReadFlag(true);
        LambdaQueryWrapperX<Notification> wrapper = new LambdaQueryWrapperX<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, false);
        if (category != null && !category.isBlank()) {
            wrapper.eq(Notification::getCategory, category);
        }
        return update(patch, wrapper);
    }

    /** 按去重键清理：本轮没再命中的旧提醒不该继续挂着。 */
    default int deleteByDedupKeysNotIn(Long userId, String type, List<String> keepKeys) {
        LambdaQueryWrapperX<Notification> wrapper = new LambdaQueryWrapperX<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getType, type);
        if (!keepKeys.isEmpty()) {
            wrapper.notIn(Notification::getDedupKey, keepKeys);
        }
        return delete(wrapper);
    }

}
