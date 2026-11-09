package com.cyt.center.module.portal.service.category;

import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryCreateReqVO;
import com.cyt.center.module.portal.dal.dataobject.category.PortalCategoryDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通用分类用例：事务边界、权限策略、审计都在这层（口径 docs/Portal管理面迁移契约.md §一「通用分类」）。
 *
 * 层职责与 Portal 的分层一一对应——SQL 只在 dal/mysql/category，形状与 attrs / code 的校验规则
 * 只在 domain/PortalCategoryScope，本层负责把它们串起来并管好事务。
 */
public interface PortalCategoryService {

    /** 管理行：一行分类 + 它的引用数与在役子行数（管理面板一次要看完） */
    record AdminRow(PortalCategoryDO category, Long refCount, Long childrenCount) {
    }

    /** 归档结果：detached = 因为父大类归档而变成「未归类」的小类数，前端确认框要回显 */
    record ArchiveResult(Long id, LocalDateTime archivedAt, Integer detached) {
    }

    /** 某 scope 的在役分类，平铺返回（前端自己组树）；登录即可读，不判能力 */
    List<PortalCategoryDO> listPublic(String scope);

    /** 管理视图：含归档行 + 计数；需要该 scope 的管理能力 */
    List<AdminRow> listAdmin(Long userId, String scope);

    PortalCategoryDO create(Long userId, PortalCategoryCreateReqVO createReqVO);

    /**
     * PATCH 语义改分类：patch 只含请求里显式出现的键（name / code / parentId / sortOrder / attrs）。
     *
     * 用 Map 而不是带类型的 VO，是为了留住「传了 null = 清空」与「没传 = 不动」的区别——
     * 普通字段两条都表现成 null，而小类摘父天天用得上前者。
     */
    PortalCategoryDO update(Long userId, Long id, Map<String, Object> patch);

    ArchiveResult archive(Long userId, Long id);

    PortalCategoryDO restore(Long userId, Long id);

    /** 硬删，只留给零引用且无子行的手滑行；归档才是正道 */
    void delete(Long userId, Long id);

    /** 同层拖拽排序：按数组顺序写 sortOrder = 1..n，回包为按新顺序排好的行 */
    List<PortalCategoryDO> reorder(Long userId, String scope, Long parentId, List<Long> orderedIds);

}
