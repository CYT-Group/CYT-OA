package com.cyt.center.module.oa.controller.admin.supply;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.enums.CommonStatusEnum;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.oa.controller.admin.supply.vo.item.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.apply.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.issue.*;
import com.cyt.center.module.oa.dal.dataobject.supply.*;
import com.cyt.center.module.oa.service.supply.*;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.*;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 办公用品")
@RestController
@RequestMapping("/oa/supply-item")
@Validated
public class OaSupplyItemController {

    @Resource
    private OaSupplyItemService supplyItemService;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建办公用品")
    @SaCheckPermission("oa:supply-item:create")
    public Result<Long> createSupplyItem(@Valid @RequestBody OaSupplyItemSaveReqVO reqVO) {
        return success(supplyItemService.createSupplyItem(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新办公用品")
    @SaCheckPermission("oa:supply-item:update")
    public Result<Boolean> updateSupplyItem(@Valid @RequestBody OaSupplyItemSaveReqVO reqVO) {
        supplyItemService.updateSupplyItem(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除办公用品")
    @Parameter(name = "id", description = "办公用品编号", required = true, example = "1024")
    @SaCheckPermission("oa:supply-item:delete")
    public Result<Boolean> deleteSupplyItem(@RequestParam("id") Long id) {
        supplyItemService.deleteSupplyItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得办公用品")
    @Parameter(name = "id", description = "办公用品编号", required = true, example = "1024")
    @SaCheckPermission("oa:supply-item:query")
    public Result<OaSupplyItemRespVO> getSupplyItem(@RequestParam("id") Long id) {
        OaSupplyItemDO item = supplyItemService.getSupplyItem(id);
        return success(BeanUtils.toBean(item, OaSupplyItemRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得办公用品分页")
    @SaCheckPermission("oa:supply-item:query")
    public Result<PageResult<OaSupplyItemRespVO>> getSupplyItemPage(@Valid OaSupplyItemPageReqVO reqVO) {
        // 1. 查询物品及所属部门
        PageResult<OaSupplyItemDO> page = supplyItemService.getSupplyItemPage(reqVO);
        Map<Long, DeptRespDTO> depts = deptApi.getDeptMap(convertSet(page.getList(), OaSupplyItemDO::getDeptId));
        // 2. 拼接部门名称
        PageResult<OaSupplyItemRespVO> result = BeanUtils.toBean(page, OaSupplyItemRespVO.class);
        result.getList().forEach(item -> {
            DeptRespDTO dept = depts.get(item.getDeptId());
            if (dept != null) {
                item.setDeptName(dept.getName());
            }
        });
        return success(result);
    }

    @GetMapping("/select-page")
    @Operation(summary = "获得可领用的办公用品分页")
    public Result<PageResult<OaSupplyItemRespVO>> getSupplyItemSelectPage(@Valid OaSupplyItemPageReqVO reqVO) {
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        PageResult<OaSupplyItemDO> page = supplyItemService.getSupplyItemPage(reqVO);
        return success(BeanUtils.toBean(page, OaSupplyItemRespVO.class));
    }

    @PutMapping("/stock-in")
    @Operation(summary = "办公用品入库")
    @SaCheckPermission("oa:supply-item:stock-in")
    public Result<Boolean> stockInSupplyItem(@Valid @RequestBody OaSupplyItemStockInReqVO reqVO) {
        supplyItemService.stockInSupplyItem(reqVO);
        return success(true);
    }

}
