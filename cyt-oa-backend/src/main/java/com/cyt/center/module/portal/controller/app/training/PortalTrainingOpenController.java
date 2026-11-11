package com.cyt.center.module.portal.controller.app.training;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingCatalogCourseVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingCatalogRespVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingCourseRespVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingGroupRespVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingLessonMediaRespVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingLessonRespVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingProgressReqVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingProgressRespVO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonProgressDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingProgressDO;
import com.cyt.center.module.portal.service.training.PortalTrainingService;
import com.cyt.center.module.portal.service.training.PortalTrainingService.CatalogGroup;
import com.cyt.center.module.portal.service.training.PortalTrainingService.CourseDetail;
import com.cyt.center.module.portal.service.training.PortalTrainingService.GroupCard;
import com.cyt.center.module.portal.service.training.PortalTrainingService.ProgressResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 培训中心读侧接口（登录即可，无能力位要求）。
 *
 * 为什么单开一套而不是让门户直接调 /admin-api：管理令牌要求 device=admin，门户的登录人拿的是普通令牌，
 * 读侧必须走 /api/**（口径 docs/Portal管理面迁移契约.md §二）。可见性规则跟管理面同一条用例，
 * 不在这儿另写一份。
 */
@Tag(name = "门户 - 培训中心（读）")
@RestController
@RequestMapping("/api/portal/training")
@Validated
@RequiredArgsConstructor
public class PortalTrainingOpenController {

    private final PortalTrainingService trainingService;

    @GetMapping("/groups")
    @Operation(summary = "大类入口页：小类数/课程数/小类名预览（只算本人可见课程，空大类不返回）")
    public Result<List<PortalTrainingGroupRespVO>> groups() {
        return success(trainingService.groups(getLoginUserId()).stream().map(this::toGroupVO).toList());
    }

    @GetMapping("/catalog")
    @Operation(summary = "课程中心：published 且部门命中的课程按小类分组 + 本人进度")
    @Parameter(name = "group", description = "大类 key；__unassigned__ = 未归类；不传 = 全部")
    public Result<List<PortalTrainingCatalogRespVO>> catalog(
            @RequestParam(value = "group", required = false) String group) {
        List<PortalTrainingCatalogRespVO> list = trainingService.catalog(getLoginUserId(), group).stream()
                .map(this::toCatalogVO).toList();
        return success(list);
    }

    @GetMapping("/get")
    @Operation(summary = "课程详情：课程 + 课时列表 + 本人各课时进度")
    @Parameter(name = "id", description = "课程编号", required = true)
    public Result<PortalTrainingCourseRespVO> get(@RequestParam("id") Long id) {
        CourseDetail detail = trainingService.courseDetail(getLoginUserId(), id);
        PortalTrainingCourseRespVO vo = BeanUtils.toBean(detail.course(), PortalTrainingCourseRespVO.class);
        vo.setLessonCount(detail.lessons().size());
        vo.setLessons(detail.lessons().stream()
                .map(l -> toLessonVO(l, detail.mine().get(l.getId()))).toList());
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "老列表（保留兼容）：status 默认 published，非 published 只有 can_view_any_status 看得到")
    public Result<PageResult<PortalTrainingCourseRespVO>> page(
            @RequestParam(value = "status", required = false) String status, PageParam pageParam) {
        PageResult<PortalTrainingDO> rows = trainingService.openPage(getLoginUserId(), status, pageParam);
        return success(BeanUtils.toBean(rows, PortalTrainingCourseRespVO.class));
    }

    @PostMapping("/lessons/{lessonId}/progress")
    @Operation(summary = "课时进度心跳：percent 只增不减；video ≥90 / 其余 ≥100 记完成")
    public Result<PortalTrainingProgressRespVO> progress(@PathVariable("lessonId") Long lessonId,
                                                         @Valid @RequestBody PortalTrainingProgressReqVO reqVO) {
        ProgressResp resp = trainingService.reportProgress(getLoginUserId(), lessonId, reqVO);
        PortalTrainingProgressRespVO vo = new PortalTrainingProgressRespVO();
        vo.setLessonId(resp.lessonId());
        vo.setStatus(resp.status());
        vo.setPosition(resp.position());
        vo.setPercent(resp.percent());
        vo.setDurationSec(resp.durationSec());
        vo.setCompletedAt(resp.completedAt());
        vo.setCourseCompleted(resp.courseCompleted());
        return success(vo);
    }

