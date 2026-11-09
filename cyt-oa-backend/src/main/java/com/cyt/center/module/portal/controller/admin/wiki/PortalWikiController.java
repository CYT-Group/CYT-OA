package com.cyt.center.module.portal.controller.admin.wiki;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.vo.PortalReviewReqVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiDetailRespVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiPageReqVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiRespVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiSaveReqVO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.wiki.PortalWikiService;
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

/**
 * 知识库管理接口层，闸与 Portal 的 routers/wiki.py 一一对应。
 *
 * 详情会自增浏览量，所以列在管理面：审阅队列看正文是正常的浏览行为。
 */
@Tag(name = "管理后台 - 知识库")
@RestController
@RequestMapping("/portal/wiki")
@Validated
@RequiredArgsConstructor
public class PortalWikiController {

    private final PortalWikiService wikiService;

    @GetMapping("/page")
    @Operation(summary = "分页列文档（默认 published；非 published 的可见范围按能力收敛）")
    public Result<PageResult<PortalWikiRespVO>> page(@Valid PortalWikiPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(wikiService.page(getLoginUserId(), pageReqVO), PortalWikiRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "单篇详情，含正文；每次调用自增浏览量")
    @Parameter(name = "id", description = "文档编号", required = true)
    public Result<PortalWikiDetailRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(wikiService.getForRead(getLoginUserId(), id), PortalWikiDetailRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新建文档（显式 draft 存草稿；审阅者默认 published；其余进 pending 走审批）")
    @SaCheckPermission(PortalCapability.CREATE_CONTENT)
    public Result<PortalWikiRespVO> create(@Valid @RequestBody PortalWikiSaveReqVO createReqVO) {
        return success(BeanUtils.toBean(wikiService.create(getLoginUserId(), createReqVO), PortalWikiRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "改文档（全量覆盖；非全量编辑权者改已发布文档会退回 pending）")
    public Result<PortalWikiRespVO> update(@Valid @RequestBody PortalWikiSaveReqVO updateReqVO) {
        return success(BeanUtils.toBean(
                wikiService.update(getLoginUserId(), updateReqVO.getId(), updateReqVO), PortalWikiRespVO.class));
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过")
    @SaCheckPermission(PortalCapability.REVIEW)
    public Result<Boolean> approve(@Valid @RequestBody PortalReviewReqVO reviewReqVO) {
        wikiService.review(getLoginUserId(), reviewReqVO.getId(), true, reviewReqVO.getComment());
        return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "审批驳回")
    @SaCheckPermission(PortalCapability.REVIEW)
    public Result<Boolean> reject(@Valid @RequestBody PortalReviewReqVO reviewReqVO) {
        wikiService.review(getLoginUserId(), reviewReqVO.getId(), false, reviewReqVO.getComment());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删文档（硬删，删除动作进操作日志）")
    @Parameter(name = "id", description = "文档编号", required = true)
    @SaCheckPermission(PortalCapability.DELETE)
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        wikiService.delete(getLoginUserId(), id);
        return success(true);
    }

}
