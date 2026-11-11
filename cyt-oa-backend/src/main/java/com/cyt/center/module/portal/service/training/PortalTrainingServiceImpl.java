package com.cyt.center.module.portal.service.training;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingLessonSaveReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingPageReqVO;
import com.cyt.center.module.portal.controller.admin.training.vo.PortalTrainingSaveReqVO;
import com.cyt.center.module.portal.controller.app.training.vo.PortalTrainingProgressReqVO;
import com.cyt.center.module.portal.dal.dataobject.category.PortalCategoryDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingLessonProgressDO;
import com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingProgressDO;
import com.cyt.center.module.portal.dal.mysql.category.PortalCategoryMapper;
import com.cyt.center.module.portal.dal.mysql.training.PortalTrainingLessonMapper;
import com.cyt.center.module.portal.dal.mysql.training.PortalTrainingLessonProgressMapper;
import com.cyt.center.module.portal.dal.mysql.training.PortalTrainingMapper;
import com.cyt.center.module.portal.dal.mysql.training.PortalTrainingProgressMapper;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.domain.PortalCategoryScope;
import com.cyt.center.module.portal.domain.training.PortalTrainingTreeParser;
import com.cyt.center.module.portal.domain.training.PortalTrainingTreeParser.ParsedCourse;
import com.cyt.center.module.portal.domain.training.PortalTrainingTreeParser.ParsedLesson;
import com.cyt.center.module.portal.domain.training.PortalTrainingTreeParser.ParsedTree;
import com.cyt.center.module.portal.domain.training.PortalTrainingTreeParser.TreeEntry;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalMediaStorage;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.permission.PermissionApi;
import com.cyt.center.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingDO.STATUS_DRAFT;
import static com.cyt.center.module.portal.dal.dataobject.training.PortalTrainingDO.STATUS_PUBLISHED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_CATEGORY_REQUIRED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_IMPORT_CONFLICT;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_LEVEL_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_LESSON_FIELDS_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_LESSON_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_MEDIA_KEY_MISSING;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_PROGRESS_PERCENT_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_PUBLISH_NO_LESSONS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.TRAINING_STATUS_INVALID;

/**
 * 培训中心用例实现（逐条对齐 Portal 的 application/training.py + application/training_import.py）。
 *
 * 媒体预签名（PortalMediaStorage）、NAS 目录导入（PortalTrainingTreeParser）、培训统计看板
 * 已在本批次接上，对应 Portal 的 lesson_media / import_scan / training_stats。
 */
@Slf4j
@Service
public class PortalTrainingServiceImpl implements PortalTrainingService {

    /** 培训分类在通用分类表里的 scope（注册表 PortalCategoryScope） */
    private static final String TRAINING_SCOPE = "training.course";

    private static final String UNASSIGNED_GROUP_NAME = "未分类";
    private static final String UNASSIGNED_GROUP_PARAM = "__unassigned__";

    /** kind_counts 的固定键序（管理端形式徽标按这个顺序画，五个键恒在、0 也给） */
    private static final List<String> KIND_ORDER = List.of("video", "pdf", "article", "cards", "link");

    private static final Set<String> KINDS = Set.of("video", "pdf", "article", "cards", "link");

    private static final String AUDIT_TABLE = "portal_training";

    private static final int BATCH_MAX = 200;

    private static final String PUBLISH_SKIP_NO_LESSONS = "no_lessons";
    private static final String PUBLISH_SKIP_ALREADY = "already_published";
    private static final String PUBLISH_SKIP_NOT_FOUND = "not_found";
    private static final String UNPUBLISH_SKIP_ALREADY_DRAFT = "already_draft";

    /** 员工端媒体预签名默认 1 小时（Portal 的 MEDIA_EXPIRES） */
    private static final int MEDIA_EXPIRES = 3600;
    /** 管理端直传预签名默认 15 分钟（Portal 的 UPLOAD_EXPIRES） */
    private static final int UPLOAD_EXPIRES = 900;
    /** 图片目录判型并发度（Portal 的 _IMAGE_PROBE_CONCURRENCY） */
    private static final int IMAGE_PROBE_CONCURRENCY = 12;
    /** 审计 after_value 里 skipped 只留前若干条（Portal 的 _AUDIT_SKIPPED_LIMIT） */
    private static final int AUDIT_SKIPPED_LIMIT = 20;

    @Resource
    private PortalTrainingMapper trainingMapper;
    @Resource
    private PortalTrainingLessonMapper lessonMapper;
    @Resource
    private PortalTrainingLessonProgressMapper lessonProgressMapper;
    @Resource
    private PortalTrainingProgressMapper trainingProgressMapper;
    @Resource
    private PortalCategoryMapper categoryMapper;
    @Resource
    private PortalAccess access;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PortalAudit audit;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PortalMediaStorage mediaStorage;

    // ══════════════ 管理端 ══════════════

    @Override
    public PageResult<CourseRow> page(PortalTrainingPageReqVO pageReqVO) {
        String status = blankToNull(pageReqVO.getStatus());
        if (status != null && !STATUS_DRAFT.equals(status) && !STATUS_PUBLISHED.equals(status)) {
            throw exception(TRAINING_STATUS_INVALID);
        }
        PageResult<PortalTrainingDO> courses = trainingMapper.selectPage(pageReqVO, status,
                blankToNull(pageReqVO.getCategory()), blankToNull(pageReqVO.getQ()));
        List<Long> ids = courses.getList().stream().map(PortalTrainingDO::getId).toList();
        Map<Long, Long> counts = lessonMapper.countByCourseIds(ids);
        Map<Long, Map<String, Integer>> kinds = lessonMapper.selectKindCountsByCourseIds(ids);
        List<CourseRow> rows = courses.getList().stream()
                .map(c -> new CourseRow(c, counts.getOrDefault(c.getId(), 0L).intValue(),
                        fixedKindCounts(kinds.get(c.getId()))))
                .toList();
        return new PageResult<>(rows, courses.getTotal());
    }

