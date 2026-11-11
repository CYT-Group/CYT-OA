package com.cyt.center.module.me.controller;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.me.model.vo.MeVos.ApplyItem;
import com.cyt.center.module.me.model.vo.MeVos.Option;
import com.cyt.center.module.me.model.vo.MeApplySyncReqVO;
import com.cyt.center.module.me.service.MeApplyService;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplyRespVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySubmitReqVO;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplySyncReqVO;
import com.cyt.center.module.oa.dal.dataobject.leave.OaLeaveApplyDO;
import com.cyt.center.module.oa.controller.admin.overtime.vo.OaOvertimeApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.overtime.vo.OaOvertimeApplySubmitReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementSaveReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementSubmitReqVO;
import com.cyt.center.module.oa.controller.admin.schedule.vo.OaScheduleSaveReqVO;
import com.cyt.center.module.oa.controller.admin.supply.vo.apply.OaSupplyApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.travel.vo.apply.OaTravelApplySaveReqVO;
import com.cyt.center.module.oa.controller.admin.travel.vo.apply.OaTravelApplySubmitReqVO;
import com.cyt.center.module.oa.controller.admin.travel.vo.reimbursement.OaTravelReimbursementSaveReqVO;
import com.cyt.center.module.oa.controller.admin.travel.vo.reimbursement.OaTravelReimbursementSubmitReqVO;
import com.cyt.center.module.oa.service.leave.OaLeaveApplyService;
import com.cyt.center.module.oa.service.overtime.OaOvertimeApplyService;
import com.cyt.center.module.oa.service.reimbursement.OaReimbursementService;
import com.cyt.center.module.oa.service.schedule.OaScheduleService;
import com.cyt.center.module.oa.service.supply.OaSupplyApplyService;
import com.cyt.center.module.oa.service.travel.OaTravelApplyService;
import com.cyt.center.module.oa.service.travel.OaTravelReimbursementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 我要办事。入参直接用各业务模块已有的 SaveReqVO，避免个人中心再复制一套字段；
 * 先落草稿拿 id，再 submit 起流程，撤销统一走流程实例。
 */
@Tag(name = "个人中心 - 我要办事")
@RestController
@RequestMapping("/api/me/applies")
public class MeApplyController {

