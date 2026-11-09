package com.cyt.center.module.me.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.enums.employee.info.HrmEmployeeEntryStatusEnum;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.mapper.MilestoneMapper;
import com.cyt.center.module.me.mapper.RecognitionMapper;
import com.cyt.center.module.me.mapper.VoiceMapper;
import com.cyt.center.module.me.model.cmd.MeCmds.BlessCmd;
import com.cyt.center.module.me.model.cmd.MeCmds.VoiceCmd;
import com.cyt.center.module.me.model.entity.Milestone;
import com.cyt.center.module.me.model.entity.Recognition;
import com.cyt.center.module.me.model.entity.Voice;
import com.cyt.center.module.me.model.vo.MeVos.ColleagueMilestoneView;
import com.cyt.center.module.me.model.vo.MeVos.Footprint;
import com.cyt.center.module.me.model.vo.MeVos.MilestoneView;
import com.cyt.center.module.me.model.vo.MeVos.Profile;
import com.cyt.center.module.me.model.vo.MeVos.RecognitionView;
import com.cyt.center.module.me.model.vo.MeVos.VoiceView;
import com.cyt.center.module.oa.controller.admin.attendance.vo.OaAttendancePageReqVO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.dal.mysql.workreport.OaWorkReportMapper;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportStatusEnum;
import com.cyt.center.module.oa.service.attendance.OaAttendanceService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 我的资料与足迹。资料只读人事档案，足迹全部由业务表反查出来，不做人工登记；
 * 认可（点赞/表扬）和心声走 CYT 老库里的 recognitions、voices 两张表。
 */
@Service
@RequiredArgsConstructor
public class MeProfileService {

    private static final int RECENT_MILESTONES = 12;
    private static final int COLLEAGUE_AHEAD_DAYS = 14;
    private static final int TEAM_AHEAD_DAYS = 30;

    private final HrmEmployeeService employees;
    private final DeptApi deptApi;
    private final AdminUserApi userApi;
    private final OaWorkReportMapper reports;
    private final OaAttendanceService attendance;
    private final MeTeamService team;
    private final MeReminderService reminders;
    private final MeApplyService applies;
    private final MilestoneMapper milestones;
    private final RecognitionMapper recognitions;
    private final VoiceMapper voices;

    public Profile profile(Long userId) {
        HrmEmployeeDO employee = employees.getEmployeeByUserId(userId);
        if (employee == null) {
            throw new BizException("还没有人事档案，请联系 HR 补全");
        }
        HrmEmployeeDO manager = employee.getLeaderEmployeeId() == null ? null
                : employees.getEmployee(employee.getLeaderEmployeeId());
        return new Profile(employee.getName(), employee.getJobNumber(), deptName(employee.getDeptId()),
                employee.getPostName(), employee.getPostLevel(),
                toLocalDate(employee.getBirthday()), toLocalDate(employee.getEntryTime()),
                workAge(employee), employee.getWorkCity(), employee.getStatus(), employee.getEntryStatus(),
                employee.getProbation(), toLocalDate(employee.getRegularTime()),
                manager == null ? null : manager.getName(),
                team.myTeam(userId).size(), footprint(userId), reminders.preferences(userId));
    }

    /** 足迹：系统里真实留下的痕迹，全部从业务表反查，不做人工登记。 */
    public Footprint footprint(Long userId) {
        String creator = String.valueOf(userId);
        int reportTotal = count(reports.selectCount(new LambdaQueryWrapperX<OaWorkReportDO>()
                .eq(OaWorkReportDO::getCreator, creator)));
        int submitted = count(reports.selectCount(new LambdaQueryWrapperX<OaWorkReportDO>()
                .eq(OaWorkReportDO::getCreator, creator)
                .eq(OaWorkReportDO::getStatus, OaWorkReportStatusEnum.SUBMITTED.getStatus())));
        PageParam one = new PageParam();
        one.setPageSize(1);
        int applyTotal = 0;
        for (String type : MeApplyService.types()) {
            applyTotal += count(applies.page(userId, type, null, one).getTotal());
        }
        OaAttendancePageReqVO attendanceReqVO = new OaAttendancePageReqVO();
        attendanceReqVO.setPageSize(1);
        return new Footprint(reportTotal, submitted, applyTotal,
                count(recognitions.countByTo(userId)), count(milestones.countByUser(userId)),
                count(attendance.getMyAttendancePage(attendanceReqVO, userId).getTotal()),
                count(voices.countByUser(userId)));
    }

    public List<MilestoneView> milestoneViews(Long userId) {
        LocalDate today = LocalDate.now();
        List<MilestoneView> list = new ArrayList<>();
        for (Milestone row : milestones.selectListByUser(userId, RECENT_MILESTONES)) {
            LocalDate date = row.getMilestoneDate();
            Integer daysLeft = date == null ? null : (int) ChronoUnit.DAYS.between(today, date);
            list.add(new MilestoneView(row.getId(), row.getType(), milestoneText(row.getType()), date,
                    row.getTitle(), daysLeft));
        }
        return list;
    }

