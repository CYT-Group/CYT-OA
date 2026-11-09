package com.cyt.center.module.me.service;

import cn.hutool.core.util.StrUtil;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.enums.CommonStatusEnum;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.approval.service.WecomApprovalBridge;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.me.model.vo.MeVos.ApplyItem;
import com.cyt.center.module.me.model.vo.MeVos.Option;
import com.cyt.center.module.oa.controller.admin.leave.vo.OaLeaveApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.overtime.vo.OaOvertimeApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementPageReqVO;
import com.cyt.center.module.oa.controller.admin.schedule.vo.OaSchedulePageReqVO;
import com.cyt.center.module.oa.controller.admin.seal.vo.OaSealPageReqVO;
import com.cyt.center.module.oa.controller.admin.seal.vo.apply.OaSealApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.supply.vo.item.OaSupplyItemPageReqVO;
import com.cyt.center.module.oa.controller.admin.supply.vo.apply.OaSupplyApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.travel.vo.apply.OaTravelApplyPageReqVO;
import com.cyt.center.module.oa.controller.admin.travel.vo.reimbursement.OaTravelReimbursementPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.leave.OaLeaveApplyDO;
import com.cyt.center.module.oa.dal.dataobject.overtime.OaOvertimeApplyDO;
import com.cyt.center.module.oa.dal.dataobject.reimbursement.OaReimbursementDO;
import com.cyt.center.module.oa.dal.dataobject.seal.OaSealApplyDO;
import com.cyt.center.module.oa.dal.dataobject.supply.OaSupplyApplyDO;
import com.cyt.center.module.oa.dal.dataobject.travel.OaTravelApplyDO;
import com.cyt.center.module.oa.dal.dataobject.travel.OaTravelReimbursementDO;
import com.cyt.center.module.oa.service.leave.OaLeaveApplyService;
import com.cyt.center.module.oa.service.overtime.OaOvertimeApplyService;
import com.cyt.center.module.oa.service.reimbursement.OaReimbursementService;
import com.cyt.center.module.oa.service.schedule.OaScheduleService;
import com.cyt.center.module.oa.service.seal.OaSealApplyService;
import com.cyt.center.module.oa.service.seal.OaSealService;
import com.cyt.center.module.oa.service.supply.OaSupplyApplyService;
import com.cyt.center.module.oa.service.supply.OaSupplyItemService;
import com.cyt.center.module.oa.service.travel.OaTravelApplyService;
import com.cyt.center.module.oa.service.travel.OaTravelReimbursementService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 我要办事。8 类单据仍写各自的 oa_ 表，这里只做「我发起的」跨类型统一投影和撤销转发；
 * 各类型的必填校验沿用移植过来的 SaveReqVO，不再复制一套个人中心专用入参。
 */
@Service
@RequiredArgsConstructor
public class MeApplyService {

    public static final String LEAVE = "leave";
    public static final String OVERTIME = "overtime";
    public static final String REIMBURSEMENT = "reimbursement";
    public static final String TRAVEL = "travel";
    public static final String TRAVEL_REIMBURSEMENT = "travel-reimbursement";
    public static final String SEAL = "seal";
    public static final String SUPPLY = "supply";
    public static final String SCHEDULE = "schedule";

    private static final Map<String, String> TYPE_TEXT = new LinkedHashMap<>();

    static {
        TYPE_TEXT.put(LEAVE, "请假");
        TYPE_TEXT.put(OVERTIME, "加班");
        TYPE_TEXT.put(REIMBURSEMENT, "报销");
        TYPE_TEXT.put(TRAVEL, "出差");
        TYPE_TEXT.put(TRAVEL_REIMBURSEMENT, "差旅报销");
        TYPE_TEXT.put(SEAL, "用章");
        TYPE_TEXT.put(SUPPLY, "物资领用");
        TYPE_TEXT.put(SCHEDULE, "日程");
    }

    private final OaLeaveApplyService leave;
    private final OaOvertimeApplyService overtime;
    private final OaReimbursementService reimbursement;
    private final OaTravelApplyService travel;
    private final OaTravelReimbursementService travelReimbursement;
    private final OaSealApplyService seal;
    private final OaSupplyApplyService supply;
    private final OaScheduleService schedule;
    private final OaSealService sealLedger;
    private final OaSupplyItemService itemLedger;
    private final WecomApprovalBridge bridge;
    private final AdminUserApi userApi;
    private final DeptApi deptApi;

