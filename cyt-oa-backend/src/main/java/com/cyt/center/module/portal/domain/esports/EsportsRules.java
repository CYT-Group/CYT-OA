package com.cyt.center.module.portal.domain.esports;

import com.cyt.center.framework.common.exception.ErrorCode;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsBookingCreateReqVO;
import com.cyt.center.module.portal.controller.app.esports.vo.EsportsSettingsUpdateReqVO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsApprovalStepDO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsBookingDO;
import com.cyt.center.module.portal.dal.dataobject.esports.PortalEsportsSettingsDO;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_BOOKING_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_CONFLICT;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.ESPORTS_SETTINGS_INVALID;

/**
 * 电竞室领域规则（1:1 迁自 Portal 的 domain/esports.py）。
 *
 * 纯函数、不访问数据库/框架：整间独占、上海营业时间与预约状态机、多级审批环节快照推进。
 * 错误码沿用 Portal 的 Invalid→422 / Conflict→409 语义，落 ESPORTS_SETTINGS_INVALID / ESPORTS_BOOKING_INVALID / ESPORTS_CONFLICT。
 */
public final class EsportsRules {

    public static final Set<String> STATUSES = Set.of("pending", "approved", "rejected", "cancelled");
    public static final Set<String> OCCUPYING_STATUSES = Set.of("pending", "approved");

    /** 审批计划：mode ∈ any|sequential，stages 为有序环节数组 [{approver_user_ids:[...]}]。 */
    public record SettingsPlan(String mode, List<Map<String, Object>> stages) {
    }

    private EsportsRules() {
    }

    // ── 时间口径 ───────────────────────────────────────────

