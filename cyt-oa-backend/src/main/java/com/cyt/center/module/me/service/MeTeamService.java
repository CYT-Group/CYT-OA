package com.cyt.center.module.me.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.mapper.RecognitionMapper;
import com.cyt.center.module.me.model.cmd.MeCmds.RecognizeCmd;
import com.cyt.center.module.me.model.entity.Recognition;
import com.cyt.center.module.me.model.vo.MeVos.Member;
import com.cyt.center.module.me.model.vo.MeVos.MemberBrief;
import com.cyt.center.module.me.model.vo.MeVos.ReportBrief;
import com.cyt.center.module.me.model.vo.MeVos.TrackCell;
import com.cyt.center.module.me.model.vo.MeVos.TrackCellHeader;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportCommentDO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.dal.mysql.workreport.OaWorkReportCommentMapper;
import com.cyt.center.module.oa.dal.mysql.workreport.OaWorkReportMapper;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportStatusEnum;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportTypeEnum;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 我的团队。上下级关系取自人事档案的直属上级的倒排（leader_employee_id），
 * 网关下发部门负责人后可与部门口径合并；看板一次查出窗口内的全部汇报，不在成员循环里查库。
 */
@Service
@RequiredArgsConstructor
public class MeTeamService {

    private static final int BOARD_WEEKS = 8;

    private final HrmEmployeeService employees;
    private final OaWorkReportMapper reportMapper;
    private final OaWorkReportCommentMapper commentMapper;
    private final RecognitionMapper recognitionMapper;
    private final AdminUserApi userApi;
    private final DeptApi deptApi;

