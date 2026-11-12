package com.cyt.center.module.me.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportPageReqVO;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportRespVO;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportSaveReqVO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportCommentDO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.dal.postgresql.workreport.OaWorkReportCommentMapper;
import com.cyt.center.module.oa.dal.postgresql.workreport.OaWorkReportMapper;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportStatusEnum;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportTypeEnum;
import com.cyt.center.module.oa.service.workreport.OaWorkReportService;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cyt.center.module.me.model.cmd.MeCmds.CommentCmd;
import com.cyt.center.module.me.model.cmd.MeCmds.CommentReplyCmd;
import com.cyt.center.module.me.model.cmd.MeCmds.ReportCmd;
import com.cyt.center.module.me.model.vo.MeVos.Report;
import com.cyt.center.module.me.model.vo.MeVos.ReportBrief;
import com.cyt.center.module.me.model.vo.MeVos.ReportComment;
import com.cyt.center.module.me.model.vo.MeVos.TrackCell;
import com.cyt.center.module.me.model.vo.MeVos.WorkItem;

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

/**
 * 我的报告。正文沿用移植过来的 oa_work_report，点评与回复是挂在它下面的新表；
 * 周期起止、标题、单号都由后端按汇报类型算出来，不让人工填。
 */
@Service
@RequiredArgsConstructor
public class MeReportService {

    private final OaWorkReportService reportService;
    private final OaWorkReportMapper reportMapper;
    private final OaWorkReportCommentMapper commentMapper;
    private final HrmEmployeeService employees;
    private final AdminUserApi userApi;
    private final DeptApi deptApi;

    public PageResult<Report> page(Long userId, Integer type, Integer status, PageParam pageParam) {
        OaWorkReportPageReqVO reqVO = new OaWorkReportPageReqVO();
        reqVO.setPageNo(pageParam.getPageNo());
        reqVO.setPageSize(pageParam.getPageSize());
        reqVO.setType(type);
        reqVO.setStatus(status);
        PageResult<OaWorkReportDO> page = reportService.getWorkReportPage(reqVO, userId);
        List<Report> list = enrich(page.getList());
        return new PageResult<>(list, page.getTotal());
    }

