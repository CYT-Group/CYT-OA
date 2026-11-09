package com.cyt.center.module.portal.controller.app.analytics;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalTrackReqVO;
import com.cyt.center.module.portal.service.analytics.PortalAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 页面浏览埋点上报。展示端每次进页面打一次，登录即可、不要求任何能力位。
 *
 * user-agent 取请求头而不是请求体：那是浏览器自己填的字段，放在 body 里只会诱导伪造。
 */
@Tag(name = "门户 - 页面浏览埋点上报")
@RestController
@RequestMapping("/api/portal/analytics")
@Validated
@RequiredArgsConstructor
public class PortalTrackController {

    private final PortalAnalyticsService analyticsService;

    @PostMapping("/track")
    @Operation(summary = "记一次页面浏览（姓名 / 部门按当前登录人快照写入）")
    public Result<Boolean> track(@Valid @RequestBody PortalTrackReqVO trackReqVO,
                                 @RequestHeader(value = "user-agent", required = false) String userAgent) {
        analyticsService.track(getLoginUserId(), trackReqVO, userAgent);
        return success(true);
    }

}