    @Override
    public CourseRow getCourse(Long id) {
        PortalTrainingDO course = id == null ? null : trainingMapper.selectById(id);
        if (course == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        Map<Long, Long> counts = lessonMapper.countByCourseIds(List.of(id));
        Map<Long, Map<String, Integer>> kinds = lessonMapper.selectKindCountsByCourseIds(List.of(id));
        return new CourseRow(course, counts.getOrDefault(id, 0L).intValue(), fixedKindCounts(kinds.get(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTrainingDO create(Long userId, PortalTrainingSaveReqVO createReqVO) {
        PortalOperator operator = operators.resolve(userId);
        String category = blankToNull(createReqVO.getCategory());
        if (category == null) {
            throw exception(TRAINING_CATEGORY_REQUIRED);
        }
        Long categoryId = ensureLeaf(category, operator.username());
        PortalTrainingDO row = new PortalTrainingDO();
        row.setTitle(createReqVO.getTitle().trim());
        row.setCategory(category);
        row.setCategoryId(categoryId);
        row.setDescription(blankToNull(createReqVO.getDescription()));
        String level = blankToNull(createReqVO.getLevel());
        row.setLevel(validateLevel(level == null ? "elective" : level));
        row.setDeptScope(normalizeDeptScope(createReqVO.getDeptScope()));
        row.setCoverImage(blankToNull(createReqVO.getCoverImage()));
        row.setInstructor(blankToNull(createReqVO.getInstructor()));
        row.setSortOrder(createReqVO.getSortOrder() == null ? 0 : createReqVO.getSortOrder());
        row.setStatus(STATUS_DRAFT);
        row.setAuthorUsername(operator.username());
        trainingMapper.insert(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTrainingDO update(Long userId, Long id, PortalTrainingSaveReqVO updateReqVO) {
        PortalTrainingDO row = id == null ? null : trainingMapper.selectById(id);
        if (row == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        PortalOperator operator = operators.resolve(userId);
        // title / category 恒非空（@NotBlank + 服务层兜底），其余字段 null 就不动
        row.setTitle(updateReqVO.getTitle().trim());
        String category = blankToNull(updateReqVO.getCategory());
        if (category == null) {
            throw exception(TRAINING_CATEGORY_REQUIRED);
        }
        row.setCategory(category);
        row.setCategoryId(ensureLeaf(category, operator.username()));
        String level = blankToNull(updateReqVO.getLevel());
        if (level != null) {
            row.setLevel(validateLevel(level));
        }
        if (updateReqVO.getDeptScope() != null) {
            row.setDeptScope(normalizeDeptScope(updateReqVO.getDeptScope()));
        }
        if (updateReqVO.getDescription() != null) {
            row.setDescription(blankToNull(updateReqVO.getDescription()));
        }
        if (updateReqVO.getCoverImage() != null) {
            row.setCoverImage(blankToNull(updateReqVO.getCoverImage()));
        }
        if (updateReqVO.getInstructor() != null) {
            row.setInstructor(blankToNull(updateReqVO.getInstructor()));
        }
        if (updateReqVO.getSortOrder() != null) {
            row.setSortOrder(updateReqVO.getSortOrder());
        }
        trainingMapper.updateById(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        PortalTrainingDO row = id == null ? null : trainingMapper.selectById(id);
        if (row == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        trainingMapper.deleteById(id);
        // 课程级进度表无 FK 留存；审计 before_value = 标题（Portal 的 admin_delete_course 口径）
        audit.record(userId, "training_delete", AUDIT_TABLE, id, row.getTitle(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTrainingDO publish(Long userId, Long id) {
        PortalTrainingDO course = id == null ? null : trainingMapper.selectById(id);
        if (course == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        int lessonCount = lessonMapper.countByCourseIds(List.of(id)).getOrDefault(id, 0L).intValue();
        if (lessonCount <= 0) {
            // no_lessons 优先于 already_published：已发布却删光课时的课属数据异常，报「没课时」更醒目
            throw exception(TRAINING_PUBLISH_NO_LESSONS);
        }
        String before = course.getStatus();
        course.setStatus(STATUS_PUBLISHED);
        trainingMapper.updateById(course);
        audit.record(userId, "training_publish", AUDIT_TABLE, id, before, STATUS_PUBLISHED, null);
        return course;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTrainingDO unpublish(Long userId, Long id) {
        PortalTrainingDO course = id == null ? null : trainingMapper.selectById(id);
        if (course == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        String before = course.getStatus();
        course.setStatus(STATUS_DRAFT);
        trainingMapper.updateById(course);
        audit.record(userId, "training_unpublish", AUDIT_TABLE, id, before, STATUS_DRAFT, null);
        return course;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchResult batchPublish(Long userId, List<Long> ids) {
        List<Long> deduped = dedupe(ids);
        Map<Long, Long> lessonCounts = lessonMapper.countByCourseIds(deduped);
        return batchSwitch(userId, deduped, STATUS_PUBLISHED,
                c -> publishBlockReason(c.getStatus(), lessonCounts.getOrDefault(c.getId(), 0L).intValue()),
                "training_course_batch_publish", "发布",
                PUBLISH_SKIP_NO_LESSONS, PUBLISH_SKIP_ALREADY, PUBLISH_SKIP_NOT_FOUND);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchResult batchUnpublish(Long userId, List<Long> ids) {
        return batchSwitch(userId, dedupe(ids), STATUS_DRAFT, c -> unpublishBlockReason(c.getStatus()),
                "training_course_batch_unpublish", "下架",
                UNPUBLISH_SKIP_ALREADY_DRAFT, PUBLISH_SKIP_NOT_FOUND);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTrainingLessonDO createLesson(Long courseId, PortalTrainingLessonSaveReqVO reqVO) {
        PortalTrainingDO course = courseId == null ? null : trainingMapper.selectById(courseId);
        if (course == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        Map<String, Object> fields = validateLessonFields(reqVO.getKind(), reqVO.getObjectKey(),
                reqVO.getPdfKey(), reqVO.getPages(), reqVO.getUrl());
        String objectKey = (String) fields.get("object_key");
        String pdfKey = (String) fields.get("pdf_key");
        @SuppressWarnings("unchecked")
        List<String> pages = (List<String>) fields.get("pages");
        String url = (String) fields.get("url");
        validateKey(objectKey);
        validateKey(pdfKey);
        if (pages != null) {
            for (String page : pages) {
                validateKey(page);
            }
        }
        int sortOrder = reqVO.getSortOrder() != null ? reqVO.getSortOrder() : lessonMapper.maxSortOrder(courseId) + 1;
        PortalTrainingLessonDO lesson = new PortalTrainingLessonDO();
        lesson.setCourseId(courseId);
        lesson.setSortOrder(sortOrder);
        lesson.setTitle(cleanTitle(reqVO.getTitle()));
        lesson.setKind(reqVO.getKind());
        lesson.setObjectKey(objectKey);
        lesson.setPdfKey(pdfKey);
        lesson.setPages(pages);
        lesson.setUrl(url);
        lessonMapper.insert(lesson);
        return lesson;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalTrainingLessonDO updateLesson(Long lessonId, PortalTrainingLessonSaveReqVO reqVO) {
        PortalTrainingLessonDO lesson = lessonId == null ? null : lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw exception(TRAINING_LESSON_NOT_EXISTS);
        }
        if (reqVO.getTitle() != null) {
            lesson.setTitle(cleanTitle(reqVO.getTitle()));
        }
        if (reqVO.getSortOrder() != null) {
            lesson.setSortOrder(reqVO.getSortOrder());
        }
        lessonMapper.updateById(lesson);
        return lesson;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLesson(Long lessonId) {
        PortalTrainingLessonDO lesson = lessonId == null ? null : lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw exception(TRAINING_LESSON_NOT_EXISTS);
        }
        lessonMapper.deleteById(lessonId);
    }

    // ══════════════ 员工端 ══════════════

    @Override
    public List<GroupCard> groups(Long userId) {
        operators.resolve(userId);  // 登录即可：未登录统一 UNAUTHORIZED（与 news 读侧同口径）
        Tree tree = loadTree();
        Map<Long, Map<String, int[]>> cells = new LinkedHashMap<>();
        for (PortalTrainingDO c : visiblePublishedCourses(userId)) {
            Slot slot = courseSlot(tree, c);
            Map<String, int[]> bucket = cells.computeIfAbsent(slot.groupId(), k -> new LinkedHashMap<>());
            int[] entry = bucket.get(slot.label());
            if (entry == null) {
                entry = new int[]{slot.order(), 0};
                bucket.put(slot.label(), entry);
            }
            entry[1]++;
        }
        List<GroupCard> out = new ArrayList<>();
        for (PortalCategoryDO g : tree.groups()) {
            GroupCard card = block(g.getCode(), g.getName(), attr(g, "icon"), attr(g, "description"),
                    cells.get(g.getId()));
            if (card != null) {
                out.add(card);
            }
        }
        GroupCard orphan = block(null, UNASSIGNED_GROUP_NAME, null, null, cells.get(null));
        if (orphan != null) {
            out.add(orphan);
        }
        return out;
    }

    @Override
    public List<CatalogGroup> catalog(Long userId, String group) {
        Tree tree = loadTree();
        List<PortalTrainingDO> courses = visiblePublishedCourses(userId);
        Map<Long, Slot> slots = new LinkedHashMap<>();
        for (PortalTrainingDO c : courses) {
            slots.put(c.getId(), courseSlot(tree, c));
        }
        if (group != null && !group.isBlank()) {
            Long want;
            if (UNASSIGNED_GROUP_PARAM.equals(group)) {
                want = null;
            } else {
                PortalCategoryDO target = tree.groupsByCode().get(group);
                if (target == null) {
                    return List.of();
                }
                want = target.getId();
            }
            Long finalWant = want;
            courses = courses.stream().filter(c -> {
                Slot s = slots.get(c.getId());
                return s.groupId() == null ? finalWant == null : finalWant.equals(s.groupId());
            }).toList();
        }
        List<Long> ids = courses.stream().map(PortalTrainingDO::getId).toList();
        Map<Long, Long> lessonCounts = lessonMapper.countByCourseIds(ids);
        Map<Long, Long> durations = lessonMapper.selectDurationSumByCourseIds(ids);
        Map<Long, Long> completed = lessonProgressMapper.selectCompletedCountByCourseIds(usernameOf(userId), ids);
        Map<String, List<CatalogCourse>> grouped = new LinkedHashMap<>();
        Map<String, int[]> order = new LinkedHashMap<>();
        for (PortalTrainingDO c : courses) {
            Slot s = slots.get(c.getId());
            int count = lessonCounts.getOrDefault(c.getId(), 0L).intValue();
            long duration = durations.getOrDefault(c.getId(), 0L);
            int done = completed.getOrDefault(c.getId(), 0L).intValue();
            int[] summary = courseProgressSummary(count, done);
            CatalogCourse item = new CatalogCourse(c.getId(), c.getTitle(), c.getDescription(), c.getCoverImage(),
                    c.getLevel(), c.getInstructor(), count, duration, summary[0], summary[1] == 1);
            String label = s.label();
            order.putIfAbsent(label, new int[]{s.order(), 0});
            grouped.computeIfAbsent(label, k -> new ArrayList<>()).add(item);
        }
        List<String> labels = new ArrayList<>(grouped.keySet());
        labels.sort((a, b) -> {
            int c = Integer.compare(order.get(a)[0], order.get(b)[0]);
            return c != 0 ? c : a.compareTo(b);
        });
        List<CatalogGroup> out = new ArrayList<>();
        for (String label : labels) {
            out.add(new CatalogGroup(label, grouped.get(label)));
        }
        return out;
    }

    @Override
    public CourseDetail courseDetail(Long userId, Long courseId) {
        PortalTrainingDO course = requireVisibleCourse(userId, courseId);
        List<PortalTrainingLessonDO> lessons = lessonMapper.selectByCourseId(course.getId());
        List<Long> lessonIds = lessons.stream().map(PortalTrainingLessonDO::getId).toList();
        Map<Long, PortalTrainingLessonProgressDO> mine = lessonProgressMapper
                .selectByUsernameAndLessonIds(usernameOf(userId), lessonIds);
        return new CourseDetail(course, lessons, mine);
    }

    @Override
    public PageResult<PortalTrainingDO> openPage(Long userId, String status, PageParam pageParam) {
        operators.resolve(userId);  // 登录即可：未登录统一 UNAUTHORIZED
        String requested = status == null || status.isBlank() ? STATUS_PUBLISHED : status;
        String resolved = STATUS_PUBLISHED.equals(requested) || !access.canViewAnyStatus(userId)
                ? STATUS_PUBLISHED : requested;
        List<PortalTrainingDO> all = STATUS_PUBLISHED.equals(resolved)
                ? trainingMapper.selectPublished()
                : trainingMapper.selectList(PortalTrainingDO::getStatus, resolved);
        if (STATUS_PUBLISHED.equals(resolved)) {
            boolean manager = isManager(userId);
            List<String> deptNames = manager ? null : userDeptNames(userId);
            all = all.stream().filter(c -> manager || deptScopeHit(c.getDeptScope(), deptNames)).toList();
        }
        all = all.stream()
                .sorted(Comparator.comparing((PortalTrainingDO c) -> c.getSortOrder() == null ? 0 : c.getSortOrder())
                        .thenComparing(PortalTrainingDO::getId))
                .toList();
        int total = all.size();
        int pageNo = pageParam.getPageNo() == null ? 1 : pageParam.getPageNo();
        int pageSize = pageParam.getPageSize() == null ? 10 : pageParam.getPageSize();
        int from = (pageNo - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<PortalTrainingDO> page = from >= total ? List.of() : all.subList(from, to);
        return new PageResult<>(page, (long) total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProgressResp reportProgress(Long userId, Long lessonId, PortalTrainingProgressReqVO reqVO) {
        PortalOperator operator = operators.resolve(userId);
        PortalTrainingLessonDO lesson = lessonId == null ? null : lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw exception(TRAINING_LESSON_NOT_EXISTS);
        }
        requireVisibleCourse(userId, lesson.getCourseId());
        int percent = reqVO.getPercent();
        if (percent < 0 || percent > 100) {
            throw exception(TRAINING_PROGRESS_PERCENT_INVALID);
        }
        PortalTrainingLessonProgressDO row = upsertProgress(operator.username(), lesson, reqVO, percent);
        boolean courseCompleted = syncCourseProgress(operator.username(), lesson.getCourseId());
        return new ProgressResp(row.getLessonId(), row.getStatus(), row.getPosition(), row.getPercent(),
                row.getDurationSec(), row.getCompletedAt(), courseCompleted);
    }

    @Override
    public List<PortalTrainingProgressDO> myProgress(Long userId) {
        return trainingProgressMapper.selectListByUsername(usernameOf(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long userId, Long courseId) {
        PortalOperator operator = operators.resolve(userId);
        PortalTrainingDO course = courseId == null ? null : trainingMapper.selectById(courseId);
        if (course == null || !STATUS_PUBLISHED.equals(course.getStatus())) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        PortalTrainingProgressDO prog = trainingProgressMapper.selectByUsernameAndTrainingId(operator.username(), courseId);
        if (prog == null) {
            PortalTrainingProgressDO row = new PortalTrainingProgressDO();
            row.setUsername(operator.username());
            row.setTrainingId(courseId);
            row.setStatus(PortalTrainingProgressDO.STATUS_STARTED);
            trainingProgressMapper.insert(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long userId, Long courseId) {
        PortalOperator operator = operators.resolve(userId);
        PortalTrainingDO course = courseId == null ? null : trainingMapper.selectById(courseId);
        if (course == null || !STATUS_PUBLISHED.equals(course.getStatus())) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        PortalTrainingProgressDO prog = trainingProgressMapper.selectByUsernameAndTrainingId(operator.username(), courseId);
        if (prog == null) {
            PortalTrainingProgressDO row = new PortalTrainingProgressDO();
            row.setUsername(operator.username());
            row.setTrainingId(courseId);
            row.setStatus(PortalTrainingProgressDO.STATUS_COMPLETED);
            row.setCompletedAt(LocalDateTime.now());
            trainingProgressMapper.insert(row);
        } else {
            prog.setStatus(PortalTrainingProgressDO.STATUS_COMPLETED);
            prog.setCompletedAt(LocalDateTime.now());
            trainingProgressMapper.updateById(prog);
        }
    }

    // ══════════════ 媒体 / 导入 / 统计 ══════════════

    @Override
    public LessonMediaResp lessonMedia(Long userId, Long lessonId) {
        PortalTrainingLessonDO lesson = lessonId == null ? null : lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw exception(TRAINING_LESSON_NOT_EXISTS);
        }
        requireVisibleCourse(userId, lesson.getCourseId());
        if (PortalTrainingLessonDO.KIND_LINK.equals(lesson.getKind())) {
            if (lesson.getUrl() == null || lesson.getUrl().isBlank()) {
                throw exception(TRAINING_MEDIA_KEY_MISSING, "外链课时缺少 url");
            }
            return new LessonMediaResp(lesson.getUrl(), null, null, null);
        }
        if (PortalTrainingLessonDO.KIND_CARDS.equals(lesson.getKind())
                || (PortalTrainingLessonDO.KIND_ARTICLE.equals(lesson.getKind())
                && (lesson.getObjectKey() == null || lesson.getObjectKey().isBlank()))) {
            if (lesson.getPages() == null || lesson.getPages().isEmpty()) {
                throw exception(TRAINING_MEDIA_KEY_MISSING,
                        PortalTrainingLessonDO.KIND_CARDS.equals(lesson.getKind())
                                ? "图卡课时缺少图片" : "图文课时缺少 html 或图片");
            }
            List<String> pages = lesson.getPages().stream()
                    .map(k -> mediaStorage.presignGet(k, MEDIA_EXPIRES))
                    .toList();
            return new LessonMediaResp(null, null, pages, MEDIA_EXPIRES);
        }
        if (lesson.getObjectKey() == null || lesson.getObjectKey().isBlank()) {
            throw exception(TRAINING_MEDIA_KEY_MISSING, "课时缺少媒体对象");
        }
        String url = mediaStorage.presignGet(lesson.getObjectKey(), MEDIA_EXPIRES);
        String pdfUrl = null;
        if (PortalTrainingLessonDO.KIND_ARTICLE.equals(lesson.getKind())
                && lesson.getPdfKey() != null && !lesson.getPdfKey().isBlank()) {
            pdfUrl = mediaStorage.presignGet(lesson.getPdfKey(), MEDIA_EXPIRES);
        }
        return new LessonMediaResp(url, pdfUrl, null, MEDIA_EXPIRES);
    }

    @Override
    public UploadUrlResp uploadUrl(Long courseId, String filename, String kind) {
        PortalTrainingDO course = courseId == null ? null : trainingMapper.selectById(courseId);
        if (course == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        String key = mediaStorage.uploadKey(courseId, filename, kind);
        return new UploadUrlResp(mediaStorage.presignPut(key, UPLOAD_EXPIRES), key, UPLOAD_EXPIRES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportSummary importScan(Long userId) {
        PortalOperator operator = operators.resolve(userId);
        List<TreeEntry> entries = mediaStorage.listTree().stream()
                .map(e -> new TreeEntry(e.key(), e.size()))
                .toList();
        ParsedTree tree = PortalTrainingTreeParser.parseTree(entries);

        // 图片目录判型：先取库里这些目录上次的判型做增量跳过，再并发探剩下的
        List<String> imagePrefixes = tree.getImageDirs().stream()
                .map(ParsedLesson::getSourcePrefix).toList();
        Map<String, PortalTrainingLessonDO> existingLessons =
                indexBy(PortalTrainingLessonDO::getSourcePrefix, lessonMapper.selectBySourcePrefixes(imagePrefixes));
        resolveImageDirs(tree.getImageDirs(), existingLessons);

        // 新顶层目录 = 新小类
        Map<String, Long> categoryIds = new LinkedHashMap<>();
        int categoriesCreated = 0;
        for (String raw : tree.categories()) {
            String name = raw.trim();
            if (categoryIds.containsKey(name)) {
                continue;
            }
            LeafResult leaf = ensureLeafResult(name, operator.username());
            categoryIds.put(name, leaf.id());
            if (leaf.created()) {
                categoriesCreated++;
            }
        }

        try {
            return applyImportWrites(tree, categoryIds, userId, categoriesCreated);
        } catch (DuplicateKeyException ex) {
            throw exception(TRAINING_IMPORT_CONFLICT);
        }
    }

    @Override
    public StatsResp trainingStats() {
        long totalCourses = trainingMapper.countPublished();
        long usersStarted = trainingProgressMapper.countDistinctUsername();
        long completions = trainingProgressMapper.countCompleted();
        return new StatsResp(totalCourses, usersStarted, completions);
    }

    // ══════════════ 内部：导入编排 ══════════════

    /** 并发探测待判型的图片目录，把 kind 原地回填到 ParsedLesson 上（增量跳过：内容未变沿用旧判型）。 */
    private void resolveImageDirs(List<ParsedLesson> imageDirs, Map<String, PortalTrainingLessonDO> existingLessons) {
        List<ParsedLesson> todo = new ArrayList<>();
        for (ParsedLesson pl : imageDirs) {
            if (!reuseKind(existingLessons.get(pl.getSourcePrefix()), pl)) {
                todo.add(pl);
            }
        }
        if (todo.isEmpty()) {
            return;
        }
        int concurrency = Math.min(IMAGE_PROBE_CONCURRENCY, todo.size());
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "training-probe-" + seq.getAndIncrement());
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(concurrency, threadFactory);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (ParsedLesson pl : todo) {
                futures.add(pool.submit(() -> probeImageDir(pl)));
            }
            for (int i = 0; i < todo.size(); i++) {
                String kind;
                try {
                    kind = futures.get(i).get();
                } catch (Exception e) {
                    kind = "article";
                }
                todo.get(i).setKind(kind);
            }
        } finally {
            pool.shutdown();
        }
    }

    /** 库里那行的 kind 能否直接沿用（省掉这个目录的探测）；能则原地写回 pl.kind。 */
    private static boolean reuseKind(PortalTrainingLessonDO row, ParsedLesson pl) {
        if (row == null) {
            return false;
        }
        String kind = row.getKind();
        if (!"cards".equals(kind) && !"article".equals(kind)) {
            return false;
        }
        if (!java.util.Objects.equals(row.getPages(), pl.getPages())
                || !java.util.Objects.equals(row.getSizeBytes(), pl.getSizeBytes())) {
            return false;
        }
        pl.setKind(kind);
        return true;
    }

    /** 探一个图片目录的判型（线程里跑，阻塞 IO）：抽样读文件头 → 解析尺寸 → 判 cards|article。 */
    private String probeImageDir(ParsedLesson pl) {
        try {
            List<String> sample = PortalTrainingTreeParser.sampleImagePages(pl.getPages());
            Iterator<int[]> sizes = new Iterator<>() {
                private int idx = 0;

                @Override
                public boolean hasNext() {
                    return idx < sample.size();
                }

                @Override
                public int[] next() {
                    byte[] head = mediaStorage.readHead(sample.get(idx), PortalMediaStorage.DEFAULT_HEAD_BYTES);
                    idx++;
                    return PortalTrainingTreeParser.parseImageSize(head);
                }
            };
            return PortalTrainingTreeParser.decideImageDirKind(sizes);
        } catch (Exception e) {
            log.warn("图片目录判型失败，按图文处理: {}",
                    pl.getPages() == null || pl.getPages().isEmpty() ? "" : pl.getPages().get(0), e);
            return "article";
        }
    }

    /** 幂等 upsert 课程与课时（写阶段，一个事务）；返回统计摘要。 */
    private ImportSummary applyImportWrites(ParsedTree tree, Map<String, Long> categoryIds,
                                            Long userId, int categoriesCreated) {
        // 课程：按 source_prefix 找已存在行——已存在只补缺失的 category/category_id，不覆盖 HR 改过的字段
        List<String> coursePrefixes = tree.getCourses().stream().map(ParsedCourse::getSourcePrefix).toList();
        Map<String, PortalTrainingDO> existingCourses = indexBy(PortalTrainingDO::getSourcePrefix, trainingMapper.selectBySourcePrefixes(coursePrefixes));
        int coursesCreated = 0;
        for (ParsedCourse pc : tree.getCourses()) {
            PortalTrainingDO row = existingCourses.get(pc.getSourcePrefix());
            Long categoryId = categoryIds.get(pc.getCategory() == null ? "" : pc.getCategory().trim());
            if (row == null) {
                PortalTrainingDO fresh = new PortalTrainingDO();
                fresh.setTitle(pc.getTitle());
                fresh.setCategory(pc.getCategory());
                fresh.setCategoryId(categoryId);
                fresh.setSourcePrefix(pc.getSourcePrefix());
                fresh.setStatus(STATUS_DRAFT);
                fresh.setLevel("elective");
                fresh.setSortOrder(pc.getSortOrder());
                trainingMapper.insert(fresh);
                existingCourses.put(pc.getSourcePrefix(), fresh);
                coursesCreated++;
            } else {
                String text = row.getCategory() == null ? "" : row.getCategory();
                boolean blankText = text.isEmpty();
                boolean textMatches = !blankText && text.trim().equals(pc.getCategory());
                if (row.getCategoryId() == null && (blankText || textMatches)) {
                    row.setCategory(pc.getCategory());
                    row.setCategoryId(categoryId);
                    trainingMapper.updateById(row);
                }
            }
        }

        // 课时：HR 可编辑的 title/sort_order 不动；kind/object_key/pdf_key/pages/size_bytes 来自 bucket，不一致时刷新
        List<String> lessonPrefixes = new ArrayList<>();
        for (ParsedCourse pc : tree.getCourses()) {
            for (ParsedLesson pl : pc.getLessons()) {
                lessonPrefixes.add(pl.getSourcePrefix());
            }
        }
        Map<String, PortalTrainingLessonDO> existingLessons = indexBy(PortalTrainingLessonDO::getSourcePrefix, lessonMapper.selectBySourcePrefixes(lessonPrefixes));
        int lessonsCreated = 0;
        int lessonsUpdated = 0;
        for (ParsedCourse pc : tree.getCourses()) {
            PortalTrainingDO course = existingCourses.get(pc.getSourcePrefix());
            for (ParsedLesson pl : pc.getLessons()) {
                PortalTrainingLessonDO lessonRow = existingLessons.get(pl.getSourcePrefix());
                if (lessonRow == null) {
                    PortalTrainingLessonDO fresh = new PortalTrainingLessonDO();
                    fresh.setCourseId(course.getId());
                    fresh.setSortOrder(pl.getSortOrder());
                    fresh.setTitle(pl.getTitle());
                    fresh.setKind(pl.getKind());
                    fresh.setObjectKey(pl.getObjectKey());
                    fresh.setPdfKey(pl.getPdfKey());
                    fresh.setPages(pl.getPages());
                    fresh.setSizeBytes(pl.getSizeBytes());
                    fresh.setSourcePrefix(pl.getSourcePrefix());
                    lessonMapper.insert(fresh);
                    lessonsCreated++;
                    continue;
                }
                boolean changed = false;
                if (!java.util.Objects.equals(lessonRow.getKind(), pl.getKind())) {
                    lessonRow.setKind(pl.getKind());
                    changed = true;
                }
                if (!java.util.Objects.equals(lessonRow.getObjectKey(), pl.getObjectKey())) {
                    lessonRow.setObjectKey(pl.getObjectKey());
                    changed = true;
                }
                if (!java.util.Objects.equals(lessonRow.getPdfKey(), pl.getPdfKey())) {
                    lessonRow.setPdfKey(pl.getPdfKey());
                    changed = true;
                }
                if (!java.util.Objects.equals(lessonRow.getPages(), pl.getPages())) {
                    lessonRow.setPages(pl.getPages());
                    changed = true;
                }
                if (!java.util.Objects.equals(lessonRow.getSizeBytes(), pl.getSizeBytes())) {
                    lessonRow.setSizeBytes(pl.getSizeBytes());
                    changed = true;
                }
                if (changed) {
                    lessonMapper.updateById(lessonRow);
                    lessonsUpdated++;
                }
            }
        }

        ImportSummary summary = new ImportSummary(tree.categories(), categoriesCreated, coursesCreated,
                tree.getCourses().size(), lessonsCreated, lessonsUpdated, tree.getSkipped());
        auditImport(userId, summary);
        return summary;
    }

    private void auditImport(Long userId, ImportSummary summary) {
        try {
            List<String> head = summary.skipped().size() > AUDIT_SKIPPED_LIMIT
                    ? summary.skipped().subList(0, AUDIT_SKIPPED_LIMIT) : summary.skipped();
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("categories", summary.categories());
            view.put("categories_created", summary.categoriesCreated());
            view.put("courses_created", summary.coursesCreated());
            view.put("courses_seen", summary.coursesSeen());
            view.put("lessons_created", summary.lessonsCreated());
            view.put("lessons_updated", summary.lessonsUpdated());
            view.put("skipped", head);
            view.put("skipped_total", summary.skipped().size());
            audit.record(userId, "training_import_scan", AUDIT_TABLE, null, null, JsonUtils.toJsonString(view), null);
        } catch (Exception e) {
            log.warn("培训导入审计写入失败", e);
        }
    }

    private static <T> Map<String, T> indexBy(Function<T, String> keyOf, List<T> rows) {
        Map<String, T> out = new LinkedHashMap<>();
        for (T row : rows) {
            out.put(keyOf.apply(row), row);
        }
        return out;
    }

    // ══════════════ 内部：小类归属 ══════════════

    /**
     * 按名找 / 建小类行（portal_categories depth 2）。对齐 Portal 的 application/training.py ensure_category_rows
     * + application/categories.py ensure_leaves：不存在则建 depth 2、parent_id NULL（不猜归属）、sort_order 0；
     * 已存在的行一个字段都不动；只有归档行时返回归档行 id、不复活。
     */
    private Long ensureLeaf(String name, String createdBy) {
        return ensureLeafResult(name, createdBy).id();
    }

    /** ensureLeaf 的带「是否新建」版本：导入统计 categories_created 用 */
    private LeafResult ensureLeafResult(String name, String createdBy) {
        String cleaned = PortalCategoryScope.cleanName(TRAINING_SCOPE, name);
        PortalCategoryDO existing = categoryMapper.selectAnyByScopeAndName(TRAINING_SCOPE, cleaned);
        if (existing != null) {
            return new LeafResult(existing.getId(), false);
        }
        PortalCategoryDO row = new PortalCategoryDO();
        row.setScope(TRAINING_SCOPE);
        row.setName(cleaned);
        row.setCode(null);
        row.setParentId(null);
        row.setDepth(2);
        row.setSortOrder(0);
        row.setAttrs(Map.of());
        row.setCreatedBy(createdBy);
        categoryMapper.insert(row);
        return new LeafResult(row.getId(), true);
    }

    private record LeafResult(Long id, boolean created) {
    }

    // ══════════════ 内部：批量骨架 ══════════════

    private BatchResult batchSwitch(Long userId, List<Long> ids, String targetStatus,
                                    Function<PortalTrainingDO, String> reasonOf, String action, String verb,
                                    String... reasonCodes) {
        List<PortalTrainingDO> courses = trainingMapper.selectList(PortalTrainingDO::getId, ids);
        Map<Long, PortalTrainingDO> byId = new LinkedHashMap<>();
        courses.forEach(c -> byId.put(c.getId(), c));
        List<Long> changed = new ArrayList<>();
        List<Skipped> skipped = new ArrayList<>();
        for (Long id : ids) {
            PortalTrainingDO course = byId.get(id);
            if (course == null) {
                skipped.add(new Skipped(id, null, PUBLISH_SKIP_NOT_FOUND));
                continue;
            }
            String reason = reasonOf.apply(course);
            if (reason != null) {
                skipped.add(new Skipped(id, course.getTitle(), reason));
                continue;
            }
            course.setStatus(targetStatus);
            trainingMapper.updateById(course);
            changed.add(id);
        }
        String reasons = reasonSummary(skipped, reasonCodes);
        audit.record(userId, action, AUDIT_TABLE, truncateIds(ids, 100), null,
                truncateIds(changed, 400),
                verb + " " + changed.size() + " 门，跳过 " + skipped.size() + " 门（" + reasons + "）");
        return new BatchResult(changed.size(), skipped);
    }

    /** 发布门禁：no_lessons 优先于 already_published（Portal 的 domain/training.publish_block_reason） */
    private static String publishBlockReason(String status, int lessonCount) {
        if (lessonCount <= 0) {
            return PUBLISH_SKIP_NO_LESSONS;
        }
        if (STATUS_PUBLISHED.equals(status)) {
            return PUBLISH_SKIP_ALREADY;
        }
        return null;
    }

    private static String unpublishBlockReason(String status) {
        return STATUS_PUBLISHED.equals(status) ? null : UNPUBLISH_SKIP_ALREADY_DRAFT;
    }

    private static List<Long> dedupe(List<Long> ids) {
        if (ids == null) {
            throw invalidParamException("ids 不能为空");
        }
        if (ids.size() > BATCH_MAX) {
            throw invalidParamException("一次最多处理 {} 门课程", BATCH_MAX);
        }
        List<Long> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : ids) {
            if (id != null && seen.add(id)) {
                out.add(id);
            }
        }
        return out;
    }

    private static String reasonSummary(List<Skipped> skipped, String... reasonCodes) {
        List<String> parts = new ArrayList<>();
        for (String code : reasonCodes) {
            long count = skipped.stream().filter(s -> code.equals(s.reason())).count();
            parts.add(code + "=" + count);
        }
        return String.join("，", parts);
    }

    /** 把 id 清单压成不超过 maxLen 个字符的字面量，装不下的记成「…+N」（Portal 的 _truncate_ids） */
    private static String truncateIds(List<Long> ids, int maxLen) {
        List<String> kept = new ArrayList<>();
        for (Long id : ids) {
            kept.add(String.valueOf(id));
            if (String.join(",", kept).length() > maxLen) {
                kept.remove(kept.size() - 1);
                break;
            }
        }
        while (!kept.isEmpty() && String.join(",", kept).length() + ("…+" + (ids.size() - kept.size())).length() > maxLen) {
            kept.remove(kept.size() - 1);
        }
        String body = String.join(",", kept);
        return kept.size() == ids.size() ? body : body + "…+" + (ids.size() - kept.size());
    }

    // ══════════════ 内部：课时字段与进度规则 ══════════════

    /**
     * 手建课时：kind 决定哪些字段必填、哪些不许带；返回归一后的四字段 dict。
     * 逐条对齐 Portal 的 domain/training.validate_lesson_fields。
     */
    private static Map<String, Object> validateLessonFields(String kind, String objectKey, String pdfKey,
                                                            List<String> pages, String url) {
        if (kind == null || !KINDS.contains(kind)) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "kind 只能是 video / pdf / article / cards / link");
        }
        Map<String, Object> given = new LinkedHashMap<>();
        given.put("object_key", objectKey);
        given.put("pdf_key", pdfKey);
        given.put("pages", pages);
        given.put("url", url);
        Set<String> present = new HashSet<>();
        for (Map.Entry<String, Object> e : given.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            if (v instanceof String s && s.isEmpty()) {
                continue;
            }
            if (v instanceof List<?> l && l.isEmpty()) {
                continue;
            }
            present.add(e.getKey());
        }
        Map<String, Set<String>> required = Map.of(
                "video", Set.of("object_key"),
                "pdf", Set.of("object_key"),
                "article", Set.of(),
                "cards", Set.of("pages"),
                "link", Set.of("url"));
        for (String name : required.get(kind)) {
            if (!present.contains(name)) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID, kind + " 课时缺少字段 " + name);
            }
        }
        Map<String, Set<String>> allowed = Map.of(
                "video", Set.of("object_key"),
                "pdf", Set.of("object_key"),
                "article", Set.of("object_key", "pdf_key", "pages"),
                "cards", Set.of("pages"),
                "link", Set.of("url"));
        Set<String> extra = new HashSet<>(present);
        extra.removeAll(allowed.get(kind));
        if (!extra.isEmpty()) {
            List<String> sorted = new ArrayList<>(extra);
            Collections.sort(sorted);
            throw exception(TRAINING_LESSON_FIELDS_INVALID, kind + " 课时不接受字段 " + String.join(", ", sorted));
        }
        if ("article".equals(kind)) {
            boolean hasObjectKey = present.contains("object_key");
            boolean hasPages = present.contains("pages");
            if (hasObjectKey == hasPages) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID,
                        "article 课时须且只能提供 object_key（html）或 pages（图片序列）之一");
            }
            if (present.contains("pdf_key") && !hasObjectKey) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID,
                        "article 的 pdf_key 只能随 object_key（html 型）一起提供");
            }
        }
        if (present.contains("pages")) {
            if (pages == null || pages.isEmpty()) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID, kind + " 的 pages 必须是非空字符串数组");
            }
            for (String p : pages) {
                if (p == null || p.isBlank()) {
                    throw exception(TRAINING_LESSON_FIELDS_INVALID, kind + " 的 pages 必须是非空字符串数组");
                }
            }
        }
        if ("link".equals(kind)) {
            String lower = url == null ? "" : url.toLowerCase();
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                throw exception(TRAINING_LESSON_FIELDS_INVALID, "link 的 url 必须以 http(s):// 开头");
            }
        }
        return given;
    }

    /** 对象键校验（Portal 的 services/minio_storage.validate_key）：拒绝 ..、以 / 开头、超 500 字节 */
    private static void validateKey(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        if (key.contains("..") || key.startsWith("/")) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键不合法");
        }
        if (key.getBytes(StandardCharsets.UTF_8).length > 500) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "对象键不能超过 500 字节");
        }
    }

    private static int completeThreshold(String kind) {
        return "video".equals(kind) ? 90 : 100;
    }

    private static int mergePercent(int current, int incoming) {
        if (incoming < 0 || incoming > 100) {
            throw exception(TRAINING_PROGRESS_PERCENT_INVALID);
        }
        return Math.max(current, incoming);
    }

    private static void applyProgress(PortalTrainingLessonProgressDO row, String kind, int percent,
                                      Integer position, Integer durationSec, LocalDateTime now) {
        row.setPercent(mergePercent(row.getPercent() == null ? 0 : row.getPercent(), percent));
        if (position != null) {
            row.setPosition(Math.max(0, position));
        }
        if (durationSec != null && durationSec > 0) {
            row.setDurationSec(durationSec);
        }
        if (!PortalTrainingLessonProgressDO.STATUS_COMPLETED.equals(row.getStatus())
                && row.getPercent() >= completeThreshold(kind)) {
            row.setStatus(PortalTrainingLessonProgressDO.STATUS_COMPLETED);
            row.setCompletedAt(now);
        }
    }

    private PortalTrainingLessonProgressDO upsertProgress(String username, PortalTrainingLessonDO lesson,
                                                          PortalTrainingProgressReqVO reqVO, int percent) {
        LocalDateTime now = LocalDateTime.now();
        for (int attempt = 1; ; attempt++) {
            PortalTrainingLessonProgressDO row = lessonProgressMapper.selectByUsernameAndLessonId(username, lesson.getId());
            if (row == null) {
                PortalTrainingLessonProgressDO fresh = new PortalTrainingLessonProgressDO();
                fresh.setUsername(username);
                fresh.setLessonId(lesson.getId());
                fresh.setStatus(PortalTrainingLessonProgressDO.STATUS_STARTED);
                fresh.setPosition(0);
                fresh.setPercent(0);
                applyProgress(fresh, lesson.getKind(), percent, reqVO.getPosition(), reqVO.getDurationSec(), now);
                try {
                    lessonProgressMapper.insert(fresh);
                } catch (DuplicateKeyException ex) {
                    if (attempt >= 2) {
                        throw exception(GlobalErrorCodeConstants.LOCKED);
                    }
                    continue;  // 撞 UNIQUE（同一员工两端同时心跳），重试一次——第二次一定能查到已存在的行
                }
                row = fresh;
            } else {
                applyProgress(row, lesson.getKind(), percent, reqVO.getPosition(), reqVO.getDurationSec(), now);
                lessonProgressMapper.updateById(row);
            }
            // video 首次回报 duration_sec 回填课时行
            if (reqVO.getDurationSec() != null && reqVO.getDurationSec() > 0
                    && PortalTrainingLessonDO.KIND_VIDEO.equals(lesson.getKind())
                    && (lesson.getDurationSec() == null || lesson.getDurationSec() == 0)) {
                lesson.setDurationSec(reqVO.getDurationSec());
                lessonMapper.updateById(lesson);
            }
            return row;
        }
    }

    private boolean syncCourseProgress(String username, Long courseId) {
        long lessonCount = lessonMapper.countByCourseIds(List.of(courseId)).getOrDefault(courseId, 0L);
        long completed = lessonProgressMapper.selectCompletedCountByCourseIds(username, List.of(courseId))
                .getOrDefault(courseId, 0L);
        boolean allDone = courseAllDone(lessonCount, completed);
        PortalTrainingProgressDO row = trainingProgressMapper.selectByUsernameAndTrainingId(username, courseId);
        if (row == null) {
            row = new PortalTrainingProgressDO();
            row.setUsername(username);
            row.setTrainingId(courseId);
            row.setStatus(PortalTrainingProgressDO.STATUS_STARTED);
            trainingProgressMapper.insert(row);
        }
        if (allDone && !PortalTrainingProgressDO.STATUS_COMPLETED.equals(row.getStatus())) {
            row.setStatus(PortalTrainingProgressDO.STATUS_COMPLETED);
            row.setCompletedAt(LocalDateTime.now());
            trainingProgressMapper.updateById(row);
        }
        return PortalTrainingProgressDO.STATUS_COMPLETED.equals(row.getStatus());
    }

    private static boolean courseAllDone(long lessonCount, long completed) {
        if (lessonCount <= 0) {
            return false;
        }
        return Math.min(completed, lessonCount) == lessonCount;
    }

    /** 课程进度折算：round(completed/lesson_count*100)；零课时 (0,false) */
    private static int[] courseProgressSummary(int lessonCount, int completedCount) {
        if (lessonCount <= 0) {
            return new int[]{0, 0};
        }
        int done = Math.min(completedCount, lessonCount);
        int percent = (int) Math.round(done * 100.0 / lessonCount);
        return new int[]{percent, done == lessonCount ? 1 : 0};
    }

    // ══════════════ 内部：可见性与分类树 ══════════════

    private boolean isManager(Long userId) {
        return access.has(userId, PortalCapability.MANAGE_TRAINING);
    }

    private List<String> userDeptNames(Long userId) {
        DeptDataPermissionRespDTO perm = permissionApi.getDeptDataPermission(userId);
        Set<Long> deptIds = perm == null ? Set.of() : perm.getDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (DeptRespDTO dept : deptApi.getDeptList(deptIds)) {
            if (dept.getName() != null) {
                names.add(dept.getName());
            }
        }
        return names;
    }

    private static boolean deptScopeHit(List<String> scope, List<String> userDepts) {
        if (scope == null || scope.isEmpty()) {
            return true;
        }
        Set<String> set = new HashSet<>(userDepts == null ? List.of() : userDepts);
        for (String d : scope) {
            if (set.contains(d)) {
                return true;
            }
        }
        return false;
    }

    private PortalTrainingDO requireVisibleCourse(Long userId, Long courseId) {
        PortalTrainingDO course = courseId == null ? null : trainingMapper.selectById(courseId);
        if (course == null) {
            throw exception(TRAINING_NOT_EXISTS);
        }
        boolean manager = isManager(userId);
        if (!manager && (!STATUS_PUBLISHED.equals(course.getStatus())
                || !deptScopeHit(course.getDeptScope(), userDeptNames(userId)))) {
            // 不存在 / 未发布 / 部门不命中统一 404 语义（防探测）
            throw exception(TRAINING_NOT_EXISTS);
        }
        return course;
    }

    private List<PortalTrainingDO> visiblePublishedCourses(Long userId) {
        boolean manager = isManager(userId);
        List<String> deptNames = manager ? null : userDeptNames(userId);
        return trainingMapper.selectPublished().stream()
                .filter(c -> manager || deptScopeHit(c.getDeptScope(), deptNames))
                .toList();
    }

    private record Tree(List<PortalCategoryDO> groups, Map<Long, PortalCategoryDO> groupsById,
                        Map<String, PortalCategoryDO> groupsByCode, Map<Long, PortalCategoryDO> leaves) {
    }

    private record Slot(Long groupId, String label, int order) {
    }

    private Tree loadTree() {
        List<PortalCategoryDO> rows = categoryMapper.selectListByScope(TRAINING_SCOPE, true);
        List<PortalCategoryDO> groups = rows.stream()
                .filter(r -> Integer.valueOf(1).equals(r.getDepth()) && r.getArchivedAt() == null)
                .sorted(Comparator.comparing((PortalCategoryDO r) -> r.getSortOrder() == null ? 0 : r.getSortOrder())
                        .thenComparing(r -> r.getCode() == null ? "" : r.getCode()))
                .toList();
        Map<Long, PortalCategoryDO> groupsById = new LinkedHashMap<>();
        Map<String, PortalCategoryDO> groupsByCode = new LinkedHashMap<>();
        for (PortalCategoryDO g : groups) {
            groupsById.put(g.getId(), g);
            if (g.getCode() != null) {
                groupsByCode.put(g.getCode(), g);
            }
        }
        Map<Long, PortalCategoryDO> leaves = new LinkedHashMap<>();
        for (PortalCategoryDO r : rows) {
            if (Integer.valueOf(2).equals(r.getDepth())) {
                leaves.put(r.getId(), r);
            }
        }
        return new Tree(groups, groupsById, groupsByCode, leaves);
    }

    private static Slot courseSlot(Tree tree, PortalTrainingDO course) {
        PortalCategoryDO leaf = course.getCategoryId() == null ? null : tree.leaves().get(course.getCategoryId());
        if (leaf == null) {
            return new Slot(null, categoryLabel(course.getCategory()), 0);
        }
        Long groupId = (leaf.getArchivedAt() == null && leaf.getParentId() != null
                && tree.groupsById().containsKey(leaf.getParentId())) ? leaf.getParentId() : null;
        return new Slot(groupId, leaf.getName(), leaf.getSortOrder() == null ? 0 : leaf.getSortOrder());
    }

    private static String categoryLabel(String category) {
        String c = category == null ? "" : category.trim();
        return c.isEmpty() ? UNASSIGNED_GROUP_NAME : c;
    }

    private static String attr(PortalCategoryDO g, String key) {
        if (g.getAttrs() == null) {
            return null;
        }
        Object v = g.getAttrs().get(key);
        return v instanceof String s ? s : null;
    }

    private static GroupCard block(String key, String name, String icon, String description,
                                   Map<String, int[]> bucket) {
        if (bucket == null || bucket.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>(bucket.keySet());
        names.sort((a, b) -> {
            int c = Integer.compare(bucket.get(a)[0], bucket.get(b)[0]);
            return c != 0 ? c : a.compareTo(b);
        });
        int total = 0;
        for (String n : names) {
            total += bucket.get(n)[1];
        }
        if (total == 0) {
            return null;
        }
        return new GroupCard(key, name, icon, description, names.size(), total, names);
    }

    // ══════════════ 内部：值域归一 ══════════════

    private static String validateLevel(String level) {
        if (!"required".equals(level) && !"elective".equals(level)) {
            throw exception(TRAINING_LEVEL_INVALID);
        }
        return level;
    }

    private static List<String> normalizeDeptScope(List<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        List<String> seen = new ArrayList<>();
        for (String d : value) {
            String trimmed = d == null ? "" : d.trim();
            if (!trimmed.isEmpty() && !seen.contains(trimmed)) {
                seen.add(trimmed);
            }
        }
        return seen.isEmpty() ? null : seen;
    }

    private static String cleanTitle(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) {
            throw exception(TRAINING_LESSON_FIELDS_INVALID, "标题不能为空");
        }
        return t;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String usernameOf(Long userId) {
        return operators.resolve(userId).username();
    }

    /** 五键 kindCounts 恒在（0 也给），顺序 video/pdf/article/cards/link */
    private static Map<String, Integer> fixedKindCounts(Map<String, Integer> raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String kind : KIND_ORDER) {
            out.put(kind, raw == null ? 0 : raw.getOrDefault(kind, 0));
        }
        return out;
    }

}
