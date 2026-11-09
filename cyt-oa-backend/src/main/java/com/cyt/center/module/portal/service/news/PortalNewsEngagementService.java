package com.cyt.center.module.portal.service.news;

import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsCommentDO;

import java.util.List;

/**
 * 动态互动用例：点赞与评论（口径 Portal 的 application/news_engagement.py）。
 *
 * 层级与 Portal 一致——应用模块间共用的可见性规则走 {@link PortalNewsService#requireVisible}，
 * 不在这里复写一遍。
 */
public interface PortalNewsEngagementService {

    /** 评论行 + 当前登录人能不能删它（作者本人或持治理能力），前端据此显隐删除按钮 */
    record CommentRow(PortalNewsCommentDO comment, boolean canDelete) {
    }

    /** 互动数据包：点赞数、我点过没、未删除的评论（时间正序，楼中楼由前端按 parentId 组装） */
    record Engagement(Long likeCount, boolean liked, List<CommentRow> comments) {
    }

    /** 点赞 / 取消点赞后的最新状态 */
    record LikeState(Long likeCount, boolean liked) {
    }

    Engagement getEngagement(Long userId, Long newsId);

    /** 点赞，幂等：重复点不报错也不重复计数 */
    LikeState like(Long userId, Long newsId);

    /** 取消点赞，幂等：没点过也静默成功 */
    LikeState unlike(Long userId, Long newsId);

    /** 发表评论或回复（parentId 非空即回复，须是同一动态下未删除的评论） */
    CommentRow comment(Long userId, Long newsId, String content, Long parentId);

    /** 软删除评论：内容换占位文案，行留在库里，列表不再返回 */
    void deleteComment(Long userId, Long newsId, Long commentId);

}