    @Resource
    private MeApplyService meApplyService;
    @Resource
    private OaLeaveApplyService leaveApplyService;
    @Resource
    private OaOvertimeApplyService overtimeApplyService;
    @Resource
    private OaReimbursementService reimbursementService;
    @Resource
    private OaTravelApplyService travelApplyService;
    @Resource
    private OaTravelReimbursementService travelReimbursementService;
    @Resource
    private OaSupplyApplyService supplyApplyService;
    @Resource
    private OaScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "我发起的单据分页，type 见 /api/me/applies/types")
    public Result<PageResult<ApplyItem>> page(@RequestParam("type") String type,
                                              @RequestParam(value = "status", required = false) Integer status,
                                              @Valid PageParam pageParam) {
        return success(meApplyService.page(getLoginUserId(), type, status, pageParam));
    }

    @GetMapping("/types")
    @Operation(summary = "可发起的单据类型")
    public Result<List<String>> types() {
        return success(MeApplyService.types());
    }

    @GetMapping("/recent")
    @Operation(summary = "跨类型最近几条，概览页用")
    public Result<List<ApplyItem>> recent(@RequestParam(value = "limit", defaultValue = "6") int limit) {
        return success(meApplyService.recent(getLoginUserId(), limit));
    }

    @GetMapping("/supply-items")
    @Operation(summary = "办公用品下拉，hint 给库存与单位")
    public Result<List<Option>> supplyItems() {
        return success(meApplyService.supplyItemOptions());
    }

    @GetMapping("/approvers")
    @Operation(summary = "请假自选审批人的候选人：在职且有企业微信账号的同事")
    public Result<List<Option>> approvers() {
        return success(meApplyService.approverCandidates(getLoginUserId()));
    }

    @DeleteMapping("/{type}/{id}")
    @Operation(summary = "撤销仍在审批中的单据；未提交的草稿保留在原表里")
    public Result<Boolean> cancel(@PathVariable("type") String type, @PathVariable("id") Long id,
                                  @RequestParam(value = "reason", required = false) String reason) {
        meApplyService.cancel(getLoginUserId(), type, id, reason);
        return success(true);
    }

    @PutMapping("/{type}/sync")
    @Operation(summary = "从企业微信回读审批状态，type 见 /api/me/applies/types（日程没有审批，不适用）")
    public Result<ApplyItem> sync(@PathVariable("type") String type,
                                  @Valid @RequestBody MeApplySyncReqVO reqVO) {
        return success(meApplyService.sync(getLoginUserId(), type, reqVO.getId()));
    }

    @PostMapping("/leave")
    @Operation(summary = "发起请假")
    public Result<Long> createLeave(@Valid @RequestBody OaLeaveApplySaveReqVO reqVO) {
        return success(leaveApplyService.createLeaveApply(reqVO));
    }

    @PutMapping("/leave/submit")
    @Operation(summary = "提交请假进入企业微信审批")
    public Result<Boolean> submitLeave(@Valid @RequestBody OaLeaveApplySubmitReqVO reqVO) {
        leaveApplyService.submitLeaveApply(reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/leave/sync")
    @Operation(summary = "从企业微信回读请假的审批状态")
    public Result<OaLeaveApplyRespVO> syncLeave(@Valid @RequestBody OaLeaveApplySyncReqVO reqVO) {
        OaLeaveApplyDO application = leaveApplyService.syncLeaveApplyStatus(reqVO.getId(), getLoginUserId());
        return success(BeanUtils.toBean(application, OaLeaveApplyRespVO.class));
    }

    @PostMapping("/overtime")
    @Operation(summary = "发起加班")
    public Result<Long> createOvertime(@Valid @RequestBody OaOvertimeApplySaveReqVO reqVO) {
        return success(overtimeApplyService.createOvertimeApply(reqVO));
    }

    @PutMapping("/overtime/submit")
    @Operation(summary = "提交加班进入审批")
    public Result<Boolean> submitOvertime(@Valid @RequestBody OaOvertimeApplySubmitReqVO reqVO) {
        overtimeApplyService.submitOvertimeApply(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/reimbursement")
    @Operation(summary = "发起费用报销")
    public Result<Long> createReimbursement(@Valid @RequestBody OaReimbursementSaveReqVO reqVO) {
        return success(reimbursementService.createReimbursement(reqVO));
    }

    @PutMapping("/reimbursement/submit")
    @Operation(summary = "提交报销进入审批")
    public Result<Boolean> submitReimbursement(@Valid @RequestBody OaReimbursementSubmitReqVO reqVO) {
        reimbursementService.submitReimbursement(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/travel")
    @Operation(summary = "发起出差申请")
    public Result<Long> createTravel(@Valid @RequestBody OaTravelApplySaveReqVO reqVO) {
        return success(travelApplyService.createTravelApply(reqVO, getLoginUserId()));
    }

    @PutMapping("/travel/submit")
    @Operation(summary = "提交出差申请进入审批")
    public Result<Boolean> submitTravel(@Valid @RequestBody OaTravelApplySubmitReqVO reqVO) {
        travelApplyService.submitTravelApply(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/travel-reimbursement")
    @Operation(summary = "发起差旅报销")
    public Result<Long> createTravelReimbursement(@Valid @RequestBody OaTravelReimbursementSaveReqVO reqVO) {
        return success(travelReimbursementService.createTravelReimbursement(reqVO, getLoginUserId()));
    }

    @PutMapping("/travel-reimbursement/submit")
    @Operation(summary = "提交差旅报销进入审批")
    public Result<Boolean> submitTravelReimbursement(@Valid @RequestBody OaTravelReimbursementSubmitReqVO reqVO) {
        travelReimbursementService.submitTravelReimbursement(reqVO, getLoginUserId());
        return success(true);
    }

    @PostMapping("/supply")
    @Operation(summary = "发起物资领用")
    public Result<Long> createSupply(@Valid @RequestBody OaSupplyApplySaveReqVO reqVO) {
        return success(supplyApplyService.createSupplyApply(reqVO, getLoginUserId()));
    }

    @PutMapping("/supply/submit")
    @Operation(summary = "提交物资领用进入审批")
    public Result<Boolean> submitSupply(@RequestParam("id") Long id) {
        supplyApplyService.submitSupplyApply(id, getLoginUserId());
        return success(true);
    }

    @PostMapping("/schedule")
    @Operation(summary = "创建日程，可同时指派参与人（团队里的「派任务 / 催一催」也用它）")
    public Result<Long> createSchedule(@Valid @RequestBody OaScheduleSaveReqVO reqVO) {
        return success(scheduleService.createSchedule(reqVO, getLoginUserId()));
    }
}
