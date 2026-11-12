package com.cyt.center.module.portal.service.esports;

import com.cyt.center.module.portal.controller.app.esports.vo.EsportsApprovalStepRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsApproverEntryRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsApproverStageRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingCreateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingLogRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsRespVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsUpdateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsUserRespVO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsApprovalStepDO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsBookingDO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsBookingLogDO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsSettingsDO;
import com.cyt.center.module.portal.dal.postgresql.esports.PortalEsportsApprovalStepMapper;
import com.cyt.center.module.portal.dal.postgresql.esports.PortalEsportsBookingLogMapper;
import com.cyt.center.module.portal.dal.postgresql.esports.PortalEsportsBookingMapper;
import com.cyt.center.module.portal.dal.postgresql.esports.PortalEsportsSettingsMapper;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.domain.esports.EsportsRules;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_BOOKING_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_BOOKING_NOT_FOUND;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_CONFLICT;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_FORBIDDEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_SETTINGS_INVALID;

/**
 * 电竞室用例实现（对齐 Portal 的 application/esports.py + domain/esports.py）。
 *
 * 锁序固定 settings→booking→steps（串行化整间房预约）；审批环节随新单保存快照，第一环节立即占位。
 * 通知（wecom_sync）依赖 OA 尚未就绪的企微推送通道，本切片不接，随推送切片落地。
 */
@Service
public class PortalEsportsServiceImpl implements PortalEsportsService {

    private static final Integer STATUS_ACTIVE = 0;

    @Resource
    private PortalEsportsSettingsMapper settingsMapper;
    @Resource
    private PortalEsportsBookingMapper bookingMapper;
    @Resource
    private PortalEsportsApprovalStepMapper stepMapper;
    @Resource
    private PortalEsportsBookingLogMapper logMapper;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PortalAccess access;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    // ══════════════ 设置 ══════════════

