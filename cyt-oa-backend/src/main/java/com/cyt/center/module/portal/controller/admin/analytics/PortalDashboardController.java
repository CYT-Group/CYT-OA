package com.cyt.center.module.portal.controller.admin.analytics;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalActiveUserRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalAnalyticsRangeReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalAnalyticsSummaryRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalAnalyticsTopReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalDailyTrendRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalPopularPageRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalUserHistoryReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalUserHistoryRespVO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.analytics.PortalAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;

/**
 * 浏览看板的查询端点，每个都要 can_view_dashboards——Portal 的 require(can_view_dashboards) 同款整端点闸。
 *
 * 数据来自 /api/portal/analytics/track 上报的 portal_page_views；上报端点在 app 侧，
 * 那是展示页每次进页面都要打的点，不属于管理面。
 */
@Tag(name = "管理后台 - 门户浏览看板")
@RestController
@RequestMapping("/portal/analytics")
@Validated
@RequiredArgsConstructor
public class PortalDashboardController {

    private final PortalAnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "总览：窗口 PV/UV、今日 PV/UV、活跃页面数")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<PortalAnalyticsSummaryRespVO> summary(@Valid PortalAnalyticsRangeReqVO rangeReqVO) {
        return success(analyticsService.summary(rangeReqVO));
    }

    @GetMapping("/popular-pages")
    @Operation(summary = "热门页面 Top N（按 PV 倒序）")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<List<PortalPopularPageRespVO>> popularPages(@Valid PortalAnalyticsTopReqVO topReqVO) {
        return success(analyticsService.popularPages(topReqVO));
    }

    @GetMapping("/daily-trend")
    @Operation(summary = "每日 PV/UV 趋势（稀疏序列，没人访问的日期缺席）")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<List<PortalDailyTrendRespVO>> dailyTrend(@Valid PortalAnalyticsRangeReqVO rangeReqVO) {
        return success(analyticsService.dailyTrend(rangeReqVO));
    }

    @GetMapping("/active-users")
    @Operation(summary = "活跃用户 Top N（匿名访问不计）")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<List<PortalActiveUserRespVO>> activeUsers(@Valid PortalAnalyticsTopReqVO topReqVO) {
        return success(analyticsService.activeUsers(topReqVO));
    }

    @GetMapping("/user-history")
    @Operation(summary = "某个人的浏览轨迹")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<List<PortalUserHistoryRespVO>> userHistory(@Valid PortalUserHistoryReqVO historyReqVO) {
        return success(analyticsService.userHistory(historyReqVO));
    }

}
