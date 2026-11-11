package com.cyt.center.module.portal.controller.app.products;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.portal.controller.admin.products.vo.PortalProductRespVO;
import com.cyt.center.module.portal.service.products.PortalProductService;
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

/**
 * 新品动态读侧接口（登录即可，无能力位要求）。
 *
 * 为什么单开一套而不是让门户直接调 /admin-api：管理令牌要求 device=admin，门户的登录人拿的是普通令牌，
 * 读侧必须走 /api/**（口径 docs/Portal管理面迁移契约.md §二）。可见性规则与 Portal 的 GET /products 一致：
 * 只回 published，未发布/不存在统一 404。
 */
@Tag(name = "门户 - 新品动态（读）")
@RestController
@RequestMapping("/api/portal/products")
@Validated
@RequiredArgsConstructor
public class PortalProductOpenController {

    private final PortalProductService productService;

    @GetMapping("/list")
    @Operation(summary = "已发布的新品列表（按创建时间倒序，limit 缺省 6）")
    @Parameter(name = "brand", description = "品牌精确匹配")
    @Parameter(name = "limit", description = "最多返回条数，缺省 6")
    public Result<List<PortalProductRespVO>> list(@RequestParam(value = "brand", required = false) String brand,
                                                  @RequestParam(value = "limit", required = false) Integer limit) {
        return success(BeanUtils.toBean(productService.listPublished(brand, limit), PortalProductRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "单条新品详情；不存在或未发布统一 404")
    @Parameter(name = "id", description = "新品编号", required = true)
    public Result<PortalProductRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(productService.getPublished(id), PortalProductRespVO.class));
    }

}