    @Override
    public EsportsSettingsRespVO readSettings(Long userId) {
        operators.resolve(userId);
        PortalEsportsSettingsDO row = settingsMapper.selectSettings();
        return toSettingsVO(row, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EsportsSettingsRespVO updateSettings(Long userId, EsportsSettingsUpdateReqVO reqVO) {
        PortalOperator operator = operators.resolve(userId);
        requireManage(userId);
        PortalEsportsSettingsDO row = settingsMapper.selectForUpdate();
        EsportsRules.validateSettings(reqVO);
        EsportsRules.SettingsPlan plan = EsportsRules.requestedPlan(reqVO);
        EsportsRules.validatePlan(plan, Boolean.TRUE.equals(reqVO.getIsOpen()));

        List<Long> ids = plan.stages().stream()
                .flatMap(s -> EsportsRules.approverIds(s).stream()).distinct().toList();
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(ids);
        if (users.size() != ids.size()) {
            throw exception(ESPORTS_SETTINGS_INVALID, "审批人账号不存在");
        }
        if (Boolean.TRUE.equals(reqVO.getIsOpen())
                && users.values().stream().anyMatch(u -> !STATUS_ACTIVE.equals(u.getStatus()))) {
            throw exception(ESPORTS_SETTINGS_INVALID, "开放预约前请配置有效的在职审批人");
        }

        row.setIsOpen(reqVO.getIsOpen());
        row.setOpenWeekdays(reqVO.getOpenWeekdays());
        row.setOpenStartTime(reqVO.getOpenStartTime());
        row.setOpenEndTime(reqVO.getOpenEndTime());
        row.setAdvanceDays(reqVO.getAdvanceDays());
        row.setMaxDurationMinutes(reqVO.getMaxDurationMinutes());
        row.setApprovalMode(plan.mode());
        row.setApprovalStages(plan.stages());
        row.setApproverUserId(plan.stages().isEmpty() ? null : EsportsRules.approverIds(plan.stages().get(0)).get(0));
        row.setUpdatedBy(operator.username());
        row.setUpdatedAt(LocalDateTime.now());
        settingsMapper.updateById(row);
        return toSettingsVO(row, userId);
    }

    // ══════════════ 账号选择 ══════════════

    @Override
    public List<EsportsUserRespVO> listApprovers(Long userId) {
        operators.resolve(userId);
        requireManage(userId);
        return activeUsersVO();
    }

    @Override
    public List<EsportsUserRespVO> listParticipants(Long userId) {
        operators.resolve(userId);
        return activeUsersVO();
    }

    // ══════════════ 预约列表 / 详情 ══════════════

    @Override
    public List<EsportsBookingRespVO> listBookings(Long userId, LocalDate dateFrom, LocalDate dateTo,
                                                   String scope, String status, String q) {
        operators.resolve(userId);
        EsportsRules.validateRange(dateFrom, dateTo);
        boolean manage = canManage(userId);
        if ("all".equals(scope) && !canViewAll(userId)) {
            throw exception(ESPORTS_FORBIDDEN, "没有查看全部电竞室预约的权限");
        }
        List<PortalEsportsBookingDO> rows = bookingMapper.selectList(dateFrom, dateTo, status, q);
        List<Long> ids = rows.stream().map(PortalEsportsBookingDO::getId).toList();
        List<PortalEsportsApprovalStepDO> steps = stepMapper.selectByBookingIds(ids);
        List<PortalEsportsBookingLogDO> logs = "calendar".equals(scope)
                ? List.of() : logMapper.selectByBookingIds(ids);

        boolean privateView = !"calendar".equals(scope);
        List<EsportsBookingRespVO> out = new ArrayList<>();
        for (PortalEsportsBookingDO row : rows) {
            if (!scopeVisible(scope, manage, row, steps, userId)) {
                continue;
            }
            out.add(toBookingVO(row, steps, logs, userId, privateView));
        }
        return out;
    }

    @Override
    public EsportsBookingRespVO bookingDetail(Long userId, Long bookingId) {
        operators.resolve(userId);
        PortalEsportsBookingDO row = bookingMapper.selectBookingById(bookingId);
        if (row == null) {
            throw exception(ESPORTS_BOOKING_NOT_FOUND);
        }
        List<PortalEsportsApprovalStepDO> steps = stepMapper.selectByBookingIds(List.of(row.getId()));
        boolean privateView = privateBooking(row, steps, userId);
        if (!privateView && !EsportsRules.OCCUPYING_STATUSES.contains(row.getStatus())) {
            throw exception(ESPORTS_BOOKING_NOT_FOUND);
        }
        List<PortalEsportsBookingLogDO> logs = privateView ? logMapper.selectByBookingIds(List.of(row.getId())) : List.of();
        return toBookingVO(row, steps, logs, userId, privateView);
    }

    // ══════════════ 创建 / 动作 ══════════════

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EsportsBookingRespVO createBooking(Long userId, EsportsBookingCreateReqVO reqVO) {
        PortalOperator operator = operators.resolve(userId);
        int participantCount = EsportsRules.participantCountFor(reqVO);
        PortalEsportsSettingsDO settings = settingsMapper.selectForUpdate();
        EsportsRules.validateBooking(reqVO, settings);
        EsportsRules.SettingsPlan plan = EsportsRules.settingsPlan(settings);
        EsportsRules.validatePlan(plan, true);

        List<Long> approverIds = plan.stages().stream()
                .flatMap(s -> EsportsRules.approverIds(s).stream()).distinct().toList();
        Map<Long, AdminUserRespDTO> approvers = adminUserApi.getUserMap(approverIds);
        if (approvers.size() != approverIds.size()
                || approvers.values().stream().anyMatch(u -> !STATUS_ACTIVE.equals(u.getStatus()))) {
            throw exception(ESPORTS_BOOKING_INVALID, "审批人未配置或已停用，请联系管理员");
        }

        List<Map<String, Object>> participants = null;
        if (reqVO.getParticipantUserIds() != null) {
            Map<Long, AdminUserRespDTO> people = adminUserApi.getUserMap(reqVO.getParticipantUserIds());
            if (people.size() != reqVO.getParticipantUserIds().size()
                    || people.values().stream().anyMatch(u -> !STATUS_ACTIVE.equals(u.getStatus()))) {
                throw exception(ESPORTS_BOOKING_INVALID, "参加人员账号不存在或已停用");
            }
            participants = new ArrayList<>();
            for (Long uid : reqVO.getParticipantUserIds()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("user_id", uid);
                p.put("display_name", displayName(people.get(uid)));
                participants.add(p);
            }
        }

        if (bookingMapper.selectOverlapping(reqVO.getBookingDate(), reqVO.getStartTime(),
                reqVO.getEndTime(), null) != null) {
            throw exception(ESPORTS_CONFLICT, "该时段已被预约，请选择其他时间");
        }

        AdminUserRespDTO first = approvers.get(EsportsRules.approverIds(plan.stages().get(0)).get(0));
        LocalDateTime now = LocalDateTime.now();
        PortalEsportsBookingDO row = new PortalEsportsBookingDO();
        row.setBookingDate(reqVO.getBookingDate());
        row.setStartTime(reqVO.getStartTime());
        row.setEndTime(reqVO.getEndTime());
        row.setTitle(reqVO.getTitle());
        row.setPurpose(reqVO.getPurpose());
        row.setParticipantCount(participantCount);
        row.setParticipants(participants);
        row.setApplicantUserId(userId);
        row.setApplicantName(operator.displayNameOrUsername());
        row.setApproverUserId(first.getId());
        row.setApproverName(displayName(first));
        row.setApprovalMode(plan.mode());
        row.setCurrentStageIndex(0);
        row.setStatus("pending");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        bookingMapper.insert(row);

        List<PortalEsportsApprovalStepDO> steps = new ArrayList<>();
        for (int index = 0; index < plan.stages().size(); index++) {
            for (Long uid : EsportsRules.approverIds(plan.stages().get(index))) {
                AdminUserRespDTO person = approvers.get(uid);
                PortalEsportsApprovalStepDO step = new PortalEsportsApprovalStepDO();
                step.setBookingId(row.getId());
                step.setStageIndex(index);
                step.setApproverUserId(uid);
                step.setApproverName(displayName(person));
                step.setStatus(index == 0 ? "pending" : "waiting");
                stepMapper.insert(step);
                steps.add(step);
            }
        }

        PortalEsportsBookingLogDO log = new PortalEsportsBookingLogDO();
        log.setBookingId(row.getId());
        log.setOperation("submit");
        log.setActorUserId(userId);
        log.setActorName(operator.displayNameOrUsername());
        log.setCreatedAt(now);
        logMapper.insert(log);

        return toBookingVO(row, steps, List.of(log), userId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EsportsBookingRespVO actOnBooking(Long userId, Long bookingId, String operation,
                                             String remark, Integer expectedStageIndex) {
        PortalOperator operator = operators.resolve(userId);
        settingsMapper.selectForUpdate();
        PortalEsportsBookingDO row = bookingMapper.selectByIdForUpdate(bookingId);
        if (row == null) {
            throw exception(ESPORTS_BOOKING_NOT_FOUND);
        }
        List<PortalEsportsApprovalStepDO> steps = stepMapper.selectByBookingIdsForUpdate(List.of(bookingId));
        List<PortalEsportsApprovalStepDO> pending = currentSteps(row, steps);

        if (!"cancel".equals(operation)) {
            List<PortalEsportsApprovalStepDO> own = effectiveSteps(row, steps);
            long stageCount = own.stream().map(PortalEsportsApprovalStepDO::getStageIndex).distinct().count();
            if (stageCount > 1 && expectedStageIndex == null) {
                throw exception(ESPORTS_BOOKING_INVALID, "请刷新审批详情后重试");
            }
            Integer currentIndex = own.get(0).getId() == null
                    ? ("pending".equals(row.getStatus()) ? 0 : null)
                    : row.getCurrentStageIndex();
            if (expectedStageIndex != null && !expectedStageIndex.equals(currentIndex)) {
                throw exception(ESPORTS_CONFLICT, "审批环节已变化，请刷新后重试");
            }
        }

        if (!"cancel".equals(operation) && !"pending".equals(row.getStatus()) && privateBooking(row, steps, userId)) {
            EsportsRules.validateTransition(row, operation);
        }

        if ("cancel".equals(operation)) {
            if (!row.getApplicantUserId().equals(userId) && !canManage(userId)) {
                throw exception(ESPORTS_FORBIDDEN, "只能取消自己的预约");
            }
        } else if (!canReview(userId, pending.stream().map(PortalEsportsApprovalStepDO::getApproverUserId).toList())) {
            throw exception(ESPORTS_FORBIDDEN, "只有当前环节审批人或管理员可以审批");
        }

        EsportsRules.validateTransition(row, operation);
        steps = materializeLegacySteps(row, steps);

        if ("approve".equals(operation) && bookingMapper.selectOverlapping(row.getBookingDate(),
                row.getStartTime(), row.getEndTime(), row.getId()) != null) {
            throw exception(ESPORTS_CONFLICT, "预约时段冲突，请刷新后处理");
        }

        LocalDateTime now = LocalDateTime.now();
        row.setUpdatedAt(now);
        if ("cancel".equals(operation)) {
            row.setStatus("cancelled");
            row.setCurrentStageIndex(null);
            row.setCancelledByUserId(userId);
            row.setCancelledAt(now);
            for (PortalEsportsApprovalStepDO step : steps) {
                if ("waiting".equals(step.getStatus()) || "pending".equals(step.getStatus())) {
                    step.setStatus("skipped");
                }
            }
        } else {
            pending = EsportsRules.pendingSteps(row, steps);
            if (pending.isEmpty()) {
                throw exception(ESPORTS_CONFLICT, "当前审批环节已变化，请刷新后重试");
            }
            PortalEsportsApprovalStepDO selected = pending.stream()
                    .filter(s -> s.getApproverUserId().equals(userId))
                    .findFirst().orElse(pending.get(0));
            EsportsRules.transitionApproval(row, steps, selected, operation, userId,
                    operator.displayNameOrUsername(), remark, now);
            if ("approved".equals(row.getStatus()) || "rejected".equals(row.getStatus())) {
                row.setReviewedByUserId(userId);
                row.setReviewedByName(operator.displayNameOrUsername());
                row.setReviewRemark(remark);
                row.setReviewedAt(now);
            }
        }

        PortalEsportsBookingLogDO log = new PortalEsportsBookingLogDO();
        log.setBookingId(row.getId());
        log.setOperation(operation);
        log.setActorUserId(userId);
        log.setActorName(operator.displayNameOrUsername());
        log.setRemark(remark);
        log.setCreatedAt(now);

        bookingMapper.updateById(row);
        for (PortalEsportsApprovalStepDO step : steps) {
            if (step.getId() != null) {
                stepMapper.updateById(step);
            }
        }
        logMapper.insert(log);

        List<PortalEsportsBookingLogDO> logs = logMapper.selectByBookingIds(List.of(row.getId()));
        return toBookingVO(row, steps, logs, userId, true);
    }

    // ══════════════ 权限 / 用户 ══════════════

    private boolean canManage(Long userId) {
        return access.has(userId, PortalCapability.MANAGE_ESPORTS);
    }

    private boolean canViewAll(Long userId) {
        return access.has(userId, PortalCapability.VIEW_ALL_ESPORTS);
    }

    private boolean canReview(Long userId, List<Long> approverIds) {
        return canManage(userId) || (userId != null && approverIds.contains(userId));
    }

    private void requireManage(Long userId) {
        if (!canManage(userId)) {
            throw exception(ESPORTS_FORBIDDEN, "没有电竞室管理权限");
        }
    }

    private boolean privateBooking(PortalEsportsBookingDO booking, List<PortalEsportsApprovalStepDO> steps, Long userId) {
        return booking.getApplicantUserId().equals(userId) || canManage(userId) || canViewAll(userId)
                || effectiveSteps(booking, steps).stream().anyMatch(s -> s.getApproverUserId().equals(userId));
    }

    private static String displayName(AdminUserRespDTO user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    // ══════════════ 步骤快照 / 兼容旧单 ══════════════

    /** 滚动旧代码新建的无节点单按原审批人合成单环节；新单直接返回自己的快照。 */
    private List<PortalEsportsApprovalStepDO> effectiveSteps(PortalEsportsBookingDO booking,
                                                             List<PortalEsportsApprovalStepDO> steps) {
        List<PortalEsportsApprovalStepDO> own = steps.stream()
                .filter(s -> s.getBookingId().equals(booking.getId())).toList();
        if (!own.isEmpty()) {
            return own;
        }
        String status = switch (booking.getStatus()) {
            case "pending" -> "pending";
            case "approved" -> "approved";
            case "rejected" -> "rejected";
            default -> "skipped";
        };
        PortalEsportsApprovalStepDO synth = new PortalEsportsApprovalStepDO();
        synth.setBookingId(booking.getId());
        synth.setStageIndex(0);
        synth.setApproverUserId(booking.getApproverUserId());
        synth.setApproverName(booking.getApproverName());
        synth.setStatus(status);
        synth.setActedByUserId(booking.getReviewedByUserId());
        synth.setActedByName(booking.getReviewedByName());
        synth.setActedAt(booking.getReviewedAt());
        synth.setRemark(booking.getReviewRemark());
        return List.of(synth);
    }

    /** 旧单即使 current index 未设置，也按自己的单环节待审快照计算。 */
    private List<PortalEsportsApprovalStepDO> currentSteps(PortalEsportsBookingDO booking,
                                                           List<PortalEsportsApprovalStepDO> steps) {
        List<PortalEsportsApprovalStepDO> own = effectiveSteps(booking, steps);
        if (own.get(0).getId() == null && "pending".equals(booking.getStatus())) {
            return own.stream().filter(s -> "pending".equals(s.getStatus())).toList();
        }
        return EsportsRules.pendingSteps(booking, own);
    }

    /** 动作持锁时惰性补旧单原审批人快照；读路径合成而不写数据库。 */
    private List<PortalEsportsApprovalStepDO> materializeLegacySteps(PortalEsportsBookingDO row,
                                                                     List<PortalEsportsApprovalStepDO> steps) {
        if (!steps.isEmpty()) {
            return steps;
        }
        PortalEsportsApprovalStepDO synth = effectiveSteps(row, List.of()).get(0);
        PortalEsportsApprovalStepDO saved = new PortalEsportsApprovalStepDO();
        saved.setBookingId(row.getId());
        saved.setStageIndex(0);
        saved.setApproverUserId(synth.getApproverUserId());
        saved.setApproverName(synth.getApproverName());
        saved.setStatus(synth.getStatus());
        saved.setActedByUserId(synth.getActedByUserId());
        saved.setActedByName(synth.getActedByName());
        saved.setActedAt(synth.getActedAt());
        saved.setRemark(synth.getRemark());
        stepMapper.insert(saved);
        row.setApprovalMode("any");
        row.setCurrentStageIndex("pending".equals(row.getStatus()) ? 0 : null);
        return List.of(saved);
    }

    // ══════════════ 范围过滤 ══════════════

    private boolean scopeVisible(String scope, boolean manage, PortalEsportsBookingDO row,
                                 List<PortalEsportsApprovalStepDO> steps, Long userId) {
        switch (scope == null ? "calendar" : scope) {
            case "mine":
                return row.getApplicantUserId().equals(userId);
            case "review":
                if (manage) {
                    return true;
                }
                List<PortalEsportsApprovalStepDO> own = steps.stream()
                        .filter(s -> s.getBookingId().equals(row.getId())).toList();
                boolean anyApprover = own.stream().anyMatch(s -> s.getApproverUserId().equals(userId));
                return anyApprover || (own.isEmpty() && row.getApproverUserId().equals(userId));
            case "calendar":
                return EsportsRules.OCCUPYING_STATUSES.contains(row.getStatus());
            default:
                return true;
        }
    }

    // ══════════════ VO 组装 ══════════════

    private EsportsSettingsRespVO toSettingsVO(PortalEsportsSettingsDO row, Long userId) {
        EsportsRules.SettingsPlan plan = EsportsRules.settingsPlan(row);
        List<Long> ids = plan.stages().stream()
                .flatMap(s -> EsportsRules.approverIds(s).stream()).distinct().toList();
        Map<Long, AdminUserRespDTO> people = adminUserApi.getUserMap(ids);
        AdminUserRespDTO approver = row.getApproverUserId() == null ? null : adminUserApi.getUser(row.getApproverUserId());

        EsportsSettingsRespVO vo = new EsportsSettingsRespVO();
        vo.setIsOpen(row.getIsOpen());
        vo.setOpenWeekdays(row.getOpenWeekdays());
        vo.setOpenStartTime(row.getOpenStartTime());
        vo.setOpenEndTime(row.getOpenEndTime());
        vo.setAdvanceDays(row.getAdvanceDays());
        vo.setMaxDurationMinutes(row.getMaxDurationMinutes());
        vo.setApproverUserId(row.getApproverUserId());
        vo.setApproverName(approver == null ? null : displayName(approver));
        vo.setApproverActive(approver != null && STATUS_ACTIVE.equals(approver.getStatus()));
        vo.setTimezone("Asia/Shanghai");
        vo.setCanManage(canManage(userId));
        vo.setCanViewAll(canViewAll(userId));
        vo.setApprovalMode(plan.mode());
        vo.setApprovalStages(plan.stages().stream().map(stage -> {
            EsportsApproverStageRespVO sv = new EsportsApproverStageRespVO();
            List<Long> uids = EsportsRules.approverIds(stage);
            sv.setApproverUserIds(uids);
            sv.setApprovers(uids.stream().map(uid -> {
                EsportsApproverEntryRespVO ev = new EsportsApproverEntryRespVO();
                AdminUserRespDTO p = people.get(uid);
                ev.setId(uid);
                ev.setDisplayName(p == null ? "未知账号" : displayName(p));
                ev.setActive(p != null && STATUS_ACTIVE.equals(p.getStatus()));
                return ev;
            }).toList());
            return sv;
        }).toList());
        vo.setCanReview(canReview(userId, ids));
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private EsportsBookingRespVO toBookingVO(PortalEsportsBookingDO row, List<PortalEsportsApprovalStepDO> steps,
                                             List<PortalEsportsBookingLogDO> logs, Long userId, boolean privateView) {
        List<PortalEsportsApprovalStepDO> own = effectiveSteps(row, steps);
        List<Long> pendingIds = currentSteps(row, own).stream()
                .map(PortalEsportsApprovalStepDO::getApproverUserId).toList();
        boolean legacy = own.get(0).getId() == null;

        EsportsBookingRespVO vo = new EsportsBookingRespVO();
        vo.setId(row.getId());
        vo.setBookingDate(row.getBookingDate());
        vo.setStartTime(row.getStartTime());
        vo.setEndTime(row.getEndTime());
        vo.setTitle(row.getTitle());
        vo.setPurpose(privateView ? row.getPurpose() : null);
        vo.setParticipantCount(row.getParticipantCount());
        vo.setParticipants(privateView ? row.getParticipants() : null);
        vo.setApplicantUserId(row.getApplicantUserId());
        vo.setApplicantName(row.getApplicantName());
        vo.setApproverUserId(row.getApproverUserId());
        vo.setApproverName(row.getApproverName());
        vo.setStatus(row.getStatus());
        vo.setReviewedByName(row.getReviewedByName());
        vo.setApprovalMode(legacy ? "any" : row.getApprovalMode());
        vo.setCurrentStageIndex(legacy ? ("pending".equals(row.getStatus()) ? 0 : null) : row.getCurrentStageIndex());
        vo.setCurrentApproverUserIds(pendingIds);
        vo.setApprovalSteps(own.stream().map(s -> toStepVO(s, privateView)).toList());
        vo.setReviewRemark(privateView ? row.getReviewRemark() : null);
        vo.setReviewedAt(row.getReviewedAt());
        vo.setCancelledAt(row.getCancelledAt());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        vo.setIsMine(row.getApplicantUserId().equals(userId));
        vo.setCanReview(EsportsRules.actionTimeOpen(row, "approve") && "pending".equals(row.getStatus())
                && canReview(userId, pendingIds));
        vo.setCanCancel(EsportsRules.actionTimeOpen(row, "cancel")
                && ("pending".equals(row.getStatus()) || "approved".equals(row.getStatus()))
                && (row.getApplicantUserId().equals(userId) || canManage(userId)));
        vo.setLogs(privateView
                ? logs.stream().filter(l -> l.getBookingId().equals(row.getId())).map(this::toLogVO).toList()
                : List.of());
        return vo;
    }

    private EsportsApprovalStepRespVO toStepVO(PortalEsportsApprovalStepDO step, boolean privateView) {
        EsportsApprovalStepRespVO vo = new EsportsApprovalStepRespVO();
        vo.setId(step.getId());
        vo.setStageIndex(step.getStageIndex());
        vo.setApproverUserId(step.getApproverUserId());
        vo.setApproverName(step.getApproverName());
        vo.setStatus(step.getStatus());
        if (privateView) {
            vo.setActedByUserId(step.getActedByUserId());
            vo.setActedByName(step.getActedByName());
            vo.setActedAt(step.getActedAt());
            vo.setRemark(step.getRemark());
        }
        return vo;
    }

    private EsportsBookingLogRespVO toLogVO(PortalEsportsBookingLogDO log) {
        EsportsBookingLogRespVO vo = new EsportsBookingLogRespVO();
        vo.setId(log.getId());
        vo.setOperation(log.getOperation());
        vo.setActorName(log.getActorName());
        vo.setRemark(log.getRemark());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }

    private List<EsportsUserRespVO> activeUsersVO() {
        List<AdminUserRespDTO> users = adminUserApi.getUserListByStatus(STATUS_ACTIVE);
        List<Long> deptIds = users.stream().map(AdminUserRespDTO::getDeptId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptNames = new LinkedHashMap<>();
        for (DeptRespDTO dept : deptApi.getDeptList(deptIds)) {
            if (dept.getName() != null) {
                deptNames.put(dept.getId(), dept.getName());
            }
        }
        List<EsportsUserRespVO> out = new ArrayList<>(users.size());
        for (AdminUserRespDTO u : users) {
            EsportsUserRespVO vo = new EsportsUserRespVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setDisplayName(u.getNickname());
            vo.setDepartment(u.getDeptId() == null ? null
                    : List.of(deptNames.getOrDefault(u.getDeptId(), "")));
            out.add(vo);
        }
        return out;
    }

}