    /** 我有没有团队：前端据此决定「带团队的人才有」这一区是否出现。 */
    public List<HrmEmployeeDO> myTeam(Long userId) {
        HrmEmployeeDO me = employees.getEmployeeByUserId(userId);
        if (me == null) {
            return List.of();
        }
        return employees.getEmployeeListByLeaderEmployeeId(me.getId()).stream()
                .filter(employee -> employee.getUserId() != null)
                .sorted(Comparator.comparing(HrmEmployeeDO::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public MemberBrief me(Long userId) {
        HrmEmployeeDO me = employees.getEmployeeByUserId(userId);
        String name = me != null ? me.getName() : userName(userId);
        return new MemberBrief(userId, name);
    }

    /** 周报看板：成员 × 最近 8 周。submitted 绿、draft 黄、missing 红。 */
    public Board board(Long userId) {
        List<HrmEmployeeDO> team = myTeam(userId);
        List<TrackCellHeader> weeks = weekHeaders(LocalDate.now());
        if (team.isEmpty()) {
            return new Board(me(userId), List.of(), weeks, List.of(), 0, null);
        }
        LocalDateTime windowStart = LocalDate.parse(weeks.get(0).weekStart()).atStartOfDay();
        LocalDateTime windowEnd = LocalDate.parse(weeks.get(weeks.size() - 1).weekStart())
                .plusDays(6).atTime(LocalTime.MAX);
        Set<String> creators = team.stream().map(e -> String.valueOf(e.getUserId())).collect(Collectors.toSet());
        List<OaWorkReportDO> rows = reportMapper.selectListByCreatorsAndStartTimeBetween(
                creators, windowStart, windowEnd);

        Map<Long, Map<String, OaWorkReportDO>> byUserAndPeriod = new HashMap<>();
        OaWorkReportTypeEnum weekly = OaWorkReportTypeEnum.WEEKLY;
        for (OaWorkReportDO row : rows) {
            Long owner = resolveOwner(row.getCreator());
            if (owner == null || row.getStartTime() == null) {
                continue;
            }
            String periodKey = weekly.formatPeriod(row.getStartTime());
            byUserAndPeriod.computeIfAbsent(owner, k -> new HashMap<>())
                    .merge(periodKey, row, MeTeamService::preferSubmitted);
        }

        List<Member> members = new ArrayList<>(team.size());
        int submittedThisWeek = 0;
        String currentPeriod = weeks.get(weeks.size() - 1).periodKey();
        String currentMonth = YearMonth.now().toString();
        Set<Long> likedIds = targetsOf(userId, Recognition.TYPE_LIKE, currentPeriod);
        Set<Long> starIds = targetsOf(userId, Recognition.TYPE_STAR_NOMINATION, currentMonth);
        for (HrmEmployeeDO employee : team) {
            Map<String, OaWorkReportDO> periods = byUserAndPeriod.getOrDefault(employee.getUserId(), Map.of());
            List<TrackCell> cells = new ArrayList<>(weeks.size());
            for (TrackCellHeader header : weeks) {
                OaWorkReportDO row = periods.get(header.periodKey());
                String state = row == null ? "missing"
                        : OaWorkReportStatusEnum.SUBMITTED.getStatus().equals(row.getStatus()) ? "submitted" : "draft";
                if ("submitted".equals(state) && header.periodKey().equals(currentPeriod)) {
                    submittedThisWeek++;
                }
                cells.add(new TrackCell(header.periodKey(), header.label(), state, row == null ? null : row.getId()));
            }
            members.add(new Member(employee.getUserId(), employee.getId(), employee.getName(),
                    employee.getPostName(), deptName(employee.getDeptId()),
                    employee.getEntryTime() == null ? null : employee.getEntryTime().toLocalDate(), cells,
                    likedIds.contains(employee.getUserId()), starIds.contains(employee.getUserId())));
        }

        List<ReportBrief> pending = pendingReviews(userId, team);
        MemberBrief star = starIds.isEmpty() ? null : team.stream()
                .filter(e -> starIds.contains(e.getUserId()))
                .findFirst()
                .map(e -> new MemberBrief(e.getUserId(), e.getName()))
                .orElse(null);
        return new Board(me(userId), members, weeks, pending, submittedThisWeek, star);
    }

    /**
     * 主管给下属的认可：点赞按周记，同一周重复点不涨第二次；表扬与本月之星按月记，
     * 之星一个月只留一个，提了别人就撤掉上一次的提名。只认直属下属，跨级的认可该由他自己的主管给。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recognize(Long fromUserId, RecognizeCmd cmd) {
        String type = cmd.getType();
        boolean star = Recognition.TYPE_STAR_NOMINATION.equals(type);
        boolean weekly = Recognition.TYPE_LIKE.equals(type);
        if (!star && !weekly && !Recognition.TYPE_PRAISE.equals(type)) {
            throw new BizException("认可只有点赞、公开表扬和本月之星三种");
        }
        HrmEmployeeDO target = myTeam(fromUserId).stream()
                .filter(employee -> Objects.equals(employee.getUserId(), cmd.getToUserId()))
                .findFirst()
                .orElseThrow(() -> new BizException("只能给直属下属点赞、表扬或提名本月之星"));
        String period = weekly
                ? OaWorkReportTypeEnum.WEEKLY.formatPeriod(
                        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay())
                : YearMonth.now().toString();
        if (star) {
            recognitionMapper.deleteByFromAndPeriod(fromUserId, Recognition.TYPE_STAR_NOMINATION, period);
        } else if (recognitionMapper.selectByPeriod(fromUserId, target.getUserId(), type, period) != null) {
            return;
        }
        Recognition row = new Recognition();
        row.setFromId(fromUserId);
        row.setToId(target.getUserId());
        row.setType(type);
        row.setScope("team");
        row.setNote(blank(cmd.getNote()) ? defaultNote(type, target.getName()) : cmd.getNote().trim());
        row.setPeriod(period);
        row.setCreatedAt(LocalDateTime.now());
        recognitionMapper.insert(row);
    }

    private static String defaultNote(String type, String targetName) {
        if (Recognition.TYPE_STAR_NOMINATION.equals(type)) {
            return "本月之星：" + targetName;
        }
        return Recognition.TYPE_PRAISE.equals(type) ? "公开表扬 " + targetName : "本周表现不错";
    }

    /** 我这个周期里给过认可的人，看板用来点亮对应的标记。 */
    private Set<Long> targetsOf(Long fromUserId, String type, String period) {
        return recognitionMapper.selectListByFromAndPeriod(fromUserId, type, period).stream()
                .map(Recognition::getToId).collect(Collectors.toSet());
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    /** 等我点评的：下属已提交、但我还没评过的周报。 */
    public List<ReportBrief> pendingReviews(Long userId, List<HrmEmployeeDO> team) {
        if (team.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDateTime windowStart = today.minusWeeks(3).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        Set<String> creators = team.stream().map(e -> String.valueOf(e.getUserId())).collect(Collectors.toSet());
        List<OaWorkReportDO> submitted = reportMapper.selectListByCreatorsAndStartTimeBetween(
                        creators, windowStart, today.atTime(LocalTime.MAX)).stream()
                .filter(row -> OaWorkReportStatusEnum.SUBMITTED.getStatus().equals(row.getStatus()))
                .toList();
        if (submitted.isEmpty()) {
            return List.of();
        }
        Set<Long> alreadyCommented = commentMapper.selectListByReviewer(userId,
                        submitted.stream().map(OaWorkReportDO::getId).toList())
                .stream().map(OaWorkReportCommentDO::getReportId).collect(Collectors.toSet());
        Map<String, String> namesByCreator = team.stream().collect(Collectors.toMap(
                e -> String.valueOf(e.getUserId()), e -> e.getName() == null ? "" : e.getName(), (a, b) -> a));
        return submitted.stream()
                .filter(row -> !alreadyCommented.contains(row.getId()))
                .sorted(Comparator.comparing(OaWorkReportDO::getStartTime, Comparator.reverseOrder()))
                .limit(20)
                .map(row -> new ReportBrief(row.getId(), row.getTitle(),
                        OaWorkReportTypeEnum.WEEKLY.formatPeriod(row.getStartTime()), row.getStatus(),
                        OaWorkReportStatusEnum.SUBMITTED.getName(), row.getCreateTime(),
                        namesByCreator.get(row.getCreator())))
                .toList();
    }

    public void requireManager(Long userId) {
        if (myTeam(userId).isEmpty()) {
            throw new BizException("你的档案里还没有直属下属，团队视图暂不可用");
        }
    }

    private static Long resolveOwner(String creator) {
        if (creator == null) {
            return null;
        }
        try {
            return Long.valueOf(creator);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static OaWorkReportDO preferSubmitted(OaWorkReportDO older, OaWorkReportDO newer) {
        boolean oldSubmitted = OaWorkReportStatusEnum.SUBMITTED.getStatus().equals(older.getStatus());
        boolean newSubmitted = OaWorkReportStatusEnum.SUBMITTED.getStatus().equals(newer.getStatus());
        if (oldSubmitted == newSubmitted) {
            return newer.getId() > older.getId() ? newer : older;
        }
        return newSubmitted ? newer : older;
    }

    private static List<TrackCellHeader> weekHeaders(LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TrackCellHeader> list = new ArrayList<>(BOARD_WEEKS);
        OaWorkReportTypeEnum weekly = OaWorkReportTypeEnum.WEEKLY;
        for (int i = BOARD_WEEKS - 1; i >= 0; i--) {
            LocalDate start = monday.minusWeeks(i);
            list.add(new TrackCellHeader(weekly.formatPeriod(start.atStartOfDay()),
                    start.getMonthValue() + "/" + start.getDayOfMonth(), start.toString()));
        }
        return list;
    }

    private Map<Long, String> nameOf(List<HrmEmployeeDO> team) {
        Map<Long, String> names = new HashMap<>();
        for (HrmEmployeeDO employee : team) {
            names.put(employee.getUserId(), employee.getName());
        }
        return names;
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

    /** 看板整体出参；star 是我本月提名的那位，没提名过就是 null */
    public record Board(MemberBrief me, List<Member> members, List<TrackCellHeader> weeks,
                        List<ReportBrief> pendingReviews, int submittedThisWeek, MemberBrief star) {
    }
}
