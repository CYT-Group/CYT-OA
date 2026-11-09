package com.cyt.center.module.portal.controller.app.news;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsCommentReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsCommentRespVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsDetailRespVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsEngagementRespVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsLikeStateRespVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsPageReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsRespVO;
import com.cyt.center.module.portal.service.news.PortalNewsEngagementService;
import com.cyt.center.module.portal.service.news.PortalNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_NOT_EXISTS;

/**
 * 公司动态读侧接口（登录即可，无能力位要求）。
 *
 * 为什么单开一套而不是让门户直接调 /admin-api：管理令牌要求 device=admin，
 * 门户的登录人拿的是普通令牌，读侧必须走 /api/**（口径 docs/Portal管理面迁移契约.md §二）。
 * 可见性规则跟管理面同一条用例，不在这儿另写一份。
 *
 * 互动（点赞 / 评论）也在这边：那是读者的动作，不是管理动作，Portal 的详情页就是这么用的。
 */
@Tag(name = "门户 - 公司动态（读）")
@RestController
@RequestMapping("/api/portal/news")
@Validated
@RequiredArgsConstructor
public class PortalNewsOpenController {

    private final PortalNewsService newsService;
    private final PortalNewsEngagementService engagementService;

    @GetMapping("/page")
    @Operation(summary = "分页列动态（默认 published）")
    public Result<PageResult<PortalNewsRespVO>> page(@Valid PortalNewsPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(newsService.page(getLoginUserId(), pageReqVO), PortalNewsRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "单条详情，含正文")
    @Parameter(name = "id", description = "动态编号", required = true)
    public Result<PortalNewsDetailRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(newsService.requireVisible(getLoginUserId(), id, NEWS_NOT_EXISTS),
                PortalNewsDetailRespVO.class));
    }

    @GetMapping("/engagement")
    @Operation(summary = "互动数据：点赞数 / 我点过没 / 评论列表（详情页一次拉走）")
    @Parameter(name = "id", description = "动态编号", required = true)
    public Result<PortalNewsEngagementRespVO> engagement(@RequestParam("id") Long id) {
        PortalNewsEngagementService.Engagement data = engagementService.getEngagement(getLoginUserId(), id);
        PortalNewsEngagementRespVO vo = new PortalNewsEngagementRespVO();
        vo.setLikeCount(data.likeCount());
        vo.setLiked(data.liked());
        vo.setCommentCount(data.comments().size());
        List<PortalNewsCommentRespVO> comments = new ArrayList<>(data.comments().size());
        for (PortalNewsEngagementService.CommentRow row : data.comments()) {
            PortalNewsCommentRespVO item = BeanUtils.toBean(row.comment(), PortalNewsCommentRespVO.class);
            item.setCanDelete(row.canDelete());
            comments.add(item);
        }
        vo.setComments(comments);
        return success(vo);
    }

    @PostMapping("/like")
    @Operation(summary = "点赞（幂等：重复点不报错也不重复计数）")
    @Parameter(name = "id", description = "动态编号", required = true)
    public Result<PortalNewsLikeStateRespVO> like(@RequestParam("id") Long id) {
        return success(toRow(engagementService.like(getLoginUserId(), id)));
    }

    @DeleteMapping("/like")
    @Operation(summary = "取消点赞（没点过也静默成功）")
    @Parameter(name = "id", description = "动态编号", required = true)
    public Result<PortalNewsLikeStateRespVO> unlike(@RequestParam("id") Long id) {
        return success(toRow(engagementService.unlike(getLoginUserId(), id)));
    }

    @PostMapping("/comment")
    @Operation(summary = "发表评论 / 回复（parentId 非空即回复）")
    @Parameter(name = "id", description = "动态编号", required = true)
    public Result<PortalNewsCommentRespVO> comment(@RequestParam("id") Long id,
                                                  @Valid @RequestBody PortalNewsCommentReqVO commentReqVO) {
        PortalNewsEngagementService.CommentRow row = engagementService.comment(getLoginUserId(), id,
                commentReqVO.getContent(), commentReqVO.getParentId());
        PortalNewsCommentRespVO vo = BeanUtils.toBean(row.comment(), PortalNewsCommentRespVO.class);
        vo.setCanDelete(row.canDelete());
        return success(vo);
    }

    @DeleteMapping("/comment")
    @Operation(summary = "软删除评论（作者本人或持评论治理能力）")
    @Parameter(name = "id", description = "动态编号", required = true)
    @Parameter(name = "commentId", description = "评论编号", required = true)
    public Result<Boolean> deleteComment(@RequestParam("id") Long id, @RequestParam("commentId") Long commentId) {
        engagementService.deleteComment(getLoginUserId(), id, commentId);
        return success(true);
    }

    private static PortalNewsLikeStateRespVO toRow(PortalNewsEngagementService.LikeState state) {
        PortalNewsLikeStateRespVO vo = new PortalNewsLikeStateRespVO();
        vo.setLikeCount(state.likeCount());
        vo.setLiked(state.liked());
        return vo;
    }

}