    /** 收到的认可；period 形如 2026-11，为空则取全部。 */
    public List<RecognitionView> recognitionViews(Long userId, String period) {
        List<RecognitionView> list = new ArrayList<>();
        List<Recognition> rows = period == null || period.isBlank()
                ? recognitions.selectListByTo(userId) : recognitions.selectListByToAndPeriod(userId, period);
        for (Recognition row : rows) {
            list.add(new RecognitionView(row.getId(), row.getType(), recognitionText(row.getType()),
                    row.getFromId(), userName(row.getFromId()), row.getScope(), row.getNote(),
                    row.getPeriod(), row.getCreatedAt()));
        }
        return list;
    }

    public List<VoiceView> voiceViews(Long userId) {
        List<VoiceView> list = new ArrayList<>();
        for (Voice row : voices.selectListByUser(userId)) {
            boolean anonymous = Boolean.TRUE.equals(row.getAnonymous());
            list.add(new VoiceView(row.getId(), row.getChannel(), voiceText(row.getChannel()), anonymous,
                    anonymous ? "匿名" : userName(userId), row.getContent(), row.getStatus(), row.getCreatedAt()));
        }
        return list;
    }

    /** 心声：员工自己的入口写本人，匿名条前端不展示作者。 */
    public Long submitVoice(Long userId, VoiceCmd cmd) {
        Voice row = new Voice();
        row.setUserId(userId);
        row.setChannel(cmd.getChannel());
        row.setContent(cmd.getContent());
        row.setAnonymous(Boolean.TRUE.equals(cmd.getAnonymous()));
        row.setStatus(Voice.STATUS_OPEN);
        row.setCreatedAt(LocalDateTime.now());
        voices.insert(row);
        return row.getId();
    }

    /** 司龄按入职时间推导，档案里已有的 company_age 优先。 */
    private static Integer workAge(HrmEmployeeDO employee) {
        if (employee.getCompanyAge() != null) {
            return employee.getCompanyAge();
        }
        LocalDate entry = toLocalDate(employee.getEntryTime());
        return entry == null ? null : (int) ChronoUnit.YEARS.between(entry, LocalDate.now());
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        var dept = deptApi.getDept(deptId);
        return dept == null ? null : dept.getName();
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        var user = userApi.getUser(userId);
        return user == null ? null : user.getNickname();
    }