    /** 台账下拉一次取满一页，公司印章与用品的量级远小于此 */
    private static final int LEDGER_PAGE_SIZE = 200;

    public static List<String> types() {
        return List.copyOf(TYPE_TEXT.keySet());
    }

    /** 用印表单里的印章下拉：取台账在用的章，员工不需要印章管理权限也能选。 */
    public List<Option> sealOptions() {
        OaSealPageReqVO reqVO = new OaSealPageReqVO();
        reqVO.setPageSize(LEDGER_PAGE_SIZE);
        return sealLedger.getSealPage(reqVO).getList().stream()
                .filter(seal -> seal.getStatus() == null || seal.getStatus() != 0)
                .map(seal -> new Option(seal.getId(), seal.getName(), seal.getNo()))
                .toList();
    }

    /** 领用表单里的用品下拉：带库存与单位，避免选出没货的。 */
    public List<Option> supplyItemOptions() {
        OaSupplyItemPageReqVO reqVO = new OaSupplyItemPageReqVO();
        reqVO.setPageSize(LEDGER_PAGE_SIZE);
        return itemLedger.getSupplyItemPage(reqVO).getList().stream()
                .filter(item -> item.getStatus() == null || item.getStatus() != 0)
                .map(item -> new Option(item.getId(), item.getName(),
                        (item.getStockQuantity() == null ? 0 : item.getStockQuantity())
                                + (item.getUnit() == null ? "" : " " + item.getUnit())))
                .toList();
    }