    public List<ReportBrief> recent(Long userId, int limit) {
        OaWorkReportPageReqVO reqVO = new OaWorkReportPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(limit);
        reqVO.setStatus(OaWorkReportStatusEnum.SUBMITTED.getStatus());
        List<Report> list = enrich(reportService.getWorkReportPage(reqVO, userId).getList());
        return list.stream()
                .sorted(Comparator.comparing(Report::createTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .map(r -> new ReportBrief(r.id(), r.title(), r.periodKey(), r.status(), r.statusText(),
                        r.createTime(), r.authorName()))
                .toList();
    }

    /** 待提交：草稿状态的汇报，本周待交与历史逾期都在里面。 */
    public List<Report> drafts(Long userId) {
        return enrich(reportMapper.selectDraftsByCreator(String.valueOf(userId)));
    }

    public Report detail(Long userId, Long id) {
        return enrich(List.of(reportService.getWorkReport(id, userId))).get(0);
    }

    /** 导出用响应体：本人或已授权的上级才拿得到，姓名与部门在这里补齐。 */
    public OaWorkReportRespVO exportView(Long userId, Long id) {
        OaWorkReportDO row = reportService.getWorkReport(id, userId);
        OaWorkReportTypeEnum type = OaWorkReportTypeEnum.valueOf(row.getType());
        OaWorkReportRespVO respVO = BeanUtils.toBean(row, OaWorkReportRespVO.class)
                .setUserId(parseUserId(row.getCreator()))
                .setUserName(userName(parseUserId(row.getCreator())))
                .setPeriodKey(type == null || row.getStartTime() == null ? null : type.formatPeriod(row.getStartTime()));
        if (row.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(row.getDeptId());
            respVO.setDeptName(dept == null ? null : dept.getName());
        }
        return respVO;
    }

    /**
     * 保存草稿：已存在同周期的草稿就更新，否则新建。周期时间按类型推导，
     * 前端只填正文，避免一周被交两次或周期填歪。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(Long userId, ReportCmd cmd) {
        LocalDateTime[] period = periodOf(cmd.getType(), cmd.getId());
        OaWorkReportSaveReqVO saveReqVO = new OaWorkReportSaveReqVO();
        saveReqVO.setId(cmd.getId());
        saveReqVO.setType(cmd.getType());
        saveReqVO.setTitle(cmd.getTitle());
        saveReqVO.setStartTime(period[0]);
        saveReqVO.setEndTime(period[1]);
        saveReqVO.setSummary(cmd.getSummary());
        saveReqVO.setPlan(cmd.getPlan());
        saveReqVO.setProblem(cmd.getProblem());
        saveReqVO.setRemark(cmd.getRemark());
        saveReqVO.setWorkItems(toWorkItems(cmd));
        saveReqVO.setPlanItems(toPlanItems(cmd));
        if (cmd.getId() != null) {
            reportService.updateWorkReport(saveReqVO, userId);
            return cmd.getId();
        }
        return reportService.createWorkReport(saveReqVO, userId);
    }

    public void submit(Long userId, Long id) {
        reportService.submitWorkReport(id, userId);
    }

    public void cancel(Long userId, Long id) {
        reportService.cancelWorkReport(id, userId);
    }

    /**
     * 12 周提交轨迹。一格一状态：submitted 已交、draft 草稿、missing 未交；
     * 未来周不生成格子。
     */
    public List<TrackCell> track(Long userId, Integer type, int weeks) {
        int safeWeeks = Math.max(1, Math.min(weeks, 52));
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime windowStart = weekStart.minusWeeks(safeWeeks - 1L);
        List<OaWorkReportDO> rows = reportMapper.selectListByCreatorsAndStartTimeBetween(
                List.of(String.valueOf(userId)), windowStart, weekStart.plusWeeks(1).minusNanos(1));
        Map<String, OaWorkReportDO> byPeriod = new HashMap<>();
        OaWorkReportTypeEnum weekly = OaWorkReportTypeEnum.valueOf(type == null
                ? OaWorkReportTypeEnum.WEEKLY.getType() : type);
        for (OaWorkReportDO row : rows) {
            if (weekly == null || !weekly.getType().equals(row.getType()) || row.getStartTime() == null) {
                continue;
            }
            byPeriod.putIfAbsent(weekly.formatPeriod(row.getStartTime()), row);
        }
        List<TrackCell> cells = new ArrayList<>(safeWeeks);
        for (int i = 0; i < safeWeeks; i++) {
            LocalDateTime monday = windowStart.plusWeeks(i);
            String periodKey = weekly.formatPeriod(monday);
            OaWorkReportDO row = byPeriod.get(periodKey);
            String state = row == null ? "missing"
                    : OaWorkReportStatusEnum.SUBMITTED.getStatus().equals(row.getStatus()) ? "submitted" : "draft";
            cells.add(new TrackCell(periodKey, labelOf(monday), state, row == null ? null : row.getId()));
        }
        return cells;
    }

    /** 收到的点评：把「谁在等我回复」和「我的报告被怎么评」一次给全。 */
    public List<ReportComment> commentsOf(Long reportId) {
        return toComments(commentMapper.selectListByReportId(reportId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long comment(Long reviewerId, CommentCmd cmd) {
        OaWorkReportDO report = reportMapper.selectById(cmd.getReportId());
        if (report == null) {
            throw new BizException("汇报不存在");
        }
        if (!canReview(reviewerId, report)) {
            throw new BizException("只能点评自己团队成员的汇报");
        }
        if (commentMapper.selectByReportAndReviewer(cmd.getReportId(), reviewerId) != null) {
            throw new BizException("这份汇报你已经点评过了");
        }
        OaWorkReportCommentDO row = new OaWorkReportCommentDO();
        row.setReportId(cmd.getReportId());
        row.setReviewerId(reviewerId);
        row.setGrade(cmd.getGrade());
        row.setReviewText(cmd.getReviewText());
        commentMapper.insert(row);
        return row.getId();
    }

    /** 回复只对汇报作者开放，且只能回自己报告下的点评。 */
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long userId, CommentReplyCmd cmd) {
        OaWorkReportCommentDO comment = commentMapper.selectById(cmd.getCommentId());
        if (comment == null) {
            throw new BizException("点评不存在");
        }
        OaWorkReportDO report = reportMapper.selectById(comment.getReportId());
        if (report == null || !String.valueOf(userId).equals(report.getCreator())) {
            throw new BizException("只能回复自己汇报下的点评");
        }
        OaWorkReportCommentDO patch = new OaWorkReportCommentDO();
        patch.setId(comment.getId());
        patch.setReply(cmd.getReply());
        patch.setReplyTime(LocalDateTime.now());
        commentMapper.updateById(patch);
    }

    /** 上级判断走人事档案的直属上级字段，网关下发部门负责人后可再叠加。 */
    private boolean canReview(Long reviewerId, OaWorkReportDO report) {
        if (String.valueOf(reviewerId).equals(report.getCreator())) {
            return false;
        }
        HrmEmployeeDO author = employees.getEmployeeByUserId(Long.valueOf(report.getCreator()));
        if (author == null || author.getLeaderEmployeeId() == null) {
            return false;
        }
        HrmEmployeeDO reviewer = employees.getEmployeeByUserId(reviewerId);
        return reviewer != null && author.getLeaderEmployeeId().equals(reviewer.getId());
    }

    private List<Report> enrich(List<OaWorkReportDO> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> reportIds = rows.stream().map(OaWorkReportDO::getId).toList();
        Map<Long, List<ReportComment>> commentsByReport = new HashMap<>();
        for (OaWorkReportCommentDO comment : commentMapper.selectListByReportIds(reportIds)) {
            commentsByReport.computeIfAbsent(comment.getReportId(), k -> new ArrayList<>())
                    .add(toComment(comment));
        }
        Map<Long, String> authorNames = new HashMap<>();
        List<Report> list = new ArrayList<>(rows.size());
        for (OaWorkReportDO row : rows) {
            OaWorkReportTypeEnum type = OaWorkReportTypeEnum.valueOf(row.getType());
            OaWorkReportStatusEnum status = OaWorkReportStatusEnum.valueOf(row.getStatus());
            Long authorId = parseUserId(row.getCreator());
            String authorName = authorId == null ? null
                    : authorNames.computeIfAbsent(authorId, this::userName);
            list.add(new Report(row.getId(), row.getNo(), row.getType(), type == null ? "汇报" : type.getName(),
                    row.getStatus(), status == null ? "" : status.getName(), row.getTitle(),
                    type == null || row.getStartTime() == null ? null : type.formatPeriod(row.getStartTime()),
                    row.getStartTime(), row.getEndTime(), row.getCreateTime(),
                    row.getSummary(), row.getPlan(), row.getProblem(), row.getRemark(),
                    toWorkItemViews(row.getWorkItems()), toPlanItemViews(row.getPlanItems()), row.getFileUrls(),
                    authorName, commentsByReport.getOrDefault(row.getId(), List.of())));
        }
        return list;
    }

    private List<ReportComment> toComments(List<OaWorkReportCommentDO> rows) {
        List<ReportComment> list = new ArrayList<>(rows.size());
        for (OaWorkReportCommentDO row : rows) {
            list.add(toComment(row));
        }
        return list;
    }

    private ReportComment toComment(OaWorkReportCommentDO row) {
        return new ReportComment(row.getId(), row.getReviewerId(), userName(row.getReviewerId()),
                row.getGrade(), row.getReviewText(), row.getReply(), row.getReplyTime(), row.getCreateTime());
    }

    private String userName(Long userId) {
        if (userId == null) {
            return null;
        }
        var user = userApi.getUser(userId);
        return user == null ? null : user.getNickname();
    }

    private static Long parseUserId(String creator) {
        if (creator == null || creator.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(creator);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<WorkItem> toWorkItemViews(List<OaWorkReportDO.WorkItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(item -> new WorkItem(item.getContent(), item.getProgress())).toList();
    }

    private static List<String> toPlanItemViews(List<OaWorkReportDO.PlanItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(OaWorkReportDO.PlanItem::getContent).toList();
    }

    private static List<OaWorkReportSaveReqVO.WorkItem> toWorkItems(ReportCmd cmd) {
        List<OaWorkReportSaveReqVO.WorkItem> list = new ArrayList<>();
        if (cmd.getWorkItems() == null) {
            return list;
        }
        for (ReportCmd.WorkItemCmd item : cmd.getWorkItems()) {
            if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            OaWorkReportSaveReqVO.WorkItem target = new OaWorkReportSaveReqVO.WorkItem();
            target.setContent(item.getContent().trim());
            target.setProgress(item.getProgress() == null ? 100 : Math.max(0, Math.min(item.getProgress(), 100)));
            list.add(target);
        }
        return list;
    }

    private static List<OaWorkReportSaveReqVO.PlanItem> toPlanItems(ReportCmd cmd) {
        List<OaWorkReportSaveReqVO.PlanItem> list = new ArrayList<>();
        if (cmd.getPlanItems() == null) {
            return list;
        }
        for (String content : cmd.getPlanItems()) {
            if (content == null || content.isBlank()) {
                continue;
            }
            OaWorkReportSaveReqVO.PlanItem item = new OaWorkReportSaveReqVO.PlanItem();
            item.setContent(content.trim());
            list.add(item);
        }
        return list;
    }

    /** 周期推导：编辑已有草稿时沿用原周期，新建时按类型取当前周期。 */
    private LocalDateTime[] periodOf(Integer type, Long id) {
        if (id != null) {
            OaWorkReportDO existing = reportMapper.selectById(id);
            if (existing == null) {
                throw new BizException("汇报不存在");
            }
            return new LocalDateTime[]{existing.getStartTime(), existing.getEndTime()};
        }
        OaWorkReportTypeEnum kind = OaWorkReportTypeEnum.valueOf(type);
        if (kind == null) {
            throw new BizException("汇报类型不正确");
        }
        LocalDate today = LocalDate.now();
        return switch (kind) {
            case DAILY -> new LocalDateTime[]{today.atStartOfDay(), today.atTime(LocalTime.MAX)};
            case WEEKLY -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new LocalDateTime[]{monday.atStartOfDay(), monday.plusDays(6).atTime(LocalTime.MAX)};
            }
            case MONTHLY -> {
                YearMonth month = YearMonth.from(today);
                yield new LocalDateTime[]{month.atDay(1).atStartOfDay(), month.atEndOfMonth().atTime(LocalTime.MAX)};
            }
        };
    }

    private static String labelOf(LocalDateTime weekStart) {
        return weekStart.getMonthValue() + "/" + weekStart.getDayOfMonth();
    }
}
