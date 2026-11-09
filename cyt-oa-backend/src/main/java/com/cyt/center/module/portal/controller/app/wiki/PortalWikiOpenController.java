package com.cyt.center.module.portal.controller.app.wiki;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiDetailRespVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiPageReqVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiRespVO;
import com.cyt.center.module.portal.service.wiki.PortalWikiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 知识库读侧接口（登录即可）。
 *
 * 与 /admin-api/portal/wiki 跑同两条用例，差别只在令牌：管理令牌要求 device=admin，
 * 门户的登录人拿的是普通令牌，读侧必须走 /api/**。详情每次调用自增浏览量，两边都算数。
 */
@Tag(name = "门户 - 知识库（读）")
@RestController
@RequestMapping("/api/portal/wiki")
@Validated
@RequiredArgsConstructor
public class PortalWikiOpenController {

    private final PortalWikiService wikiService;

    @GetMapping("/page")
    @Operation(summary = "分页列文档（默认 published）")
    public Result<PageResult<PortalWikiRespVO>> page(@Valid PortalWikiPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(wikiService.page(getLoginUserId(), pageReqVO), PortalWikiRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "单篇详情，含正文；每次调用自增浏览量")
    @Parameter(name = "id", description = "文档编号", required = true)
    public Result<PortalWikiDetailRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(wikiService.getForRead(getLoginUserId(), id), PortalWikiDetailRespVO.class));
    }

}