    /**
     * 请假自选审批人的候选人。审批单最终发在企业微信，所以只列在职且有企微账号的人；
     * 本人排除在外——自己审自己的请假单没有意义。部门跟在姓名后面，避免同名选错人。
     */
    public List<Option> approverCandidates(Long userId) {
        List<AdminUserRespDTO> candidates = new ArrayList<>();
        for (AdminUserRespDTO user : userApi.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus())) {
            if (StrUtil.isNotBlank(user.getUsername()) && !user.getId().equals(userId)) {
                candidates.add(user);
            }
        }
        Map<Long, DeptRespDTO> depts = deptApi.getDeptMap(candidates.stream()
                .map(AdminUserRespDTO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<Option> options = new ArrayList<>(candidates.size());
        for (AdminUserRespDTO user : candidates) {
            DeptRespDTO dept = depts.get(user.getDeptId());
            options.add(new Option(user.getId(), dept == null ? user.getNickname()
                    : user.getNickname() + "（" + dept.getName() + "）", null));
        }
        return options;
    }

    public static String typeText(String type) {
        return TYPE_TEXT.get(type);
    }

    /** 单类「我发起的」分页。日程无审批状态，status 对其无效。 */
    public PageResult<ApplyItem> page(Long userId, String type, Integer status, PageParam pageParam) {
        requireType(type);
        return switch (type) {
            case LEAVE -> {
                OaLeaveApplyPageReqVO reqVO = new OaLeaveApplyPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(leave.getLeaveApplyPage(userId, reqVO), row -> item(LEAVE, row.getId(),
                        row.getWecomApprovalNo(),
                        row.getTitle(), row.getStatus(), null, row.getCreateTime(),
                        row.getStartTime(), row.getEndTime(), row.getProcessInstanceId()));
            }
            case OVERTIME -> {
                OaOvertimeApplyPageReqVO reqVO = new OaOvertimeApplyPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(overtime.getOvertimeApplyPage(userId, reqVO), row -> item(OVERTIME, row.getId(), null,
                        row.getTitle(), row.getStatus(), null, row.getCreateTime(),
                        row.getStartTime(), row.getEndTime(), row.getProcessInstanceId()));
            }
            case REIMBURSEMENT -> {
                OaReimbursementPageReqVO reqVO = new OaReimbursementPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(reimbursement.getReimbursementPage(userId, reqVO), row -> item(REIMBURSEMENT,
                        row.getId(), null, row.getTitle(), row.getStatus(), row.getTotalPrice(), row.getCreateTime(),
                        null, null, row.getProcessInstanceId()));
            }
            case TRAVEL -> {
                OaTravelApplyPageReqVO reqVO = new OaTravelApplyPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(travel.getTravelApplyPage(userId, reqVO), row -> item(TRAVEL, row.getId(), row.getNo(),
                        row.getReason(), row.getStatus(), row.getEstimatedPrice(), row.getCreateTime(),
                        row.getStartTime(), row.getEndTime(), row.getProcessInstanceId()));
            }
            case TRAVEL_REIMBURSEMENT -> {
                OaTravelReimbursementPageReqVO reqVO = new OaTravelReimbursementPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(travelReimbursement.getTravelReimbursementPage(userId, reqVO),
                        row -> item(TRAVEL_REIMBURSEMENT, row.getId(), row.getNo(), row.getReason(), row.getStatus(),
                                row.getTotalPrice(), row.getCreateTime(), row.getStartTime(), row.getEndTime(),
                                row.getProcessInstanceId()));
            }
            case SEAL -> {
                OaSealApplyPageReqVO reqVO = new OaSealApplyPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(seal.getSealApplyPage(userId, reqVO), row -> item(SEAL, row.getId(), row.getNo(),
                        row.getDocumentTitle(), row.getStatus(), row.getContractPrice(), row.getCreateTime(),
                        row.getExpectedUseTime(), row.getExpectedReturnTime(), row.getProcessInstanceId()));
            }
            case SUPPLY -> {
                OaSupplyApplyPageReqVO reqVO = new OaSupplyApplyPageReqVO();
                copyPage(pageParam, reqVO);
                reqVO.setStatus(status);
                yield convert(supply.getSupplyApplyPage(reqVO, userId), row -> item(SUPPLY, row.getId(), row.getNo(),
                        row.getReason(), row.getStatus(), null, row.getCreateTime(),
                        row.getApplyTime(), null, row.getProcessInstanceId()));
            }
            default -> {
                OaSchedulePageReqVO reqVO = new OaSchedulePageReqVO();
                copyPage(pageParam, reqVO);
                yield convert(schedule.getMySchedulePage(reqVO, userId), row -> new ApplyItem(SCHEDULE, "日程",
                        row.getId(), null, row.getTitle(), null, null, null, row.getCreateTime(),
                        row.getStartTime(), row.getEndTime(), null));
            }
        };
    }

    /** 概览用：跨 8 类各取最新几条后按时间倒序合并。 */
    public List<ApplyItem> recent(Long userId, int limit) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(Math.max(1, Math.min(limit, 50)));
        List<ApplyItem> merged = new ArrayList<>();
        for (String type : types()) {
            merged.addAll(page(userId, type, null, pageParam).getList());
        }
        merged.sort(Comparator.comparing(ApplyItem::createTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return merged.size() > limit ? merged.subList(0, limit) : merged;
    }

    /** 撤销仍在流程中的申请单：审批单都在企业微信，网关没有暴露撤销操作，统一引导去企微撤回再回来同步。 */
    public void cancel(Long userId, String type, Long id, String reason) {
        requireType(type);
        if (LEAVE.equals(type)) {
            // 请假的审批单在企业微信，只能撤回原单再同步回 OA，不能在这里假装取消
            OaLeaveApplyDO application = require(leave.getLeaveApply(id), type);
            throw new BizException(StrUtil.isBlank(application.getWecomApprovalNo())
                    ? "该请假还没提交审批，无需撤销，可直接修改后重新提交"
                    : "请假审批单在企业微信里，请在企业微信撤回这张单，再回到本页点「同步审批状态」更新结果");
        }
        boolean submitted = switch (type) {
            case OVERTIME -> bridge.linked(OVERTIME, id);
            case REIMBURSEMENT -> bridge.linked(REIMBURSEMENT, id);
            case TRAVEL -> bridge.linked(TRAVEL, id);
            case TRAVEL_REIMBURSEMENT -> bridge.linked(TRAVEL_REIMBURSEMENT, id);
            case SEAL -> bridge.linked(SEAL, id);
            case SUPPLY -> bridge.linked(SUPPLY, id);
            default -> false;
        };
        if (!submitted) {
            throw new BizException("该申请还没提交审批，无需撤销，可直接修改后重新提交");
        }
        throw new BizException(typeText(type) + "审批单在企业微信里，请在企业微信撤回这张单，再回到本页点「同步审批状态」更新结果");
    }

    /** 从企业微信回读审批状态，返回刷新后的单条投影；日程没有审批，不适用。 */
    public ApplyItem sync(Long userId, String type, Long id) {
        requireType(type);
        return switch (type) {
            case LEAVE -> {
                OaLeaveApplyDO row = leave.syncLeaveApplyStatus(id, userId);
                yield item(LEAVE, row.getId(), row.getWecomApprovalNo(), row.getTitle(), row.getStatus(),
                        null, row.getCreateTime(), row.getStartTime(), row.getEndTime(), row.getProcessInstanceId());
            }
            case OVERTIME -> {
                OaOvertimeApplyDO row = overtime.syncOvertimeApplyStatus(id, userId);
                yield item(OVERTIME, row.getId(), null, row.getTitle(), row.getStatus(), null,
                        row.getCreateTime(), row.getStartTime(), row.getEndTime(), row.getProcessInstanceId());
            }
            case REIMBURSEMENT -> {
                OaReimbursementDO row = reimbursement.syncReimbursementStatus(id, userId);
                yield item(REIMBURSEMENT, row.getId(), null, row.getTitle(), row.getStatus(),
                        row.getTotalPrice(), row.getCreateTime(), null, null, row.getProcessInstanceId());
            }
            case TRAVEL -> {
                OaTravelApplyDO row = travel.syncTravelApplyStatus(id, userId);
                yield item(TRAVEL, row.getId(), row.getNo(), row.getReason(), row.getStatus(),
                        row.getEstimatedPrice(), row.getCreateTime(), row.getStartTime(), row.getEndTime(),
                        row.getProcessInstanceId());
            }
            case TRAVEL_REIMBURSEMENT -> {
                OaTravelReimbursementDO row = travelReimbursement.syncTravelReimbursementStatus(id, userId);
                yield item(TRAVEL_REIMBURSEMENT, row.getId(), row.getNo(), row.getReason(), row.getStatus(),
                        row.getTotalPrice(), row.getCreateTime(), row.getStartTime(), row.getEndTime(),
                        row.getProcessInstanceId());
            }
            case SEAL -> {
                OaSealApplyDO row = seal.syncSealApplyStatus(id, userId);
                yield item(SEAL, row.getId(), row.getNo(), row.getDocumentTitle(), row.getStatus(),
                        row.getContractPrice(), row.getCreateTime(), row.getExpectedUseTime(),
                        row.getExpectedReturnTime(), row.getProcessInstanceId());
            }
            case SUPPLY -> {
                OaSupplyApplyDO row = supply.syncSupplyApplyStatus(id, userId);
                yield item(SUPPLY, row.getId(), row.getNo(), row.getReason(), row.getStatus(), null,
                        row.getCreateTime(), row.getApplyTime(), null, row.getProcessInstanceId());
            }
            default -> throw new BizException("日程没有审批状态，无需同步");
        };
    }

    private static <T> PageResult<ApplyItem> convert(PageResult<T> source,
                                                     java.util.function.Function<T, ApplyItem> mapper) {
        List<ApplyItem> list = new ArrayList<>(source.getList().size());
        for (T row : source.getList()) {
            list.add(mapper.apply(row));
        }
        return new PageResult<>(list, source.getTotal());
    }

    private static <T> T require(T row, String type) {
        if (row == null) {
            throw new BizException(typeText(type) + "申请不存在");
        }
        return row;
    }

    private static ApplyItem item(String type, Long id, String no, String title, Integer status,
                                  BigDecimal amount, LocalDateTime createTime,
                                  LocalDateTime startTime, LocalDateTime endTime, String processInstanceId) {
        BpmProcessInstanceStatusEnum statusEnum = BpmProcessInstanceStatusEnum.valueOf(status);
        return new ApplyItem(type, TYPE_TEXT.get(type), id, no, title, status,
                statusEnum == null ? null : statusEnum.getDesc(),
                amount == null ? null : amount.toPlainString(), createTime, startTime, endTime, processInstanceId);
    }

    private static void requireType(String type) {
        if (!TYPE_TEXT.containsKey(type)) {
            throw new BizException("办事类型不正确：" + type);
        }
    }

    private static void copyPage(PageParam source, PageParam target) {
        target.setPageNo(source.getPageNo());
        target.setPageSize(source.getPageSize());
    }
}
