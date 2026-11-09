package com.cyt.center.module.portal.service.news;

import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsCommentDO;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsLikeDO;
import com.cyt.center.module.portal.dal.mysql.news.PortalNewsCommentMapper;
import com.cyt.center.module.portal.dal.mysql.news.PortalNewsLikeMapper;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.dal.dataobject.news.PortalNewsCommentDO.DELETED_PLACEHOLDER;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_COMMENT_CONTENT_EMPTY;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_COMMENT_DELETE_FORBIDDEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_COMMENT_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_COMMENT_PARENT_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_NOT_VISIBLE;

/**
 * 动态互动用例实现（对齐 Portal 的 application/news_engagement.py）。
 *
 * 可见性不在这里再写一遍：套 {@link PortalNewsService#requireVisible}，与详情接口同一条规则——
 * 两处各写一份迟早会漂，而非 published 的动态允许谁互动正是会漂的那半。
 */
@Slf4j
@Service
public class PortalNewsEngagementServiceImpl implements PortalNewsEngagementService {

    @Resource
    private PortalNewsLikeMapper likeMapper;
    @Resource
    private PortalNewsCommentMapper commentMapper;
    @Resource
    private PortalNewsService newsService;
    @Resource
    private PortalAccess access;
    @Resource
    private PortalOperatorResolver operators;

    @Override
    public Engagement getEngagement(Long userId, Long newsId) {
        newsService.requireVisible(userId, newsId, NEWS_NOT_VISIBLE);
        PortalOperator operator = operators.resolve(userId);
        List<PortalNewsCommentDO> comments = commentMapper.selectListByNewsId(newsId);
        boolean canModerate = access.canModerateComments(userId);
        List<CommentRow> rows = new ArrayList<>(comments.size());
        for (PortalNewsCommentDO comment : comments) {
            rows.add(new CommentRow(comment, canDelete(comment, operator, canModerate)));
        }
        boolean liked = likeMapper.selectByNewsIdAndUsername(newsId, operator.username()) != null;
        return new Engagement(likeMapper.selectCountByNewsId(newsId), liked, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeState like(Long userId, Long newsId) {
        newsService.requireVisible(userId, newsId, NEWS_NOT_VISIBLE);
        PortalOperator operator = operators.resolve(userId);
        PortalNewsLikeDO row = new PortalNewsLikeDO();
        row.setNewsId(newsId);
        row.setUsername(operator.username());
        row.setDisplayName(operator.displayNameOrUsername());
        try {
            likeMapper.insert(row);
        } catch (DuplicateKeyException ex) {
            // 多端同时点同一篇会撞 UNIQUE(news_id, username)：撞了就当已经点过，幂等返回
            log.debug("[like][动态({}) 用户({})] 重复点赞，按幂等处理", newsId, operator.username());
        }
        return new LikeState(likeMapper.selectCountByNewsId(newsId), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeState unlike(Long userId, Long newsId) {
        newsService.requireVisible(userId, newsId, NEWS_NOT_VISIBLE);
        PortalOperator operator = operators.resolve(userId);
        // 不校验有没有点过：没点过也删 0 行、静默成功
        likeMapper.deleteByNewsIdAndUsername(newsId, operator.username());
        return new LikeState(likeMapper.selectCountByNewsId(newsId), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentRow comment(Long userId, Long newsId, String content, Long parentId) {
        newsService.requireVisible(userId, newsId, NEWS_NOT_VISIBLE);
        PortalOperator operator = operators.resolve(userId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw exception(NEWS_COMMENT_CONTENT_EMPTY);
        }
        if (parentId != null) {
            PortalNewsCommentDO parent = commentMapper.selectById(parentId);
            if (parent == null || Boolean.TRUE.equals(parent.getIsDeleted())
                    || !Objects.equals(parent.getNewsId(), newsId)) {
                throw exception(NEWS_COMMENT_PARENT_INVALID);
            }
        }
        PortalNewsCommentDO row = new PortalNewsCommentDO();
        row.setNewsId(newsId);
        row.setUsername(operator.username());
        // 姓名与头像都是写入时的快照：改名换头像不该回头改写历史评论
        row.setDisplayName(operator.displayNameOrUsername());
        row.setAvatarUrl(operator.avatar());
        row.setContent(text);
        row.setParentId(parentId);
        row.setIsDeleted(false);
        // 时间戳自己写：靠库的 DEFAULT CURRENT_TIMESTAMP 回填不到返回给前端的那行，评论会显示成「—」
        row.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(row);
        return new CommentRow(row, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long newsId, Long commentId) {
        newsService.requireVisible(userId, newsId, NEWS_NOT_VISIBLE);
        PortalOperator operator = operators.resolve(userId);
        PortalNewsCommentDO row = commentId == null ? null : commentMapper.selectById(commentId);
        if (row == null || !Objects.equals(row.getNewsId(), newsId)) {
            throw exception(NEWS_COMMENT_NOT_EXISTS);
        }
        if (!canDelete(row, operator, access.canModerateComments(userId))) {
            throw exception(NEWS_COMMENT_DELETE_FORBIDDEN);
        }
        // 软删：占位文案只存在于库里，列表过滤 is_deleted，所以对外表现为评论数变少
        row.setIsDeleted(true);
        row.setContent(DELETED_PLACEHOLDER);
        row.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(row);
    }

    /** 作者本人或持评论治理能力（can_moderate_comments） */
    private static boolean canDelete(PortalNewsCommentDO comment, PortalOperator operator, boolean canModerate) {
        return Objects.equals(comment.getUsername(), operator.username()) || canModerate;
    }

}
