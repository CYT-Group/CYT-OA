package com.cyt.center.module.portal.controller.app.esports;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsActionReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingCreateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsRejectReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsUpdateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsUserRespVO;
import com.cyt.center.module.portal.service.esports.PortalEsportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 电竞室 Open API（迁自 Portal 的 routers/esports.py）。
 *
 * 全员自助预约 + 设置驱动多级审批。管理/审批/全量查看的能力判定在服务层（对齐契约「可见性按能力收敛」），
 * 不做 /admin-api 分叉。通知（wecom_sync）依赖 OA 尚未就绪的企微推送，本切片不接。
 */
@Tag(name = "门户 - 电竞室")
@RestController
@RequestMapping("/api/portal/esports")
@Validated
@RequiredArgsConstructor
public class PortalEsportsOpenController {

    private final PortalEsportsService esportsService;

    @GetMapping("/settings")
    @Operation(summary = "读开放规则（含当前登录人的 can_manage / can_view_all / can_review）")
    public Result<EsportsSettingsRespVO> settings() {
        return success(esportsService.readSettings(getLoginUserId()));
    }

    @PutMapping("/settings")
    @Operation(summary = "管理者设置开放开关、营业规则和之后新单的审批人")
    public Result<EsportsSettingsRespVO> updateSettings(@Valid @RequestBody EsportsSettingsUpdateReqVO reqVO) {
        return success(esportsService.updateSettings(getLoginUserId(), reqVO));
    }

    @GetMapping("/approvers")
    @Operation(summary = "可配置的在职审批账号（管理者）")
    public Result<List<EsportsUserRespVO>> approvers() {
        return success(esportsService.listApprovers(getLoginUserId()));
    }

    @GetMapping("/participants")
    @Operation(summary = "可选的参加人员（在职账号最小字段）")
    public Result<List<EsportsUserRespVO>> participants() {
        return success(esportsService.listParticipants(getLoginUserId()));
    }

    @GetMapping("/bookings")
    @Operation(summary = "按日历 / 本人 / 审批快照 / 全量范围查询预约")
    public Result<List<EsportsBookingRespVO>> bookings(
            @Parameter(description = "开始日期 yyyy-MM-dd", required = true)
            @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @Parameter(description = "结束日期 yyyy-MM-dd", required = true)
            @RequestParam("dateTo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @Parameter(description = "范围：calendar / mine / review / all")
            @RequestParam(value = "scope", required = false) String scope,
            @Parameter(description = "状态过滤")
            @RequestParam(value = "status", required = false) String status,
            @Parameter(description = "标题 / 申请人搜索")
            @RequestParam(value = "q", required = false) String q) {
        return success(esportsService.listBookings(getLoginUserId(), dateFrom, dateTo, scope, status, q));
    }

    @PostMapping("/bookings")
    @Operation(summary = "提交预约（待审批时已占用整间房）")
    public Result<EsportsBookingRespVO> createBooking(@Valid @RequestBody EsportsBookingCreateReqVO reqVO) {
        return success(esportsService.createBooking(getLoginUserId(), reqVO));
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(summary = "预约详情（参与人看完整，其他人只看占位）")
    public Result<EsportsBookingRespVO> bookingDetail(@PathVariable("bookingId") Long bookingId) {
        return success(esportsService.bookingDetail(getLoginUserId(), bookingId));
    }

    @PostMapping("/bookings/{bookingId}/approve")
    @Operation(summary = "通过当前环节")
    public Result<EsportsBookingRespVO> approve(@PathVariable("bookingId") Long bookingId,
                                                @Valid @RequestBody EsportsActionReqVO reqVO) {
        return success(esportsService.actOnBooking(getLoginUserId(), bookingId, "approve",
                reqVO.getRemark(), reqVO.getExpectedStageIndex()));
    }

    @PostMapping("/bookings/{bookingId}/reject")
    @Operation(summary = "驳回（须留原因）")
    public Result<EsportsBookingRespVO> reject(@PathVariable("bookingId") Long bookingId,
                                               @Valid @RequestBody EsportsRejectReqVO reqVO) {
        return success(esportsService.actOnBooking(getLoginUserId(), bookingId, "reject",
                reqVO.getRemark(), reqVO.getExpectedStageIndex()));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    @Operation(summary = "取消尚未开始的预约")
    public Result<EsportsBookingRespVO> cancel(@PathVariable("bookingId") Long bookingId,
                                               @Valid @RequestBody EsportsActionReqVO reqVO) {
        return success(esportsService.actOnBooking(getLoginUserId(), bookingId, "cancel",
                reqVO.getRemark(), reqVO.getExpectedStageIndex()));
    }

}
