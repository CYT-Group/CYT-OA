package com.cyt.center.module.oa.controller.admin.supply;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.collection.MapUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.oa.controller.admin.supply.vo.apply.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.issue.*;
import com.cyt.center.module.oa.dal.dataobject.supply.*;
import com.cyt.center.module.oa.service.supply.*;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
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

@Tag(name = "管理后台 - 用品领用申请")
@RestController
@RequestMapping("/oa/supply-apply")
@Validated
public class OaSupplyApplyController {

    @Resource
    private OaSupplyApplyService supplyApplyService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建用品领用申请")
    @SaCheckPermission("oa:supply-apply:create")
    public Result<Long> createSupplyApply(@Valid @RequestBody OaSupplyApplySaveReqVO reqVO) {
        return success(supplyApplyService.createSupplyApply(reqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新用品领用申请")
    @SaCheckPermission("oa:supply-apply:update")
    public Result<Boolean> updateSupplyApply(@Valid @RequestBody OaSupplyApplySaveReqVO reqVO) {
        supplyApplyService.updateSupplyApply(reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用品领用申请")
    @Parameter(name = "id", description = "申请编号", required = true, example = "1024")
    @SaCheckPermission("oa:supply-apply:delete")
    public Result<Boolean> deleteSupplyApply(@RequestParam("id") Long id) {
        supplyApplyService.deleteSupplyApply(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交用品领用申请")
    @Parameter(name = "id", description = "申请编号", required = true, example = "1024")
    @SaCheckPermission("oa:supply-apply:create")
    public Result<Boolean> submitSupplyApply(@RequestParam("id") Long id) {
        supplyApplyService.submitSupplyApply(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消用品领用申请")
    @Parameter(name = "id", description = "申请编号", required = true, example = "1024")
    @SaCheckPermission("oa:supply-apply:update")
    public Result<Boolean> cancelSupplyApply(@RequestParam("id") Long id) {
        supplyApplyService.cancelSupplyApply(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得用品领用申请")
    @Parameter(name = "id", description = "申请编号", required = true, example = "1024")
    @SaCheckPermission("oa:supply-apply:query")
    public Result<OaSupplyApplyRespVO> getSupplyApply(@RequestParam("id") Long id) {
        OaSupplyApplyDO apply = supplyApplyService.getSupplyApply(id);
        return success(buildSupplyApplyRespVO(apply));
    }

    @GetMapping("/page")
    @Operation(summary = "获得本人的用品领用申请分页")
    @SaCheckPermission("oa:supply-apply:query")
    public Result<PageResult<OaSupplyApplyRespVO>> getSupplyApplyPage(@Valid OaSupplyApplyPageReqVO reqVO) {
        PageResult<OaSupplyApplyDO> page = supplyApplyService.getSupplyApplyPage(reqVO, getLoginUserId());
        return success(new PageResult<>(buildSupplyApplyRespVOList(page.getList()), page.getTotal()));
    }

    // ==================== 拼接 VO ====================

    /**
     * 拼接用品领用申请详情
     *
     * @param apply 领用申请
     * @return 领用申请详情
     */
    private OaSupplyApplyRespVO buildSupplyApplyRespVO(OaSupplyApplyDO apply) {
        if (apply == null) {
            return null;
        }
        List<OaSupplyApplyItemDO> items = supplyApplyService.getSupplyApplyItemList(apply.getId());
        OaSupplyApplyRespVO respVO = CollUtil.getFirst(buildSupplyApplyRespVOList(Collections.singletonList(apply)));
        respVO.setItems(BeanUtils.toBean(items, OaSupplyIssueRespVO.class));
        return respVO;
    }

    /**
     * 拼接申请人及申请部门
     *
     * @param applies 申请列表
     * @return 申请响应列表
     */
    private List<OaSupplyApplyRespVO> buildSupplyApplyRespVOList(List<OaSupplyApplyDO> applies) {
        if (CollUtil.isEmpty(applies)) {
            return Collections.emptyList();
        }
        // 1. 查询申请人及部门
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(convertSet(applies, apply -> Long.valueOf(apply.getCreator())));
        Map<Long, DeptRespDTO> depts = deptApi.getDeptMap(convertSet(applies, OaSupplyApplyDO::getDeptId));
        // 2. 拼接展示字段
        return convertList(applies, apply -> {
            OaSupplyApplyRespVO respVO = BeanUtils.toBean(apply, OaSupplyApplyRespVO.class);
            MapUtils.findAndThen(users, Long.valueOf(apply.getCreator()), user -> respVO.setCreatorName(user.getNickname()));
            MapUtils.findAndThen(depts, apply.getDeptId(), dept -> respVO.setDeptName(dept.getName()));
            return respVO;
        });
    }

}
