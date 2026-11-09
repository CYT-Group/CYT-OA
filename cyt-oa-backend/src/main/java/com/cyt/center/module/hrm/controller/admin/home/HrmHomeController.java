package com.cyt.center.module.hrm.controller.admin.home;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.controller.admin.home.vo.HrmHomeCalendarItemRespVO;
import com.cyt.center.module.hrm.controller.admin.home.vo.HrmHrHomeStatisticsRespVO;
import com.cyt.center.module.hrm.controller.admin.home.vo.HrmTeamHomeStatisticsRespVO;
import com.cyt.center.module.hrm.service.home.HrmHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - HRM 首页")
@RestController
@RequestMapping("/hrm/home")
@Validated
public class HrmHomeController {

    @Resource
    private HrmHomeService homeService;

    @GetMapping("/hr-statistics-summary")
    @Operation(summary = "获得 HR 工作台统计")
    @SaCheckPermission("hrm:home:hr-query")
    public Result<HrmHrHomeStatisticsRespVO> getHrHomeStatisticsSummary() {
        return success(homeService.getHrHomeStatisticsSummary());
    }

    @GetMapping("/hr-calendar")
    @Operation(summary = "获得 HR 工作台日历")
    @Parameters({
            @Parameter(name = "startDate", description = "开始日期", required = true, example = "2026-08-01"),
            @Parameter(name = "endDate", description = "结束日期", required = true, example = "2026-08-31")
    })
    @SaCheckPermission("hrm:home:hr-query")
    public Result<List<HrmHomeCalendarItemRespVO>> getHrHomeCalendar(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return success(homeService.getHrHomeCalendar(getLoginUserId(), startDate, endDate));
    }

    @GetMapping("/team-statistics-summary")
    @Operation(summary = "获得团队工作台统计")
    @SaCheckPermission("hrm:home:team-query")
    public Result<HrmTeamHomeStatisticsRespVO> getTeamHomeStatisticsSummary() {
        return success(homeService.getTeamHomeStatisticsSummary(getLoginUserId()));
    }

    @GetMapping("/team-calendar")
    @Operation(summary = "获得团队工作台日历")
    @Parameters({
            @Parameter(name = "startDate", description = "开始日期", required = true, example = "2026-08-01"),
            @Parameter(name = "endDate", description = "结束日期", required = true, example = "2026-08-31")
    })
    @SaCheckPermission("hrm:home:team-query")
    public Result<List<HrmHomeCalendarItemRespVO>> getTeamHomeCalendar(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return success(homeService.getTeamHomeCalendar(getLoginUserId(), startDate, endDate));
    }

}