    @GetMapping("/lessons/{lessonId}/media")
    @Operation(summary = "课时媒体预签名：video/pdf/html 型 article → url（article 另带 pdfUrl）；cards/图片序列 → pages；link → url")
    public Result<PortalTrainingLessonMediaRespVO> media(@PathVariable("lessonId") Long lessonId) {
        PortalTrainingService.LessonMediaResp resp = trainingService.lessonMedia(getLoginUserId(), lessonId);
        PortalTrainingLessonMediaRespVO vo = new PortalTrainingLessonMediaRespVO();
        vo.setUrl(resp.url());
        vo.setPdfUrl(resp.pdfUrl());
        vo.setPages(resp.pages());
        vo.setExpiresIn(resp.expiresIn());
        return success(vo);
    }

    @GetMapping("/my-progress")
    @Operation(summary = "当前用户的课程级学习进度列表（老表）")
    public Result<List<PortalTrainingProgressDO>> myProgress() {
        return success(trainingService.myProgress(getLoginUserId()));
    }

    @PostMapping("/start")
    @Operation(summary = "标记开始学习（幂等，无行才建 started）")
    @Parameter(name = "id", description = "课程编号", required = true)
    public Result<Boolean> start(@RequestParam("id") Long id) {
        trainingService.start(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/complete")
    @Operation(summary = "标记完成学习（无行建 completed，有行改状态+完成时间）")
    @Parameter(name = "id", description = "课程编号", required = true)
    public Result<Boolean> complete(@RequestParam("id") Long id) {
        trainingService.complete(getLoginUserId(), id);
        return success(true);
    }

    // ══════════════ 序列化 ══════════════

    private PortalTrainingGroupRespVO toGroupVO(GroupCard card) {
        PortalTrainingGroupRespVO vo = new PortalTrainingGroupRespVO();
        vo.setKey(card.key());
        vo.setName(card.name());
        vo.setIcon(card.icon());
        vo.setDescription(card.description());
        vo.setCategoryCount(card.categoryCount());
        vo.setCourseCount(card.courseCount());
        vo.setCategories(card.categories());
        return vo;
    }

    private PortalTrainingCatalogRespVO toCatalogVO(CatalogGroup group) {
        PortalTrainingCatalogRespVO vo = new PortalTrainingCatalogRespVO();
        vo.setCategory(group.category());
        vo.setCourses(group.courses().stream().map(c -> {
            PortalTrainingCatalogCourseVO cv = new PortalTrainingCatalogCourseVO();
            cv.setId(c.id());
            cv.setTitle(c.title());
            cv.setDescription(c.description());
            cv.setCoverImage(c.coverImage());
            cv.setLevel(c.level());
            cv.setInstructor(c.instructor());
            cv.setLessonCount(c.lessonCount());
            cv.setDurationTotalSec(c.durationTotalSec());
            cv.setProgressPercent(c.progressPercent());
            cv.setCompleted(c.completed());
            return cv;
        }).toList());
        return vo;
    }

    private PortalTrainingLessonRespVO toLessonVO(PortalTrainingLessonDO lesson,
                                                  PortalTrainingLessonProgressDO mine) {
        PortalTrainingLessonRespVO vo = new PortalTrainingLessonRespVO();
        vo.setId(lesson.getId());
        vo.setTitle(lesson.getTitle());
        vo.setKind(lesson.getKind());
        vo.setSortOrder(lesson.getSortOrder());
        vo.setDurationSec(lesson.getDurationSec());
        vo.setSizeBytes(lesson.getSizeBytes());
        vo.setHasPdf(lesson.getPdfKey() != null && !lesson.getPdfKey().isEmpty());
        vo.setPageCount(lesson.getPages() == null || lesson.getPages().isEmpty() ? null : lesson.getPages().size());
        vo.setUrl("link".equals(lesson.getKind()) ? lesson.getUrl() : null);
        if (mine != null) {
            PortalTrainingLessonRespVO.MyProgress my = new PortalTrainingLessonRespVO.MyProgress();
            my.setStatus(mine.getStatus());
            my.setPosition(mine.getPosition());
            my.setPercent(mine.getPercent());
            my.setDurationSec(mine.getDurationSec());
            my.setCompletedAt(mine.getCompletedAt());
            vo.setMy(my);
        }
        return vo;
    }

}