    private static int count(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private static LocalDate toLocalDate(LocalDateTime time) {
        return time == null ? null : time.toLocalDate();
    }

    /** 同事的生日与周年：下属加同部门，只认档案里算得出的那天，临近了才出现在祝福带里。 */
    public List<ColleagueMilestoneView> colleagueMilestoneViews(Long userId) {
        HrmEmployeeDO me = employees.getEmployeeByUserId(userId);
        if (me == null) {
            return List.of();
        }
        List<HrmEmployeeDO> subordinates = employees.getEmployeeListByLeaderEmployeeId(me.getId());
        List<HrmEmployeeDO> deptPeers = me.getDeptId() == null
                ? List.of() : employees.getEmployeeListByDeptIds(List.of(me.getDeptId()));
        Map<Long, HrmEmployeeDO> peers = new LinkedHashMap<>();
        Stream.concat(subordinates.stream(), deptPeers.stream())
                .filter(employee -> !Objects.equals(employee.getId(), me.getId()))
                .forEach(employee -> peers.putIfAbsent(employee.getId(), employee));
        Set<Long> subordinateIds = subordinates.stream().map(HrmEmployeeDO::getId).collect(Collectors.toSet());
        return milestoneViews(userId, peers.values(), subordinateIds, false, COLLEAGUE_AHEAD_DAYS);
    }

    /** 团队关怀雷达：只看下属，窗口比祝福带宽，还要多盯一项转正评估——那是要提前安排述职的事，不是祝福。 */
    public List<ColleagueMilestoneView> teamMilestoneViews(Long userId) {
        List<HrmEmployeeDO> subordinates = team.myTeam(userId);
        if (subordinates.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = subordinates.stream().map(HrmEmployeeDO::getId).collect(Collectors.toSet());
        return milestoneViews(userId, subordinates, ids, true, TEAM_AHEAD_DAYS);
    }

    private List<ColleagueMilestoneView> milestoneViews(Long userId, Collection<HrmEmployeeDO> pool,
                                                        Set<Long> subordinateIds, boolean withProbation,
                                                        int aheadDays) {
        LocalDate today = LocalDate.now();
        Set<String> sent = sentBlessings(userId);
        List<ColleagueMilestoneView> list = new ArrayList<>();
        for (HrmEmployeeDO peer : pool) {
            if (peer.getUserId() == null
                    || !HrmEmployeeEntryStatusEnum.ACTIVE_STATUSES.contains(peer.getEntryStatus())) {
                continue;
            }
            String relation = subordinateIds.contains(peer.getId()) ? "下属" : "同部门";
            if (peer.getBirthday() != null) {
                addColleague(list, peer, MeReminderService.nextYearly(peer.getBirthday().toLocalDate(), today),
                        today, Milestone.TYPE_BIRTHDAY, "生日", relation, sent, aheadDays);
            }
            if (peer.getEntryTime() != null) {
                LocalDate date = MeReminderService.nextYearly(peer.getEntryTime().toLocalDate(), today);
                int years = (int) ChronoUnit.YEARS.between(peer.getEntryTime().toLocalDate(), date);
                addColleague(list, peer, date, today, Milestone.TYPE_ANNIVERSARY,
                        "入职 " + years + " 周年", relation, sent, aheadDays);
            }
            if (withProbation) {
                LocalDate due = probationDue(peer);
                if (due != null && !due.isBefore(today)) {
                    addColleague(list, peer, due, today, Milestone.TYPE_PROBATION, "转正评估", relation, sent, aheadDays);
                }
            }
        }
        list.sort(Comparator.comparing(ColleagueMilestoneView::daysLeft));
        return list;
    }

    /** 还没转正才有待评估的那天：档案里 probation 单位是月，与 HRM 侧 entryTime.plusMonths 同口径。 */
    private static LocalDate probationDue(HrmEmployeeDO employee) {
        if (employee.getRegularTime() != null || employee.getEntryTime() == null
                || employee.getProbation() == null || employee.getProbation() <= 0) {
            return null;
        }
        return employee.getEntryTime().toLocalDate().plusMonths(employee.getProbation());
    }

    private void addColleague(List<ColleagueMilestoneView> list, HrmEmployeeDO peer, LocalDate date, LocalDate today,
                              String type, String typeText, String relation, Set<String> sent, int aheadDays) {
        if (date.isAfter(today.plusDays(aheadDays))) {
            return;
        }
        // 转正评估没法祝福，主管能做的是安排述职；按钮口径由这里定，前端不再自己猜类型
        boolean blessable = !Milestone.TYPE_PROBATION.equals(type);
        boolean blessed = blessable && sent.contains(peer.getUserId() + "|" + type + "|" + date);
        list.add(new ColleagueMilestoneView(peer.getUserId(), peer.getName(), deptName(peer.getDeptId()),
                type, typeText, date, (int) ChronoUnit.DAYS.between(today, date), relation, blessed,
                blessable ? "bless" : "arrange"));
    }

    private Set<String> sentBlessings(Long userId) {
        Set<String> keys = new HashSet<>();
        for (Recognition row : recognitions.selectListByFrom(userId, Recognition.TYPE_BLESSING)) {
            keys.add(row.getToId() + "|" + row.getScope() + "|" + row.getPeriod());
        }
        return keys;
    }

    /** 送祝福：同一个人同一天的同一件事只记一次，重复点不会把对方的认可墙刷屏。 */
    public void sendBlessing(Long fromUserId, BlessCmd cmd) {
        if (!Milestone.TYPE_BIRTHDAY.equals(cmd.getType()) && !Milestone.TYPE_ANNIVERSARY.equals(cmd.getType())) {
            throw new BizException("祝福只针对生日和入职周年");
        }
        if (recognitions.selectBlessing(fromUserId, cmd.getToUserId(), cmd.getType(), cmd.getDate().toString()) != null) {
            return;
        }
        Recognition row = new Recognition();
        row.setFromId(fromUserId);
        row.setToId(cmd.getToUserId());
        row.setType(Recognition.TYPE_BLESSING);
        row.setScope(cmd.getType());
        row.setNote(Milestone.TYPE_BIRTHDAY.equals(cmd.getType()) ? "生日快乐" : "周年快乐");
        row.setPeriod(cmd.getDate().toString());
        row.setCreatedAt(LocalDateTime.now());
        recognitions.insert(row);
    }

    private static String milestoneText(String type) {
        return switch (type == null ? "" : type) {
            case Milestone.TYPE_BIRTHDAY -> "生日";
            case Milestone.TYPE_ANNIVERSARY -> "入职周年";
            case Milestone.TYPE_PROBATION -> "转正评估";
            case Milestone.TYPE_PROMOTION -> "晋级";
            default -> "里程碑";
        };
    }

    private static String recognitionText(String type) {
        return switch (type == null ? "" : type) {
            case Recognition.TYPE_LIKE -> "点赞";
            case Recognition.TYPE_PRAISE -> "表扬";
            case Recognition.TYPE_STAR_NOMINATION -> "明星提名";
            case Recognition.TYPE_BLESSING -> "祝福";
            default -> "认可";
        };
    }

    private static String voiceText(String channel) {
        return switch (channel == null ? "" : channel) {
            case Voice.CHANNEL_SUGGESTION -> "建议";
            case Voice.CHANNEL_CEO -> "CEO 信箱";
            case Voice.CHANNEL_SURVEY -> "调研";
            default -> "心声";
        };
    }
}
