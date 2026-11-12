package com.cyt.center.module.portal.service.category;

import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryCreateReqVO;
import com.cyt.center.module.portal.dal.dataobject.category.PortalCategoryDO;
import com.cyt.center.module.portal.dal.postgresql.category.PortalCategoryMapper;
import com.cyt.center.module.portal.dal.postgresql.training.PortalTrainingMapper;
import com.cyt.center.module.portal.domain.PortalCategoryScope;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_ALREADY_ARCHIVED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_ARCHIVED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_CODE_DUPLICATE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_DUPLICATE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_HAS_CHILDREN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_HAS_REFS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_NAME_DUPLICATE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_NOT_ARCHIVED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_PARENT_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_REORDER_CROSS_DEPTH;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_REORDER_CROSS_LAYER;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_REORDER_DUPLICATE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_REORDER_EMPTY;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_REORDER_MISSING;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_RESTORE_NAME_DUPLICATE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_STILL_REFERENCED;

/**
 * 通用分类用例实现（逐条对齐 Portal 的 application/categories.py）。
 *
 * 事务在这层：写方法一律 @Transactional；撞唯一索引 / 外键后抛业务异常，由 Spring 回滚，
 * 等价于那边显式的 rollback + raise Conflict。审计走 {@link PortalAudit}，排在业务 commit 之后，
 * 业务成功不会被审计抖动打成失败。
 */
@Slf4j
@Service
@Validated
public class PortalCategoryServiceImpl implements PortalCategoryService {

    private static final String AUDIT_TABLE = "portal_categories";
    /** PATCH 请求里允许出现的键；其余键按 Portal 的 extra=ignore 口径静默丢弃 */
    private static final Set<String> PATCH_KEYS = Set.of("name", "code", "parentId", "sortOrder", "attrs");

    @Resource
    private PortalCategoryMapper categoryMapper;
    @Resource
    private PortalTrainingMapper trainingMapper;
    @Resource
    private PortalAccess access;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PortalAudit audit;

    @Override
    public List<PortalCategoryDO> listPublic(String scope) {
        PortalCategoryScope.get(scope);
        return categoryMapper.selectListByScope(scope, false);
    }

