package com.cyt.center.module.portal.controller.admin.news;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsApproveReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsDetailRespVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsPageReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsRespVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsSaveReqVO;
import com.cyt.center.module.portal.controller.admin.vo.PortalReviewReqVO;
import com.cyt.center.module.portal.domain.PortalCapability;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_NOT_EXISTS;

/**
 * 公司动态管理接口层。
 *
 * 端点闸按 Portal 的 require(...) 一一对应：新建要 can_create_content、删除要 can_delete、
 * 审批要 can_review；改稿不设端点闸——作者本人也能改自己的，谁能改由用例按人判
 * （Portal 的 PUT /news/{id} 同样只有 get_current_user）。
 *
 * 点赞与评论不在这里：那是读侧互动，挂在 /api/portal/news/** 给展示端用。
 */
@Tag(name = "管理后台 - 公司动态")
@RestController
@RequestMapping("/portal/news")
@Validated
@RequiredArgsConstructor
public class PortalNewsController {

    private final PortalNewsService newsService;

    @GetMapping("/page")
    @Operation(summary = "分页列动态（默认 published；非 published 的可见范围按能力收敛）")
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

    @PostMapping("/create")
    @Operation(summary = "新建动态（无全量编辑权的角色进 pending 走审批）")
    @SaCheckPermission(PortalCapability.CREATE_CONTENT)
    public Result<PortalNewsRespVO> create(@Valid @RequestBody PortalNewsSaveReqVO createReqVO) {
        return success(BeanUtils.toBean(newsService.create(getLoginUserId(), createReqVO), PortalNewsRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "改动态（全量覆盖；非全量编辑权者改已发布稿会退回 pending）")
    public Result<PortalNewsRespVO> update(@Valid @RequestBody PortalNewsSaveReqVO updateReqVO) {
        return success(BeanUtils.toBean(newsService.update(getLoginUserId(), updateReqVO.getId(), updateReqVO),
                PortalNewsRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删动态（硬删，删除动作进操作日志）")
    @Parameter(name = "id", description = "动态编号", required = true)
    @SaCheckPermission(PortalCapability.DELETE)
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        newsService.delete(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过并消费作者的推送意图")
    @SaCheckPermission(PortalCapability.REVIEW)
    public Result<PortalNewsRespVO> approve(@Valid @RequestBody PortalNewsApproveReqVO approveReqVO) {
        Long id = approveReqVO.getId();
        newsService.approve(getLoginUserId(), id, approveReqVO);
        return success(BeanUtils.toBean(newsService.requireVisible(getLoginUserId(), id, NEWS_NOT_EXISTS),
                PortalNewsRespVO.class));
    }

    @PostMapping("/reject")
    @Operation(summary = "审批驳回（置 rejected，推送意图一并清掉）")
    @SaCheckPermission(PortalCapability.REVIEW)
    public Result<Boolean> reject(@Valid @RequestBody PortalReviewReqVO reviewReqVO) {
        newsService.reject(getLoginUserId(), reviewReqVO.getId(), reviewReqVO.getComment());
        return success(true);
    }

}
