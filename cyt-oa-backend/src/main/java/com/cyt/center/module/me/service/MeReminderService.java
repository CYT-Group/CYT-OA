package com.cyt.center.module.me.service;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.mapper.MilestoneMapper;
import com.cyt.center.module.me.mapper.NotificationMapper;
import com.cyt.center.module.me.mapper.NotifyPreferenceMapper;
import com.cyt.center.module.me.model.cmd.MeCmds.PreferenceCmd;
import com.cyt.center.module.me.model.entity.Milestone;
import com.cyt.center.module.me.model.entity.Notification;
import com.cyt.center.module.me.model.entity.RhythmNode;
import com.cyt.center.module.me.model.entity.NotifyPreference;
import com.cyt.center.module.me.model.vo.MeVos.Reminder;
import com.cyt.center.module.me.model.vo.MeVos.ReminderPreference;
import com.cyt.center.module.oa.controller.admin.announcement.vo.OaAnnouncementPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.dal.mysql.workreport.OaWorkReportMapper;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportTypeEnum;
import com.cyt.center.module.oa.service.announcement.OaAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 我的提醒。提醒不是人工登记的留言，而是从真实业务状态算出来的：
 * 待交与逾期的周报、档案里的生日与入职周年、公司最新公告。
 * 每条都带去重键，重复同步只更新不叠加；员工可在偏好里关掉某几类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeReminderService {

    /** 里程碑提前多少天开始提醒 */
    private static final int MILESTONE_AHEAD_DAYS = 14;

    private final NotificationMapper notifications;
    private final NotifyPreferenceMapper preferences;
    private final MilestoneMapper milestones;
    private final OaWorkReportMapper reports;
    private final OaAnnouncementService announcements;
    private final HrmEmployeeService employees;
    private final MeRhythmService rhythm;

    public PageResult<Reminder> page(Long userId, String category, Boolean unreadOnly, PageParam pageParam) {
        PageResult<Notification> page = notifications.selectPageForUser(userId, category, unreadOnly, pageParam);
        return new PageResult<>(page.getList().stream().map(MeReminderService::toReminder).toList(),
                page.getTotal());
    }

    public List<Reminder> recent(Long userId, int limit) {
        return notifications.selectRecent(userId, limit).stream().map(MeReminderService::toReminder).toList();
    }

    public long unreadCount(Long userId) {
        Long count = notifications.selectUnreadCount(userId);
        return count == null ? 0L : count;
    }

    public void markRead(Long userId, Long id) {
        Notification row = notifications.selectById(id);
        if (row == null || !row.getUserId().equals(userId)) {
            return;
        }
        Notification patch = new Notification();
        patch.setId(id);
        patch.setReadFlag(true);
        notifications.updateById(patch);
    }

    public int markAllRead(Long userId, String category) {
        return notifications.markAllRead(userId, category);
    }

    public ReminderPreference preferences(Long userId) {
        NotifyPreference row = preferences.selectById(userId);
        if (row == null) {
            return new ReminderPreference(true, true, true, true);
        }
        return new ReminderPreference(flag(row.getReportDue()), flag(row.getOverdue()),
                flag(row.getMilestone()), flag(row.getCorpNews()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePreferences(Long userId, PreferenceCmd cmd) {
        NotifyPreference row = preferences.selectById(userId);
        boolean isNew = row == null;
        if (isNew) {
            row = new NotifyPreference();
            row.setUserId(userId);
            row.setReportDue(true);
            row.setOverdue(true);
            row.setMilestone(true);
            row.setCorpNews(true);
        }
        if (cmd.getReportDue() != null) {
            row.setReportDue(cmd.getReportDue());
        }
        if (cmd.getOverdue() != null) {
            row.setOverdue(cmd.getOverdue());
        }
        if (cmd.getMilestone() != null) {
            row.setMilestone(cmd.getMilestone());
        }
        if (cmd.getCorpNews() != null) {
            row.setCorpNews(cmd.getCorpNews());
        }
        if (isNew) {
            preferences.insert(row);
        } else {
            preferences.updateById(row);
        }
    }

    /**
     * 重新计算派生提醒。概览、徽标计数和提醒列表都先调用它，保证徽章和列表同一口径。
     * 单项数据源查不到时只跳过该来源，不影响其它提醒。
     */
    @Transactional(rollbackFor = Exception.class)
    public void sync(Long userId) {
        ReminderPreference preference = preferences(userId);
        cleanupApprovalReminders(userId);
        syncRhythmNodes(userId);
        if (preference.reportDue() || preference.overdue()) {
            syncWorkReports(userId, preference.overdue());
        }
        if (preference.milestone()) {
            syncMilestones(userId);
        }
        if (preference.corpNews()) {
            syncCorpNews(userId);
        }
    }

    /** 内部审批中心已退役，审批都在企业微信：清掉历史「待我审批」提醒（含旧样例类型），不再生成。 */
    private void cleanupApprovalReminders(Long userId) {
        notifications.deleteByDedupKeysNotIn(userId, "task_pending", List.of());
        notifications.deleteByDedupKeysNotIn(userId, "approval_pending", List.of());
    }

    private void syncWorkReports(Long userId, boolean includeOverdue) {
        String creator = String.valueOf(userId);
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();

        List<OaWorkReportDO> drafts = reports.selectDraftsByCreator(creator);
        OaWorkReportDO current = drafts.stream()
                .filter(report -> report.getStartTime() != null && !report.getStartTime().isBefore(weekStart))
                .findFirst().orElse(null);
        String currentKey = "report-current";
        if (current == null) {
            removeByDedupKey(userId, currentKey);
        } else {
            OaWorkReportTypeEnum type = OaWorkReportTypeEnum.valueOf(current.getType());
            String periodKey = type == null ? "" : type.formatPeriod(current.getStartTime());
            upsert(userId, Notification.CATEGORY_TODO, "report_due",
                    (type == null ? "汇报" : type.getName()) + " " + periodKey + " 还没提交",
                    "/me/report?tab=draft", currentKey);
        }

        if (!includeOverdue) {
            return;
        }
        long overdue = drafts.stream()
                .filter(report -> report.getStartTime() != null && report.getStartTime().isBefore(weekStart))
                .count();
        String overdueKey = "report-overdue";
        if (overdue <= 0) {
            removeByDedupKey(userId, overdueKey);
            return;
        }
        upsert(userId, Notification.CATEGORY_COMPLIANCE, "report_overdue",
                overdue + " 份汇报已逾期，建议补齐", "/me/report?tab=timeline", overdueKey);
    }

    /** 生日与入职周年按档案算，只提醒本人；同时把它们落到 milestones 表，供资料页的足迹使用。 */
    private void syncMilestones(Long userId) {
        HrmEmployeeDO employee = employees.getEmployeeByUserId(userId);
        if (employee == null) {
            return;
        }
        LocalDate today = LocalDate.now();

        LocalDate birthday = employee.getBirthday() == null
                ? null : nextYearly(employee.getBirthday().toLocalDate(), today);
        pruneDerived(userId, Milestone.TYPE_BIRTHDAY, birthday);
        if (birthday != null) {
            rememberMilestone(userId, Milestone.TYPE_BIRTHDAY, birthday, "生日 · " + birthday);
            if (!birthday.isAfter(today.plusDays(MILESTONE_AHEAD_DAYS))) {
                upsert(userId, Notification.CATEGORY_MILESTONE, "birthday",
                        "生日提醒：" + daysText(today, birthday), "/me/profile?tab=footprint",
                        "milestone-birthday-" + birthday);
            }
        }

        LocalDate anniversary = employee.getEntryTime() == null
                ? null : nextYearly(employee.getEntryTime().toLocalDate(), today);
        pruneDerived(userId, Milestone.TYPE_ANNIVERSARY, anniversary);
        if (anniversary != null) {
            int years = (int) ChronoUnit.YEARS.between(employee.getEntryTime().toLocalDate(), anniversary);
            rememberMilestone(userId, Milestone.TYPE_ANNIVERSARY, anniversary,
                    "入职 " + years + " 周年 · " + anniversary);
            if (!anniversary.isAfter(today.plusDays(MILESTONE_AHEAD_DAYS))) {
                upsert(userId, Notification.CATEGORY_MILESTONE, "anniversary",
                        "入职 " + years + " 周年就在 " + daysText(today, anniversary), "/me/reminder?cat=milestone",
                        "milestone-anniversary-" + anniversary);
            }
        }

        // 已转正就把转正那天当既成事实留着，未转正才用试用期长度推算出待评估的那天
        LocalDate probation;
        if (employee.getRegularTime() != null) {
            probation = employee.getRegularTime().toLocalDate();
        } else if (employee.getProbation() != null && employee.getProbation() > 0 && employee.getEntryTime() != null) {
            // 档案里 probation 单位是月（0 表示无试用期），与 HRM 侧 entryTime.plusMonths 保持一致
            probation = employee.getEntryTime().toLocalDate().plusMonths(employee.getProbation());
        } else {
            probation = null;
        }
        boolean probationPending = employee.getRegularTime() == null;
        pruneDerived(userId, Milestone.TYPE_PROBATION, probation);
        if (probation != null) {
            rememberMilestone(userId, Milestone.TYPE_PROBATION, probation,
                    (probationPending ? "转正评估 · " : "转正 · ") + probation);
            if (probationPending && !probation.isBefore(today)
                    && !probation.isAfter(today.plusDays(MILESTONE_AHEAD_DAYS))) {
                upsert(userId, Notification.CATEGORY_COMPLIANCE, "probation",
                        "试用期 " + probation + " 到期，记得准备转正材料", "/me/profile?tab=basic",
                        "milestone-probation-" + probation);
            }
        }
    }

    /**
     * 派生出来的里程碑只认档案当前算出的那一天：档案改了、日期滚到下一年，旧行和旧提醒都要跟着消失，
     * 否则足迹里会挂着一堆对不上档案的记录。三个标记串在 milestones 和 notifications 里是同一个词。
     */
    private void pruneDerived(Long userId, String type, LocalDate date) {
        milestones.deleteDerivedNotDate(userId, type, date);
        notifications.deleteByDedupKeysNotIn(userId, type,
                date == null ? List.of() : List.of("milestone-" + type + "-" + date));
    }

    /** 关注的大促 / 新品节点临近，按节点各自的提前天数提醒；取消关注后这条会自动消失。 */
    private void syncRhythmNodes(Long userId) {
        LocalDate today = LocalDate.now();
        List<String> keepKeys = new ArrayList<>();
        for (RhythmNode node : rhythm.upcomingFollowedNodes(userId, today)) {
            String key = "rhythm-" + node.getId();
            keepKeys.add(key);
            upsert(userId, Notification.CATEGORY_MILESTONE, "rhythm",
                    node.getName() + " " + daysText(today, node.getStartDate()) + "开始", "/me/rhythm", key);
        }
        notifications.deleteByDedupKeysNotIn(userId, "rhythm", keepKeys);
    }

    private void syncCorpNews(Long userId) {
        List<OaAnnouncementDO> latest;
        try {
            OaAnnouncementPageReqVO reqVO = new OaAnnouncementPageReqVO();
            reqVO.setPageNo(1);
            reqVO.setPageSize(3);
            latest = announcements.getPublishedAnnouncementPage(reqVO, userId).getList();
        } catch (Exception ex) {
            log.warn("[syncCorpNews][用户 {} 的公告查询失败]", userId, ex);
            return;
        }
        for (OaAnnouncementDO item : latest) {
            String key = "announce-" + item.getId();
            upsertOnce(userId, Notification.CATEGORY_CORP, "announcement",
                    item.getTitle(), "/page/oa-announcement", key);
        }
    }

    /** 已存在且标题未变时保持已读状态；标题变了说明事情本身变了，重新置为未读。 */
    private void upsert(Long userId, String category, String type, String title, String link, String dedupKey) {
        Notification existing = notifications.selectByDedupKey(userId, dedupKey);
        if (existing == null) {
            insert(userId, category, type, title, link, dedupKey);
            return;
        }
        if (title.equals(existing.getTitle())) {
            return;
        }
        existing.setTitle(title);
        existing.setLink(link);
        existing.setCategory(category);
        existing.setType(type);
        existing.setReadFlag(false);
        notifications.updateById(existing);
    }

    /** 一次性提醒：同一来源只生成一次，读过的不再弹回来。 */
    private void upsertOnce(Long userId, String category, String type, String title, String link, String dedupKey) {
        if (notifications.selectByDedupKey(userId, dedupKey) == null) {
            insert(userId, category, type, title, link, dedupKey);
        }
    }

    private void insert(Long userId, String category, String type, String title, String link, String dedupKey) {
        Notification row = new Notification();
        row.setUserId(userId);
        row.setCategory(category);
        row.setType(type);
        row.setTitle(title);
        row.setLink(link);
        row.setReadFlag(false);
        row.setChannel("site");
        row.setDedupKey(dedupKey);
        row.setCreatedAt(LocalDateTime.now());
        notifications.insert(row);
    }

    private void removeByDedupKey(Long userId, String dedupKey) {
        Notification existing = notifications.selectByDedupKey(userId, dedupKey);
        if (existing != null) {
            notifications.deleteById(existing.getId());
        }
    }

    private void rememberMilestone(Long userId, String type, LocalDate date, String title) {
        if (milestones.selectByTypeAndDate(userId, type, date) != null) {
            return;
        }
        Milestone row = new Milestone();
        row.setUserId(userId);
        row.setType(type);
        row.setMilestoneDate(date);
        row.setTitle(title);
        row.setCreatedAt(LocalDateTime.now());
        milestones.insert(row);
    }

    /** 每年重复的那一天，今年已过就取明年；当天算今天。同事里程碑带用同一条规则。 */
    public static LocalDate nextYearly(LocalDate origin, LocalDate today) {
        LocalDate candidate = origin.withYear(today.getYear());
        return candidate.isBefore(today) ? candidate.plusYears(1) : candidate;
    }

    private static String daysText(LocalDate today, LocalDate target) {
        long days = ChronoUnit.DAYS.between(today, target);
        if (days == 0) {
            return "今天";
        }
        return days + " 天后（" + target + "）";
    }

    private static boolean flag(Boolean value) {
        return value == null || value;
    }

    static Reminder toReminder(Notification row) {
        return new Reminder(row.getId(), row.getCategory(), row.getType(), row.getTitle(), row.getLink(),
                Boolean.TRUE.equals(row.getReadFlag()), row.getChannel(), row.getCreatedAt());
    }

    /** 供徽标统计：仍未提交的草稿汇报条数。 */
    public long countDraftReports(Long userId) {
        return reports.selectDraftsByCreator(String.valueOf(userId)).size();
    }
}
