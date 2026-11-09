package com.cyt.center.module.portal.controller.admin.category;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryAdminRespVO;
import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryCreateReqVO;
import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryReorderReqVO;
import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryRespVO;
import com.cyt.center.module.portal.dal.dataobject.category.PortalCategoryDO;
import com.cyt.center.module.portal.service.category.PortalCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 通用分类接口层：只剩接线（参数、鉴权依赖、序列化）。
 *
 * 权限不在这里整端点闸——能力取决于 body / query 里的 scope，只能由用例按 scope 判，
 * 这与 Portal 的口径一致；端点级的 /admin-api 登录闸由 SaTokenConfigure 统一给。
 *
 * 路径刻意沿用 Portal 的资源名（categories / news / wiki / analytics），
 * B9 让 Portal 改读 OA 时就是逐字替换基地址，不用两头再对一遍命名。
 */
@Tag(name = "管理后台 - 门户通用分类")
@RestController
@RequestMapping("/portal/categories")
@Validated
@RequiredArgsConstructor
public class PortalCategoryController {

    private final PortalCategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "某 scope 的在役分类（平铺，前端自己组树）")
    public Result<List<PortalCategoryRespVO>> list(@RequestParam("scope") String scope) {
        return success(convertList(categoryService.listPublic(scope), PortalCategoryController::toRow));
    }

    @GetMapping("/admin-list")
    @Operation(summary = "管理视图：含归档行，每行带引用数与在役子行数")
    public Result<List<PortalCategoryAdminRespVO>> adminList(@RequestParam("scope") String scope) {
        return success(convertList(categoryService.listAdmin(getLoginUserId(), scope),
                PortalCategoryController::toAdminRow));
    }

    @PostMapping("/create")
    @Operation(summary = "新建分类")
    public Result<PortalCategoryRespVO> create(@Valid @RequestBody PortalCategoryCreateReqVO createReqVO) {
        return success(toRow(categoryService.create(getLoginUserId(), createReqVO)));
    }

    @PutMapping("/update")
    @Operation(summary = "改分类（PATCH 语义：只动请求体里显式出现的键 name / code / parentId / sortOrder / attrs）")
    @Parameter(name = "id", description = "分类编号", required = true)
    public Result<PortalCategoryRespVO> update(@RequestParam("id") Long id,
                                               @RequestBody Map<String, Object> patch) {
        return success(toRow(categoryService.update(getLoginUserId(), id, patch)));
    }

    @PostMapping("/reorder")
    @Operation(summary = "同层拖拽排序，回包为按新顺序排好的行")
    public Result<List<PortalCategoryRespVO>> reorder(@Valid @RequestBody PortalCategoryReorderReqVO reorderReqVO) {
        return success(convertList(categoryService.reorder(getLoginUserId(), reorderReqVO.getScope(),
                reorderReqVO.getParentId(), reorderReqVO.getOrderedIds()), PortalCategoryController::toRow));
    }

    @PostMapping("/archive")
    @Operation(summary = "归档；detached 为因此变成未归类的小类数，确认框回显用")
    @Parameter(name = "id", description = "分类编号", required = true)
    public Result<PortalCategoryService.ArchiveResult> archive(@RequestParam("id") Long id) {
        return success(categoryService.archive(getLoginUserId(), id));
    }

    @PostMapping("/restore")
    @Operation(summary = "恢复归档行为在役")
    @Parameter(name = "id", description = "分类编号", required = true)
    public Result<PortalCategoryRespVO> restore(@RequestParam("id") Long id) {
        return success(toRow(categoryService.restore(getLoginUserId(), id)));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "硬删（仅零引用且无子行；归档才是正道）")
    @Parameter(name = "id", description = "分类编号", required = true)
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        categoryService.delete(getLoginUserId(), id);
        return success(true);
    }

    private static PortalCategoryRespVO toRow(PortalCategoryDO row) {
        return BeanUtils.toBean(row, PortalCategoryRespVO.class);
    }

    private static PortalCategoryAdminRespVO toAdminRow(PortalCategoryService.AdminRow item) {
        PortalCategoryAdminRespVO row = BeanUtils.toBean(item.category(), PortalCategoryAdminRespVO.class);
        row.setRefCount(item.refCount());
        row.setChildrenCount(item.childrenCount());
        return row;
    }

}
