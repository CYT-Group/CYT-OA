package com.cyt.center.module.portal.controller.admin.products;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductPageReqVO;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductRespVO;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductSaveReqVO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.products.PortalProductService;
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

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 新品动态管理接口层（迁自 Portal 的 routers/products.py 管理端一段）。
 *
 * 写端点统一 @SaCheckPermission(can_manage_products)：Portal 侧 create/update 用 can_manage_products、
 * delete 用 can_delete，迁移契约把这组无审批流的写操作收口到管理能力一把闸。
 */
@Tag(name = "管理后台 - 新品动态")
@RestController
@RequestMapping("/portal/products")
@Validated
@RequiredArgsConstructor
public class PortalProductController {

    private final PortalProductService productService;

    @GetMapping("/page")
    @Operation(summary = "分页列新品（含全部状态）")
    public Result<PageResult<PortalProductRespVO>> page(@Valid PortalProductPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(productService.page(pageReqVO), PortalProductRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "单条新品详情（编辑表单用，不看状态）")
    @Parameter(name = "id", description = "新品编号", required = true)
    public Result<PortalProductRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(productService.get(id), PortalProductRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "新建新品")
    @SaCheckPermission(PortalCapability.MANAGE_PRODUCTS)
    public Result<PortalProductRespVO> create(@Valid @RequestBody PortalProductSaveReqVO createReqVO) {
        return success(BeanUtils.toBean(productService.create(createReqVO), PortalProductRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "改新品（全量覆盖）")
    @SaCheckPermission(PortalCapability.MANAGE_PRODUCTS)
    public Result<PortalProductRespVO> update(@Valid @RequestBody PortalProductSaveReqVO updateReqVO) {
        return success(BeanUtils.toBean(productService.update(updateReqVO.getId(), updateReqVO),
                PortalProductRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删新品（硬删，删除动作进操作日志）")
    @Parameter(name = "id", description = "新品编号", required = true)
    @SaCheckPermission(PortalCapability.MANAGE_PRODUCTS)
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        productService.delete(getLoginUserId(), id);
        return success(true);
    }

}
