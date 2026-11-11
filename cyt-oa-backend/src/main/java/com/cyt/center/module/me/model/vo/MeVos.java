package com.cyt.center.module.me.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人中心各视图的出参。全部为只读投影，字段名即前端 JSON 键名。
 */
public final class MeVos {

    private MeVos() {
    }

    /** GET /api/me/summary —— 侧边栏徽章与身份摘要 */
    public record Summary(String deptName, String jobTitle, int teamCount, Counts counts) {
        public record Counts(int reminder, int report, int team) {
        }
    }

    /** GET /api/me/overview —— 概览：节奏横幅 + 统一待办 + 最近报告 + 最新提醒 */
    public record Overview(Greeting greeting, RhythmNodeView currentRhythm, List<TodoGroup> todos,
                           List<ReportBrief> recentReports, List<Reminder> latestReminders) {
    }

    public record Greeting(String text, String date, String weekday, String name) {
    }

    /** 统一待办的一组，如「待交汇报 2」 */
    public record TodoGroup(String key, String title, int count, String link, List<Item> items) {
        public record Item(String title, String desc, String link) {
        }
    }

    public record ReportBrief(Long id, String title, String periodKey, Integer status,
                              String statusText, LocalDateTime submittedTime, String authorName) {
    }

    /** GET /api/me/rhythm/nodes —— 大促·新品节点 */
    public record RhythmNodeView(Long id, String name, String kind, String kindText,
                                 LocalDate startDate, LocalDate endDate,
                                 String state, Integer daysLeft, String note,
                                 List<DeptAction> deptActions, List<String> linkedRefs,
                                 Boolean followed) {
    }

    public record DeptAction(String dept, List<String> items) {
    }

    /** 办事表单里的下拉项：办公用品等台账选值 */
    public record Option(Long value, String label, String hint) {
    }

    /** GET /api/me/reminders */
    public record Reminder(Long id, String category, String type, String title, String link,
                           Boolean read, String channel, LocalDateTime createdAt) {
    }

    /** GET /api/me/reminders/preferences */
    public record ReminderPreference(boolean reportDue, boolean overdue, boolean milestone, boolean corpNews) {
    }

    /** GET /api/me/reports、/api/me/reports/{id} */
    public record Report(Long id, String no, Integer type, String typeText, Integer status, String statusText,
                         String title, String periodKey, LocalDateTime startTime, LocalDateTime endTime,
                         LocalDateTime createTime, String summary, String plan, String problem, String remark,
                         List<WorkItem> workItems, List<String> planItems, List<String> fileUrls,
                         String authorName, List<ReportComment> comments) {
    }

    public record WorkItem(String content, Integer progress) {
    }

    /** 报告点评与员工回复 */
    public record ReportComment(Long id, Long reviewerId, String reviewerName, String grade,
                                String reviewText, String reply, LocalDateTime replyTime,
                                LocalDateTime createTime) {
    }

    /** GET /api/me/reports/track —— 12 周提交轨迹 */
    public record TrackCell(String periodKey, String label, String state, Long reportId) {
    }

    /** GET /api/me/applies —— 我发起的办事单（跨类型统一投影） */
    public record ApplyItem(String type, String typeText, Long id, String no, String title,
                            Integer status, String statusText, String amount,
                            LocalDateTime createTime, LocalDateTime startTime, LocalDateTime endTime,
                            String processInstanceId) {
    }

    /** GET /api/me/profile */
    public record Profile(String name, String jobNumber, String deptName, String postName, String postLevel,
                          LocalDate birthday, LocalDate entryTime, Integer workAge, String workCity,
                          Integer status, Integer entryStatus, Integer probation, LocalDate regularTime,
                          String managerName, int subCount, Footprint footprint, ReminderPreference preference) {
    }

    /** GET /api/me/footprint —— 我在系统里留下的痕迹 */
    public record Footprint(int reports, int submittedReports, int applies,
                            int recognitions, int milestones, int attendanceRecords, int voices) {
    }

    public record RecognitionView(Long id, String type, String typeText, Long fromId, String fromName,
                                  String scope, String note, String period, LocalDateTime createdAt) {
    }

    public record MilestoneView(Long id, String type, String typeText, LocalDate date, String title,
                                Integer daysLeft) {
    }

    /**
     * 同事 / 下属临近的里程碑。action 决定前端给哪个按钮：bless 是送祝福，arrange 是主管要提前安排的事
     * （转正评估不能祝福，只能安排述职）；祝福是否已送出看 blessed。
     */
    public record ColleagueMilestoneView(Long userId, String name, String deptName, String type, String typeText,
                                         LocalDate date, Integer daysLeft, String relation, Boolean blessed,
                                         String action) {
    }

    public record VoiceView(Long id, String channel, String channelText, Boolean anonymous,
                            String authorName, String content, String status, LocalDateTime createdAt) {
    }

    public record MemberBrief(Long userId, String name) {
    }

    /** liked / starred 是本周期我有没有给过他认可，看板认可列据此点亮 */
    public record Member(Long userId, Long employeeId, String name, String postName, String deptName,
                         LocalDate entryTime, List<TrackCell> cells, Boolean liked, Boolean starred) {
    }

    public record TrackCellHeader(String periodKey, String label, String weekStart) {
    }
}
