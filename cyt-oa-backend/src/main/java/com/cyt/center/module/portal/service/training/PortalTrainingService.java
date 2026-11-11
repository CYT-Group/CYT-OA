package com.cyt.center.module.portal.service.training;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingLessonSaveReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingPageReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingSaveReqVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingProgressReqVO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonProgressDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingProgressDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 培训中心用例：课程/课时/进度、发布下架门禁、员工端可见性（口径 docs/Portal管理面迁移契约.md §一「培训中心」）。
 *
 * 逐条对齐 Portal 的 application/training.py；层划分照那边——SQL 只在 dal/mysql/training，本层只串规则与事务。
 * 媒体预签名（MinIO）、NAS 目录导入、培训统计看板在本批次不做，留 TODO。
 */
public interface PortalTrainingService {

    // ══════════════ 管理端 ══════════════

    /** 管理端分页列课程（含 draft）+ 课时数 + 五键 kindCounts（0 也给） */
    PageResult<CourseRow> page(PortalTrainingPageReqVO pageReqVO);

    /** 管理端取单门课程（不看状态，编辑表单用），不存在 404 */
    CourseRow getCourse(Long id);

    /** 手建课程：进草稿，分类必填（catalog 按它分组），作者记当前用户 */
    PortalTrainingDO create(Long userId, PortalTrainingSaveReqVO createReqVO);

    /** PATCH 语义改课程：只动非 null 字段（title/category 由 @NotBlank 保证非空） */
    PortalTrainingDO update(Long userId, Long id, PortalTrainingSaveReqVO updateReqVO);

    /** 删除课程（级联课时与课时进度），审计 training_delete */
    void delete(Long userId, Long id);

    /** 发布：至少一个课时（无课时 409），已发布再发 = 幂等空操作；审计 training_publish */
    PortalTrainingDO publish(Long userId, Long id);

    /** 下架回草稿（幂等），审计 training_unpublish */
    PortalTrainingDO unpublish(Long userId, Long id);

    /** 批量发布：整体 200，每门独立判定，发不了的进 skipped；审计 training_course_batch_publish */
    BatchResult batchPublish(Long userId, List<Long> ids);

    /** 批量下架：整体 200，镜像批量发布；审计 training_course_batch_unpublish */
    BatchResult batchUnpublish(Long userId, List<Long> ids);

    /** 手建课时：kind 与字段匹配校验不过 422；sort_order 未传追加到末尾 */
    PortalTrainingLessonDO createLesson(Long courseId, PortalTrainingLessonSaveReqVO reqVO);

    /** 改课时标题 / 排序（PATCH 语义，只动显式传入的字段） */
    PortalTrainingLessonDO updateLesson(Long lessonId, PortalTrainingLessonSaveReqVO reqVO);

    /** 删除课时（该课时的员工进度随 FK 级联） */
    void deleteLesson(Long lessonId);

    // ══════════════ 员工端（登录即可） ══════════════

    /** 大类入口页：每个大类的小类数/课程数/小类名预览；只统计本人可见课程，空大类不返回 */
    List<GroupCard> groups(Long userId);

    /** 课程中心：published 且 dept_scope 命中的课程按小类分组 + 本人进度；group 传大类 code / __unassigned__ */
    List<CatalogGroup> catalog(Long userId, String group);

    /** 课程详情：课程 + 课时列表 + 本人各课时进度 */
    CourseDetail courseDetail(Long userId, Long courseId);

    /** 老 GET /training 列表：status 默认 published，非 can_view_any_status 静默降级；published 再做 dept_scope 过滤 */
    PageResult<PortalTrainingDO> openPage(Long userId, String status, PageParam pageParam);

    /** 课时进度心跳：percent 只增不减、跨阈值记 completed、同步老课程级表 */
    ProgressResp reportProgress(Long userId, Long lessonId, PortalTrainingProgressReqVO reqVO);

    /** 当前用户的课程级学习进度列表（老表） */
    List<PortalTrainingProgressDO> myProgress(Long userId);

    /** 标记开始学习（幂等，无行才建 started） */
    void start(Long userId, Long courseId);

    /** 标记完成学习（无行建 completed，有行改状态+完成时间） */
    void complete(Long userId, Long courseId);

    // ══════════════ 媒体 / 导入 / 统计 ══════════════

    /** 课时媒体预签名：可见性与详情同一套规则（管理者豁免）；键缺失 422、NAS 未配置 503 */
    LessonMediaResp lessonMedia(Long userId, Long lessonId);

    /** 管理端直传预签名：课程不存在 404、扩展名/kind 不匹配 422、NAS 未配置 503 */
    UploadUrlResp uploadUrl(Long courseId, String filename, String kind);

    /** NAS 目录导入：整桶 listTree → parse_tree → 幂等 upsert，返回统计摘要；NAS 连不上 503 */
    ImportSummary importScan(Long userId);

    /** 全站培训统计：totalCourses（已发布课程数）、usersStarted、completions */
    StatsResp trainingStats();

    // ══════════════ 聚合返回类型 ══════════════

    /** 管理端课程行：课程 + 课时数 + 五键 kindCounts（顺序 video/pdf/article/cards/link，恒在） */
    record CourseRow(PortalTrainingDO course, int lessonCount, Map<String, Integer> kindCounts) {
    }

    /** 批量操作里被跳过的一门课 */
    record Skipped(Long id, String title, String reason) {
    }

    /** 批量结果：成功数（调用方按 published / unpublished 命名）+ 跳过明细 */
    record BatchResult(int count, List<Skipped> skipped) {
    }

    /** 大类卡片（key=null 是「未分类」伪大类） */
    record GroupCard(String key, String name, String icon, String description,
                     int categoryCount, int courseCount, List<String> categories) {
    }

    /** catalog 里的一门课（按小类分组前的行） */
    record CatalogCourse(Long id, String title, String description, String coverImage, String level,
                         String instructor, int lessonCount, long durationTotalSec, int progressPercent,
                         boolean completed) {
    }

    /** catalog 的一个小类分组 */
    record CatalogGroup(String category, List<CatalogCourse> courses) {
    }

    /** 课程详情：课程 + 课时 + 本人进度（lesson_id → 进度行） */
    record CourseDetail(PortalTrainingDO course, List<PortalTrainingLessonDO> lessons,
                        Map<Long, PortalTrainingLessonProgressDO> mine) {
    }

    /** 进度心跳回包 */
    record ProgressResp(Long lessonId, String status, Integer position, Integer percent, Integer durationSec,
                        LocalDateTime completedAt, boolean courseCompleted) {
    }

    /** 课时媒体预签名回包：video/pdf/html 型 article 给 url（article 带 pdf_key 时另给 pdfUrl）；cards/图片序列给 pages；link 给 url */
    record LessonMediaResp(String url, String pdfUrl, List<String> pages, Integer expiresIn) {
    }

    /** 管理端直传预签名回包 */
    record UploadUrlResp(String uploadUrl, String objectKey, Integer expiresIn) {
    }

    /** NAS 目录导入统计摘要 */
    record ImportSummary(List<String> categories, int categoriesCreated, int coursesCreated, int coursesSeen,
                         int lessonsCreated, int lessonsUpdated, List<String> skipped) {
    }

    /** 培训整体统计 */
    record StatsResp(long totalCourses, long usersStarted, long completions) {
    }

}
