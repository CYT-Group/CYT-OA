package com.cyt.center.module.portal.controller.admin.training;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingBatchReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingBatchResultVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingImportRespVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingLessonRespVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingLessonSaveReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingPageReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingRespVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingSaveReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingStatsRespVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingUploadUrlReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingUploadUrlRespVO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonDO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.training.PortalTrainingService;
import com.cyt.center.module.portal.service.training.PortalTrainingService.CourseRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 培训中心管理接口层（迁自 Portal 的 routers/training.py 管理端一段）。
 *
 * 全部端点 @SaCheckPermission(can_manage_training)：Portal 侧 require(can_manage_training) 的整端点闸
 * 一一对应。分类（大类 / 小类）的增删改不走这里——那些代理到通用分类模块（/portal/category），
 * 这里只管课程 / 课时 / 发布下架 / 批量 / 直传 / NAS 导入 / 统计。
 */
@Tag(name = "管理后台 - 培训中心")
@RestController
@RequestMapping("/portal/training")
@Validated
@RequiredArgsConstructor
public class PortalTrainingController {

    private final PortalTrainingService trainingService;

    @GetMapping("/page")
    @Operation(summary = "分页列课程（全部状态含草稿）+ 课时数 + 五键 kindCounts")
    public Result<PageResult<PortalTrainingRespVO>> page(@Valid PortalTrainingPageReqVO pageReqVO) {
        PageResult<CourseRow> rows = trainingService.page(pageReqVO);
        List<PortalTrainingRespVO> list = rows.getList().stream().map(this::toRow).toList();
        return success(new PageResult<>(list, rows.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "单门课程（编辑表单用，不看状态）")
    @Parameter(name = "id", description = "课程编号", required = true)
    public Result<PortalTrainingRespVO> get(@RequestParam("id") Long id) {
        return success(toRow(trainingService.getCourse(id)));
    }

    @PostMapping("/create")
    @Operation(summary = "手建课程（进草稿）")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingRespVO> create(@Valid @RequestBody PortalTrainingSaveReqVO createReqVO) {
        return success(BeanUtils.toBean(trainingService.create(getLoginUserId(), createReqVO),
                PortalTrainingRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "PATCH 语义改课程（只动非 null 字段；title/category 必填）")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingRespVO> update(@Valid @RequestBody PortalTrainingSaveReqVO updateReqVO) {
        return success(BeanUtils.toBean(trainingService.update(getLoginUserId(), updateReqVO.getId(), updateReqVO),
                PortalTrainingRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除课程（级联课时与课时进度）")
    @Parameter(name = "id", description = "课程编号", required = true)
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        trainingService.delete(getLoginUserId(), id);
        return success(true);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布（要求至少一个课时，否则 409）")
    @Parameter(name = "id", description = "课程编号", required = true)
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingRespVO> publish(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(trainingService.publish(getLoginUserId(), id),
                PortalTrainingRespVO.class));
    }

    @PostMapping("/unpublish")
    @Operation(summary = "下架回草稿")
    @Parameter(name = "id", description = "课程编号", required = true)
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingRespVO> unpublish(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(trainingService.unpublish(getLoginUserId(), id),
                PortalTrainingRespVO.class));
    }

    @PostMapping("/batch-publish")
    @Operation(summary = "批量发布：发不了的进 skipped 带原因")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingBatchResultVO> batchPublish(@Valid @RequestBody PortalTrainingBatchReqVO reqVO) {
        PortalTrainingService.BatchResult result = trainingService.batchPublish(getLoginUserId(), reqVO.getIds());
        PortalTrainingBatchResultVO vo = new PortalTrainingBatchResultVO();
        vo.setPublished(result.count());
        vo.setSkipped(toSkipped(result));
        return success(vo);
    }

    @PostMapping("/batch-unpublish")
    @Operation(summary = "批量下架：下不了的进 skipped 带原因")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingBatchResultVO> batchUnpublish(@Valid @RequestBody PortalTrainingBatchReqVO reqVO) {
        PortalTrainingService.BatchResult result = trainingService.batchUnpublish(getLoginUserId(), reqVO.getIds());
        PortalTrainingBatchResultVO vo = new PortalTrainingBatchResultVO();
        vo.setUnpublished(result.count());
        vo.setSkipped(toSkipped(result));
        return success(vo);
    }

    @PostMapping("/lesson/create")
    @Operation(summary = "手建课时（kind 与字段匹配校验不过 422）")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingLessonRespVO> createLesson(@Valid @RequestBody PortalTrainingLessonSaveReqVO reqVO) {
        return success(toLessonVO(trainingService.createLesson(reqVO.getCourseId(), reqVO)));
    }

    @PutMapping("/lesson/update")
    @Operation(summary = "改课时标题 / 排序（PATCH 语义）")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingLessonRespVO> updateLesson(@Valid @RequestBody PortalTrainingLessonSaveReqVO reqVO) {
        return success(toLessonVO(trainingService.updateLesson(reqVO.getId(), reqVO)));
    }

    @DeleteMapping("/lesson/delete")
    @Operation(summary = "删除课时（该课时的员工进度随 FK 级联）")
    @Parameter(name = "id", description = "课时编号", required = true)
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<Boolean> deleteLesson(@RequestParam("id") Long id) {
        trainingService.deleteLesson(id);
        return success(true);
    }

    @PostMapping("/upload-url")
    @Operation(summary = "管理端直传预签名：浏览器拿到 uploadUrl 直接 PUT 到 NAS")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingUploadUrlRespVO> uploadUrl(@Valid @RequestBody PortalTrainingUploadUrlReqVO reqVO) {
        PortalTrainingService.UploadUrlResp resp = trainingService.uploadUrl(reqVO.getCourseId(),
                reqVO.getFilename(), reqVO.getKind());
        PortalTrainingUploadUrlRespVO vo = new PortalTrainingUploadUrlRespVO();
        vo.setUploadUrl(resp.uploadUrl());
        vo.setObjectKey(resp.objectKey());
        vo.setExpiresIn(resp.expiresIn());
        return success(vo);
    }

    @PostMapping("/import/scan")
    @Operation(summary = "扫描 NAS 整桶并幂等导入为草稿课程/课时；后端连不上 NAS 时 503 含原因")
    @SaCheckPermission(PortalCapability.MANAGE_TRAINING)
    public Result<PortalTrainingImportRespVO> importScan() {
        PortalTrainingService.ImportSummary summary = trainingService.importScan(getLoginUserId());
        PortalTrainingImportRespVO vo = new PortalTrainingImportRespVO();
        vo.setCategories(summary.categories());
        vo.setCategoriesCreated(summary.categoriesCreated());
        vo.setCoursesCreated(summary.coursesCreated());
        vo.setCoursesSeen(summary.coursesSeen());
        vo.setLessonsCreated(summary.lessonsCreated());
        vo.setLessonsUpdated(summary.lessonsUpdated());
        vo.setSkipped(summary.skipped());
        return success(vo);
    }

    @GetMapping("/stats")
    @Operation(summary = "培训整体统计（含全员进度，设 can_view_dashboards 闸）")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<PortalTrainingStatsRespVO> stats() {
        PortalTrainingService.StatsResp stats = trainingService.trainingStats();
        PortalTrainingStatsRespVO vo = new PortalTrainingStatsRespVO();
        vo.setTotalCourses(stats.totalCourses());
        vo.setUsersStarted(stats.usersStarted());
        vo.setCompletions(stats.completions());
        return success(vo);
    }

    // ══════════════ 序列化 ══════════════

    private PortalTrainingRespVO toRow(CourseRow row) {
        PortalTrainingRespVO vo = BeanUtils.toBean(row.course(), PortalTrainingRespVO.class);
        vo.setLessonCount(row.lessonCount());
        vo.setKindCounts(row.kindCounts());
        return vo;
    }

    private static PortalTrainingLessonRespVO toLessonVO(PortalTrainingLessonDO lesson) {
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
        return vo;
    }

    private static List<PortalTrainingBatchResultVO.Skipped> toSkipped(PortalTrainingService.BatchResult result) {
        return result.skipped().stream().map(s -> {
            PortalTrainingBatchResultVO.Skipped vo = new PortalTrainingBatchResultVO.Skipped();
            vo.setId(s.id());
            vo.setTitle(s.title());
            vo.setReason(s.reason());
            return vo;
        }).toList();
    }

}
