package com.cyt.center.module.me.service;

import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.me.model.entity.RhythmNode;
import com.cyt.center.module.me.model.vo.MeVos.Greeting;
import com.cyt.center.module.me.model.vo.MeVos.Overview;
import com.cyt.center.module.me.model.vo.MeVos.ReportBrief;
import com.cyt.center.module.me.model.vo.MeVos.Reminder;
import com.cyt.center.module.me.model.vo.MeVos.Report;
import com.cyt.center.module.me.model.vo.MeVos.Summary;
import com.cyt.center.module.me.model.vo.MeVos.Summary.Counts;
import com.cyt.center.module.me.model.vo.MeVos.TodoGroup;
import com.cyt.center.module.me.model.vo.MeVos.TodoGroup.Item;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 概览与侧边栏徽标。数字与列表取自同一批查询，保证「徽章写 2、点进去也是 2 条」；
 * 提醒先按真实业务状态同步，再取未读数。
 */
@Service
@RequiredArgsConstructor
public class MeSummaryService {

    private static final int GROUP_ITEM_LIMIT = 3;

    private final MeReminderService reminders;
    private final MeReportService reports;
    private final MeTeamService team;
    private final MeRhythmService rhythm;
    private final HrmEmployeeService employees;
    private final DeptApi deptApi;
    private final AdminUserApi userApi;

    public Summary summary(Long userId) {
        reminders.sync(userId);
        HrmEmployeeDO employee = employees.getEmployeeByUserId(userId);
        int teamCount = team.myTeam(userId).size();
        int report = (int) reminders.countDraftReports(userId);
        int review = team.pendingReviews(userId, team.myTeam(userId)).size();
        int unread = (int) reminders.unreadCount(userId);
        return new Summary(employee == null ? null : deptName(employee.getDeptId()),
                employee == null ? null : employee.getPostName(), teamCount,
                new Counts(unread, report, review));
    }

    public Overview overview(Long userId) {
        reminders.sync(userId);
        List<TodoGroup> todos = new ArrayList<>();

        List<Report> drafts = reports.drafts(userId);
        addGroup(todos, "report", "待交汇报", drafts.size(), "/me/report?tab=draft",
                drafts.stream().limit(GROUP_ITEM_LIMIT)
                        .map(row -> new Item(text(row.periodKey(), row.typeText()), text(row.title(), "还没写标题"),
                                "/me/report?tab=draft"))
                        .toList());

        List<HrmEmployeeDO> members = team.myTeam(userId);
        List<ReportBrief> pendingReviews = team.pendingReviews(userId, members);
        addGroup(todos, "review", "等我的点评", pendingReviews.size(), "/me/team?tab=review",
                pendingReviews.stream().limit(GROUP_ITEM_LIMIT)
                        .map(row -> new Item(text(row.authorName(), "团队成员"),
                                row.periodKey() + " 已提交", "/me/team?tab=review"))
                        .toList());

        List<RhythmNode> upcoming = rhythm.upcomingFollowedNodes(userId, LocalDate.now());
        addGroup(todos, "rhythm", "关注节点临近", upcoming.size(), "/me/rhythm",
                upcoming.stream().limit(GROUP_ITEM_LIMIT)
                        .map(node -> new Item(node.getName(), node.getStartDate() + " 开始", "/me/rhythm"))
                        .toList());

        List<Reminder> latest = reminders.recent(userId, 5);
        return new Overview(greeting(userId), rhythm.current(userId), todos,
                reports.recent(userId, 5), latest);
    }

    private void addGroup(List<TodoGroup> groups, String key, String title, int count, String link, List<Item> items) {
        if (count > 0) {
            groups.add(new TodoGroup(key, title, count, link, items));
        }
    }

    private Greeting greeting(Long userId) {
        int hour = LocalTime.now().getHour();
        String prefix = hour < 11 ? "早上好" : hour < 14 ? "中午好" : hour < 18 ? "下午好" : "晚上好";
        HrmEmployeeDO employee = employees.getEmployeeByUserId(userId);
        String name = employee != null ? employee.getName() : userName(userId);
        LocalDate today = LocalDate.now();
        return new Greeting(prefix + "，" + text(name, "同学"), today.toString(),
                weekdayText(today.getDayOfWeek()), name);
    }

    private static String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String weekdayText(DayOfWeek dayOfWeek) {
        return "周" + "一二三四五六日".charAt(dayOfWeek.getValue() - 1);
    }

    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        var dept = deptApi.getDept(deptId);
        return dept == null ? null : dept.getName();
    }

    private String userName(Long userId) {
        var user = userApi.getUser(userId);
        return user == null ? null : user.getNickname();
    }
}