    /** 按上海墙上时间校验预约；审计列另存为北京时间（迁移时 app_utc 换算）。 */
    public static LocalDateTime shanghaiNow() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }

    // ── 环节数组辅助 ───────────────────────────────────────

    public static List<Long> approverIds(Map<String, Object> stage) {
        Object ids = stage == null ? null : stage.get("approver_user_ids");
        if (!(ids instanceof List<?> list)) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (Object x : list) {
            if (x instanceof Number n) {
                out.add(n.longValue());
            }
        }
        return out;
    }

    public static Map<String, Object> stageOf(List<Long> ids) {
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("approver_user_ids", ids == null ? List.of() : ids);
        return stage;
    }

    private static Long firstApproverId(List<Map<String, Object>> stages) {
        if (stages == null || stages.isEmpty()) {
            return null;
        }
        List<Long> ids = approverIds(stages.get(0));
        return ids.isEmpty() ? null : ids.get(0);
    }

    // ── 设置投影 ───────────────────────────────────────────

    /** 兼容滚动旧代码改写单审批人：投影不一致时视为单环节旧配置。 */
    public static SettingsPlan settingsPlan(PortalEsportsSettingsDO settings) {
        List<Map<String, Object>> stages = settings.getApprovalStages() != null ? settings.getApprovalStages() : List.of();
        Long firstId = firstApproverId(stages);
        if (!Objects.equals(firstId, settings.getApproverUserId())) {
            List<Map<String, Object>> fallback = settings.getApproverUserId() != null
                    ? List.of(stageOf(List.of(settings.getApproverUserId())))
                    : List.of();
            return new SettingsPlan("any", fallback);
        }
        return new SettingsPlan(settings.getApprovalMode(), stages);
    }

    /** 新环节数组为权威；只带旧单人字段的请求继续表达单环节任一审批。 */
    public static SettingsPlan requestedPlan(EsportsSettingsUpdateReqVO body) {
        if (body.getApprovalStages() != null) {
            List<Map<String, Object>> stages = body.getApprovalStages().stream()
                    .map(s -> stageOf(s.getApproverUserIds()))
                    .toList();
            String mode = body.getApprovalMode() == null ? "any" : body.getApprovalMode();
            return new SettingsPlan(mode, stages);
        }
        List<Map<String, Object>> stages = body.getApproverUserId() != null
                ? List.of(stageOf(List.of(body.getApproverUserId())))
                : List.of();
        return new SettingsPlan("any", stages);
    }

    // ── 校验 ───────────────────────────────────────────────

    /** 限制日历查询为最多 62 个自然日，避免无限拉取历史记录。 */
    public static void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 61) {
            throw exception(ESPORTS_BOOKING_INVALID, "日期范围必须为 1 至 62 天");
        }
    }

    private static void validateMinuteTime(LocalTime value, ErrorCode code) {
        if (value.getSecond() != 0 || value.getNano() != 0) {
            throw exception(code, "时间必须为无时区的 HH:mm");
        }
    }

    /** 开放规则不支持跨午夜，星期使用 ISO 周一 1 至周日 7。 */
    public static void validateSettings(EsportsSettingsUpdateReqVO body) {
        validateMinuteTime(body.getOpenStartTime(), ESPORTS_SETTINGS_INVALID);
        validateMinuteTime(body.getOpenEndTime(), ESPORTS_SETTINGS_INVALID);
        if (!body.getOpenStartTime().isBefore(body.getOpenEndTime())) {
            throw exception(ESPORTS_SETTINGS_INVALID, "开放结束时间必须晚于开始时间");
        }
        if (new HashSet<>(body.getOpenWeekdays()).size() != body.getOpenWeekdays().size()) {
            throw exception(ESPORTS_SETTINGS_INVALID, "开放星期不能重复");
        }
        if (Boolean.TRUE.equals(body.getIsOpen())
                && (body.getOpenWeekdays() == null || body.getOpenWeekdays().isEmpty())) {
            throw exception(ESPORTS_SETTINGS_INVALID, "开放前请设置开放星期和审批人");
        }
    }

    /** 串行模式最多十环节，每环节最多二十人，任一模式只有一环节。 */
    public static void validatePlan(SettingsPlan plan, boolean isOpen) {
        List<Map<String, Object>> stages = plan.stages();
        if (isOpen && stages.isEmpty()) {
            throw exception(ESPORTS_SETTINGS_INVALID, "开放预约前请设置审批人");
        }
        if (stages.size() > 10 || ("any".equals(plan.mode()) && stages.size() > 1)) {
            throw exception(ESPORTS_SETTINGS_INVALID, "任一审批只能配置一个环节，顺序审批最多十个环节");
        }
        for (Map<String, Object> stage : stages) {
            List<Long> ids = approverIds(stage);
            if (ids.isEmpty() || ids.size() > 20 || new HashSet<>(ids).size() != ids.size()) {
                throw exception(ESPORTS_SETTINGS_INVALID, "每个审批环节须配置 1 至 20 位不重复的审批人");
            }
        }
    }

    /** 新单遵守当前开放规则，旧单审批不受之后的关闭或规则变更影响。 */
    public static void validateBooking(EsportsBookingCreateReqVO body, PortalEsportsSettingsDO settings) {
        LocalDateTime now = shanghaiNow();
        if (!Boolean.TRUE.equals(settings.getIsOpen())) {
            throw exception(ESPORTS_CONFLICT, "电竞室暂未开放预约");
        }
        validateMinuteTime(body.getStartTime(), ESPORTS_BOOKING_INVALID);
        validateMinuteTime(body.getEndTime(), ESPORTS_BOOKING_INVALID);
        if (!body.getStartTime().isBefore(body.getEndTime())) {
            throw exception(ESPORTS_BOOKING_INVALID, "结束时间必须晚于开始时间，预约不能跨午夜");
        }
        if (!LocalDateTime.of(body.getBookingDate(), body.getStartTime()).isAfter(now)) {
            throw exception(ESPORTS_BOOKING_INVALID, "不能预约已经开始或过去的时间");
        }
        if (body.getBookingDate().isAfter(now.toLocalDate().plusDays(settings.getAdvanceDays()))) {
            throw exception(ESPORTS_BOOKING_INVALID, "超过可提前预约天数");
        }
        if (!settings.getOpenWeekdays().contains(body.getBookingDate().getDayOfWeek().getValue())) {
            throw exception(ESPORTS_BOOKING_INVALID, "当天不开放预约");
        }
        if (body.getStartTime().isBefore(settings.getOpenStartTime())
                || body.getEndTime().isAfter(settings.getOpenEndTime())) {
            throw exception(ESPORTS_BOOKING_INVALID, "预约时间超出每日开放时段");
        }
        long duration = Duration.between(body.getStartTime(), body.getEndTime()).toMinutes();
        if (duration > settings.getMaxDurationMinutes()) {
            throw exception(ESPORTS_BOOKING_INVALID, "超过单次最长预约时长");
        }
    }

    /** 新表单人数由不重复名单决定，旧表单必须继续提供人数且不伪造名单。 */
    public static int participantCountFor(EsportsBookingCreateReqVO body) {
        List<Long> ids = body.getParticipantUserIds();
        if (ids == null) {
            if (body.getParticipantCount() == null) {
                throw exception(ESPORTS_BOOKING_INVALID, "请选择参加人员或提供参加人数");
            }
            return body.getParticipantCount();
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw exception(ESPORTS_BOOKING_INVALID, "参加人员不能重复");
        }
        if (body.getParticipantCount() != null && body.getParticipantCount() != ids.size()) {
            throw exception(ESPORTS_BOOKING_INVALID, "参加人数与人员名单不一致");
        }
        return ids.size();
    }

    /** 审批可持续到预约结束，取消须在开始前。 */
    public static boolean actionTimeOpen(PortalEsportsBookingDO booking, String operation) {
        LocalTime deadline = "cancel".equals(operation) ? booking.getStartTime() : booking.getEndTime();
        return LocalDateTime.of(booking.getBookingDate(), deadline).isAfter(shanghaiNow());
    }

    /** 待审单结束前可审批，待审或已通过单开始前可取消。 */
    public static void validateTransition(PortalEsportsBookingDO booking, String operation) {
        Set<String> allowed = "cancel".equals(operation) ? OCCUPYING_STATUSES : Set.of("pending");
        if (!allowed.contains(booking.getStatus())) {
            throw exception(ESPORTS_CONFLICT, "预约状态已变化，请刷新后重试");
        }
        if (!actionTimeOpen(booking, operation)) {
            throw exception(ESPORTS_CONFLICT, "cancel".equals(operation) ? "已经开始的预约不能取消" : "已经结束的预约不能审批");
        }
    }

    // ── 审批状态机 ─────────────────────────────────────────

    /** 只有当前环节的待审批节点持有处理权。 */
    public static List<PortalEsportsApprovalStepDO> pendingSteps(PortalEsportsBookingDO booking,
                                                                 List<PortalEsportsApprovalStepDO> steps) {
        if (!"pending".equals(booking.getStatus())) {
            return List.of();
        }
        return steps.stream()
                .filter(s -> Objects.equals(s.getStageIndex(), booking.getCurrentStageIndex())
                        && "pending".equals(s.getStatus()))
                .toList();
    }

    /** 环节内任一通过、任一拒绝终止；管理员代办仍然一次只推进一个环节。 */
    public static void transitionApproval(PortalEsportsBookingDO booking, List<PortalEsportsApprovalStepDO> steps,
                                          PortalEsportsApprovalStepDO selected, String operation,
                                          Long actorId, String actorName, String remark, LocalDateTime now) {
        selected.setStatus("approve".equals(operation) ? "approved" : "rejected");
        selected.setActedByUserId(actorId);
        selected.setActedByName(actorName);
        selected.setActedAt(now);
        selected.setRemark(remark);
        for (PortalEsportsApprovalStepDO step : steps) {
            if (step != selected && "pending".equals(step.getStatus())) {
                step.setStatus("skipped");
            }
        }
        if ("reject".equals(operation)) {
            booking.setStatus("rejected");
            booking.setCurrentStageIndex(null);
            for (PortalEsportsApprovalStepDO step : steps) {
                if ("waiting".equals(step.getStatus())) {
                    step.setStatus("skipped");
                }
            }
            return;
        }
        List<PortalEsportsApprovalStepDO> next = steps.stream()
                .filter(s -> Objects.equals(s.getStageIndex(), selected.getStageIndex() + 1)
                        && "waiting".equals(s.getStatus()))
                .toList();
        if (!next.isEmpty()) {
            booking.setCurrentStageIndex(selected.getStageIndex() + 1);
            next.forEach(s -> s.setStatus("pending"));
        } else {
            booking.setStatus("approved");
            booking.setCurrentStageIndex(null);
        }
    }

}