    @Override
    public List<AdminRow> listAdmin(Long userId, String scope) {
        access.requireScopeManager(userId, scope);
        List<PortalCategoryDO> rows = categoryMapper.selectListByScope(scope, true);
        Map<Long, Long> refs = refCountsOfScope(scope);
        Map<Long, Long> kids = categoryMapper.selectChildrenCountMap(scope);
        List<AdminRow> result = new ArrayList<>(rows.size());
        for (PortalCategoryDO row : rows) {
            result.add(new AdminRow(row, refs.getOrDefault(row.getId(), 0L), kids.getOrDefault(row.getId(), 0L)));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalCategoryDO create(Long userId, PortalCategoryCreateReqVO createReqVO) {
        String scope = createReqVO.getScope() == null ? "" : createReqVO.getScope().trim();
        access.requireScopeManager(userId, scope);
        String name = PortalCategoryScope.cleanName(scope, createReqVO.getName());
        PortalCategoryDO parent = parentOrThrow(createReqVO.getParentId());
        int depth = PortalCategoryScope.resolveDepth(scope, parent);
        String code = PortalCategoryScope.cleanCode(scope, depth, createReqVO.getCode(), true, null);
        Map<String, Object> attrs = PortalCategoryScope.cleanAttrs(scope, depth, createReqVO.getAttrs());
        if (categoryMapper.selectActiveByScopeAndName(scope, name) != null) {
            throw exception(CATEGORY_NAME_DUPLICATE);
        }
        if (code != null && categoryMapper.selectByScopeAndCode(scope, code) != null) {
            throw exception(CATEGORY_CODE_DUPLICATE);
        }
        Long parentId = parent == null ? null : parent.getId();
        // 不传 sort_order = 排在这一层末尾，不打乱已有顺序
        Integer sortOrder = createReqVO.getSortOrder() != null
                ? createReqVO.getSortOrder()
                : categoryMapper.selectMaxSortOrder(scope, parentId, depth) + 1;
        PortalCategoryDO row = new PortalCategoryDO();
        row.setScope(scope);
        row.setName(name);
        row.setCode(code);
        row.setParentId(parentId);
        row.setDepth(depth);
        row.setSortOrder(sortOrder);
        row.setAttrs(attrs);
        row.setCreatedBy(operators.resolve(userId).username());
        try {
            categoryMapper.insert(row);
        } catch (DuplicateKeyException ex) {
            // 并发建同名 / 同 code：唯一索引兜底，转成与预检同款的话术
            throw exception(CATEGORY_DUPLICATE);
        }
        audit.record(userId, "category_create", AUDIT_TABLE, row.getId(), null, snapshot(row), null);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalCategoryDO update(Long userId, Long id, Map<String, Object> body) {
        PortalCategoryDO row = getForUpdateOrThrow(id);
        String scope = row.getScope();
        access.requireScopeManager(userId, scope);
        ensureActive(row);
        Map<String, Object> patch = retainPatchKeys(body);
        String before = snapshot(row);
        boolean renamed = false;

        if (patch.containsKey("parentId")) {
            PortalCategoryDO parent = parentOrThrow(longOf(patch, "parentId"));
            row.setParentId(PortalCategoryScope.validateReparent(scope, row, parent));
        }
        if (patch.containsKey("name")) {
            String name = PortalCategoryScope.cleanName(scope, stringOf(patch, "name"));
            if (!name.equals(row.getName())) {
                PortalCategoryDO dup = categoryMapper.selectActiveByScopeAndName(scope, name);
                if (dup != null && !dup.getId().equals(row.getId())) {
                    throw exception(CATEGORY_NAME_DUPLICATE);
                }
                row.setName(name);
                renamed = true;
            }
        }
        if (patch.containsKey("code")) {
            String newCode = blankToNull(stringOf(patch, "code"));
            if (!Objects.equals(newCode, row.getCode())) {
                String code = PortalCategoryScope.cleanCode(scope, row.getDepth(), newCode, false, row.getCode());
                if (code != null) {
                    PortalCategoryDO dup = categoryMapper.selectByScopeAndCode(scope, code);
                    if (dup != null && !dup.getId().equals(row.getId())) {
                        throw exception(CATEGORY_CODE_DUPLICATE);
                    }
                }
                row.setCode(code);
            }
        }
        if (patch.containsKey("sortOrder")) {
            Integer sortOrder = intOf(patch, "sortOrder");
            row.setSortOrder(sortOrder == null ? 0 : sortOrder);
        }
        if (patch.containsKey("attrs")) {
            row.setAttrs(PortalCategoryScope.cleanAttrs(scope, row.getDepth(), mapOf(patch, "attrs")));
        }

        if (renamed) {
            cascadeRename(row);
        }
        try {
            categoryMapper.updateById(row);
        } catch (DuplicateKeyException ex) {
            throw exception(CATEGORY_DUPLICATE);
        }
        audit.record(userId, "category_update", AUDIT_TABLE, row.getId(), before, snapshot(row), null);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveResult archive(Long userId, Long id) {
        PortalCategoryDO row = getForUpdateOrThrow(id);
        access.requireScopeManager(userId, row.getScope());
        ensureNotArchivedYet(row);
        String before = snapshot(row);
        int detached = 0;
        if (PortalCategoryScope.refTarget(row.getScope(), row.getDepth()) == null) {
            // 培训大类：没有业务表挂它，归档只影响子行归属
            detached = categoryMapper.detachChildren(row.getId());
        } else {
            ensureNoRefs(row.getScope(), row.getDepth(), refCount(row));
        }
        PortalOperator operator = operators.resolve(userId);
        LocalDateTime now = LocalDateTime.now();
        row.setArchivedAt(now);
        row.setArchivedBy(operator.username());
        categoryMapper.updateById(row);
        audit.record(userId, "category_archive", AUDIT_TABLE, row.getId(), before,
                "archived_at=" + now + " detached=" + detached, null);
        return new ArchiveResult(row.getId(), now, detached);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalCategoryDO restore(Long userId, Long id) {
        PortalCategoryDO row = getForUpdateOrThrow(id);
        access.requireScopeManager(userId, row.getScope());
        ensureArchived(row);
        if (categoryMapper.selectActiveByScopeAndName(row.getScope(), row.getName()) != null) {
            throw exception(CATEGORY_RESTORE_NAME_DUPLICATE);
        }
        String before = snapshot(row);
        row.setArchivedAt(null);
        row.setArchivedBy(null);
        try {
            categoryMapper.updateById(row);
        } catch (DuplicateKeyException ex) {
            throw exception(CATEGORY_RESTORE_NAME_DUPLICATE);
        }
        audit.record(userId, "category_restore", AUDIT_TABLE, row.getId(), before, snapshot(row), null);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        PortalCategoryDO row = getForUpdateOrThrow(id);
        access.requireScopeManager(userId, row.getScope());
        if (categoryMapper.selectChildrenCount(row.getId(), false) > 0) {
            throw exception(CATEGORY_HAS_CHILDREN);
        }
        ensureNoRefs(row.getScope(), row.getDepth(), refCount(row));
        String before = snapshot(row);
        try {
            categoryMapper.deleteById(row.getId());
        } catch (DataIntegrityViolationException ex) {
            // 预检与删除之间被并发引用：FK RESTRICT 拒删（撞唯一索引是 DuplicateKeyException，两回事）
            throw exception(CATEGORY_STILL_REFERENCED);
        }
        audit.record(userId, "category_delete", AUDIT_TABLE, id, before, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PortalCategoryDO> reorder(Long userId, String scope, Long parentId, List<Long> orderedIds) {
        access.requireScopeManager(userId, scope);
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw exception(CATEGORY_REORDER_EMPTY);
        }
        if (new LinkedHashSet<>(orderedIds).size() != orderedIds.size()) {
            throw exception(CATEGORY_REORDER_DUPLICATE);
        }
        PortalCategoryDO parent = parentOrThrow(parentId);
        // flat 带 parent / parent 不合格在这一步就报错
        PortalCategoryScope.resolveDepth(scope, parent);
        Map<Long, PortalCategoryDO> rows = new LinkedHashMap<>();
        categoryMapper.selectListByIds(orderedIds).forEach(row -> rows.put(row.getId(), row));
        for (Long orderedId : orderedIds) {
            if (!rows.containsKey(orderedId)) {
                throw exception(CATEGORY_REORDER_MISSING, orderedId);
            }
        }
        for (PortalCategoryDO row : rows.values()) {
            if (!Objects.equals(row.getScope(), scope) || !Objects.equals(row.getParentId(), parentId)) {
                throw exception(CATEGORY_REORDER_CROSS_LAYER);
            }
        }
        if (rows.values().stream().map(PortalCategoryDO::getDepth).distinct().count() > 1) {
            throw exception(CATEGORY_REORDER_CROSS_DEPTH);
        }
        String before = JsonUtils.toJsonString(rows.values().stream()
                .sorted(Comparator.comparing((PortalCategoryDO row) -> row.getSortOrder())
                        .thenComparing(PortalCategoryDO::getName))
                .map(PortalCategoryDO::getId).toList());
        List<PortalCategoryDO> ordered = new ArrayList<>(orderedIds.size());
        int position = 1;
        for (Long orderedId : orderedIds) {
            PortalCategoryDO row = rows.get(orderedId);
            row.setSortOrder(position++);
            categoryMapper.updateById(row);
            ordered.add(row);
        }
        // 整层操作没有单一业务编号，target 用 scope/父 表达；biz_id 留空、原串进 extra
        audit.record(userId, "category_reorder", AUDIT_TABLE,
                scope + "/" + (parentId == null ? "root" : parentId), before, JsonUtils.toJsonString(orderedIds), null);
        return ordered;
    }

    // ══════════════ 内部 ══════════════

    private PortalCategoryDO getForUpdateOrThrow(Long id) {
        PortalCategoryDO row = id == null ? null : categoryMapper.selectByIdForUpdate(id);
        if (row == null) {
            throw exception(CATEGORY_NOT_EXISTS);
        }
        return row;
    }

    /**
     * 父分类指向不存在的行按「入参不对」处理，不是 404：资源本身在，错的是这次传的参数
     * （Portal 的 _parent_or_422 同款）。不加行锁：父行只是被读来判形状，改的是本行。
     */
    private PortalCategoryDO parentOrThrow(Long parentId) {
        if (parentId == null) {
            return null;
        }
        PortalCategoryDO parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw exception(CATEGORY_PARENT_NOT_EXISTS);
        }
        return parent;
    }

    private static void ensureActive(PortalCategoryDO row) {
        if (row.getArchivedAt() != null) {
            throw exception(CATEGORY_ARCHIVED);
        }
    }

    private static void ensureNotArchivedYet(PortalCategoryDO row) {
        if (row.getArchivedAt() != null) {
            throw exception(CATEGORY_ALREADY_ARCHIVED);
        }
    }

    private static void ensureArchived(PortalCategoryDO row) {
        if (row.getArchivedAt() == null) {
            throw exception(CATEGORY_NOT_ARCHIVED);
        }
    }

    private static void ensureNoRefs(String scope, Integer depth, long refCount) {
        if (refCount <= 0) {
            return;
        }
        if (PortalCategoryScope.REF_TRAINING.equals(PortalCategoryScope.refTarget(scope, depth))) {
            throw exception(CATEGORY_HAS_REFS, "仍有 " + refCount + " 门课程引用该分类，先改它们的分类");
        }
        throw exception(CATEGORY_HAS_REFS, "仍有 " + refCount + " 个在役商品引用该分类，先改它们的分类");
    }

    /**
     * 引用计数：培训小类数课程含草稿（Portal 的 repositories/categories.count_courses_of 口径）。
     * 物资（REF_MATERIAL_PRODUCTS）业务表要到 B5 才进 OA，此刻仍按 0 处理。
     */
    private long refCount(PortalCategoryDO row) {
        if (PortalCategoryScope.REF_TRAINING.equals(PortalCategoryScope.refTarget(row.getScope(), row.getDepth()))) {
            return trainingMapper.countByCategoryId(row.getId());
        }
        return 0L;
    }

    /** 整个培训 scope 一次算出 {category_id: 引用数}（管理列表用；口径同 refCount） */
    private Map<Long, Long> refCountsOfScope(String scope) {
        PortalCategoryScope.Scope spec = PortalCategoryScope.get(scope);
        if (!PortalCategoryScope.REF_TRAINING.equals(PortalCategoryScope.refTarget(scope, spec.leafDepth()))) {
            return Map.of();
        }
        List<Long> leafIds = categoryMapper.selectListByScope(scope, true).stream()
                .filter(r -> Integer.valueOf(spec.leafDepth()).equals(r.getDepth()))
                .map(PortalCategoryDO::getId).toList();
        if (leafIds.isEmpty()) {
            return Map.of();
        }
        return trainingMapper.countByCategoryIds(leafIds);
    }

    /** 改培训小类名 → 批量刷课程表的过渡期文本镜像列（S3a 停写文本列后删）；物资分支 B5 再接 */
    private void cascadeRename(PortalCategoryDO row) {
        if (PortalCategoryScope.REF_TRAINING.equals(PortalCategoryScope.refTarget(row.getScope(), row.getDepth()))) {
            trainingMapper.updateCategoryMirror(row.getId(), row.getName());
        }
    }

    /** 只保留 PATCH 允许的键，其余丢弃——「传了 null」与「没传」的差别全在这个键集合上 */
    private static Map<String, Object> retainPatchKeys(Map<String, Object> body) {
        Map<String, Object> patch = new LinkedHashMap<>();
        if (body == null) {
            return patch;
        }
        body.forEach((key, value) -> {
            if (PATCH_KEYS.contains(key)) {
                patch.put(key, value);
            }
        });
        return patch;
    }

    /** 行的纯数据快照（审计 before/after 用），键名沿用 Portal 的 snake_case，与 B1 搬来的历史行对齐 */
    private String snapshot(PortalCategoryDO row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("scope", row.getScope());
        snapshot.put("name", row.getName());
        snapshot.put("code", row.getCode());
        snapshot.put("parent_id", row.getParentId());
        snapshot.put("depth", row.getDepth());
        snapshot.put("sort_order", row.getSortOrder());
        snapshot.put("attrs", row.getAttrs() == null ? Map.of() : row.getAttrs());
        snapshot.put("archived_at", row.getArchivedAt() == null ? null : row.getArchivedAt().toString());
        return JsonUtils.toJsonString(snapshot);
    }

    private static String stringOf(Map<String, Object> patch, String key) {
        Object value = patch.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw invalidParamException("{} 必须是字符串", key);
        }
        return text;
    }

    private static Long longOf(Map<String, Object> patch, String key) {
        Object value = patch.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw invalidParamException("{} 必须是整数", key);
    }

    private static Integer intOf(Map<String, Object> patch, String key) {
        Object value = patch.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw invalidParamException("{} 必须是整数", key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Map<String, Object> patch, String key) {
        Object value = patch.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw invalidParamException("{} 必须是对象", key);
        }
        return (Map<String, Object>) value;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

}
