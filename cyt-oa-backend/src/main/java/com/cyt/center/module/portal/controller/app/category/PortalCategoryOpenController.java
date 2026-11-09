package com.cyt.center.module.portal.controller.app.category;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.category.vo.PortalCategoryRespVO;
import com.cyt.center.module.portal.service.category.PortalCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;

/**
 * 分类树读侧接口：某 scope 的在役分类，平铺返回，组树由前端按 parentId 拼。
 *
 * 归档行不在这里（只在 /admin-api 的管理列表出现）；读侧不需要能力位——
 * 分类是给各模块当筛选项用的公共数据，谁能看哪条动态另有可见性规则。
 */
@Tag(name = "门户 - 通用分类（读）")
@RestController
@RequestMapping("/api/portal/categories")
@Validated
@RequiredArgsConstructor
public class PortalCategoryOpenController {

    private final PortalCategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "某 scope 的在役分类（平铺，前端自己组树）")
    @Parameter(name = "scope", description = "分类域", required = true, example = "material.spu")
    public Result<List<PortalCategoryRespVO>> list(@RequestParam("scope") String scope) {
        return success(convertList(categoryService.listPublic(scope),
                row -> BeanUtils.toBean(row, PortalCategoryRespVO.class)));
    }

}
