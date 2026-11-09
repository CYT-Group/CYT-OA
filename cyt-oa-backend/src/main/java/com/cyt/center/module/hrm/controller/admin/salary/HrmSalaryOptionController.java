package com.cyt.center.module.hrm.controller.admin.salary;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionRespVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionUpdateEnabledReqVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionUpdateVisibleReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import com.cyt.center.module.hrm.service.salary.config.HrmSalaryOptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - HRM 薪资项")
@RestController
@RequestMapping("/hrm/salary/option")
@Validated
public class HrmSalaryOptionController {

    @Resource
    private HrmSalaryOptionService salaryOptionService;

    @PostMapping("/create")
    @Operation(summary = "创建薪资项")
    @SaCheckPermission("hrm:salary:option:create")
    public Result<Long> createSalaryOption(@Valid @RequestBody HrmSalaryOptionSaveReqVO reqVO) {
        return success(salaryOptionService.createSalaryOption(reqVO));
    }

    @PutMapping("/update-enabled")
    @Operation(summary = "更新薪资项启用状态")
    @SaCheckPermission("hrm:salary:option:update")
    public Result<Boolean> updateSalaryOptionEnabled(
            @Valid @RequestBody HrmSalaryOptionUpdateEnabledReqVO reqVO) {
        salaryOptionService.updateSalaryOptionEnabled(reqVO.getId(), reqVO.getEnabled());
        return success(true);
    }

    @PutMapping("/update-visible")
    @Operation(summary = "更新薪资项显示状态")
    @SaCheckPermission("hrm:salary:option:update")
    public Result<Boolean> updateSalaryOptionVisible(
            @Valid @RequestBody HrmSalaryOptionUpdateVisibleReqVO reqVO) {
        salaryOptionService.updateSalaryOptionVisible(reqVO.getId(), reqVO.getVisible());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除薪资项")
    @Parameter(name = "id", description = "薪资项编号", required = true, example = "1024")
    @SaCheckPermission("hrm:salary:option:delete")
    public Result<Boolean> deleteSalaryOption(@RequestParam("id") Long id) {
        salaryOptionService.deleteSalaryOption(id);
        return success(true);
    }

    @PutMapping("/sync")
    @Operation(summary = "同步标准薪资项")
    @SaCheckPermission("hrm:salary:option:update")
    public Result<Boolean> syncSalaryOption() {
        salaryOptionService.syncSalaryOption();
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获得薪资项列表")
    @SaCheckPermission("hrm:salary:option:query")
    public Result<List<HrmSalaryOptionRespVO>> getSalaryOptionList() {
        List<HrmSalaryOptionDO> list = salaryOptionService.getSalaryOptionList();
        return success(BeanUtils.toBean(list, HrmSalaryOptionRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得薪资项精简列表", description = "只包含已启用的薪资项")
    @Parameter(name = "adjustable", description = "是否可调薪", example = "true")
    @SaCheckPermission("hrm:salary:option:query")
    public Result<List<HrmSalaryOptionRespVO>> getSalaryOptionSimpleList(
            @RequestParam(value = "adjustable", required = false) Boolean adjustable) {
        List<HrmSalaryOptionDO> list = salaryOptionService.getSalaryOptionList(adjustable);
        return success(convertList(list, option -> new HrmSalaryOptionRespVO()
                .setCode(option.getCode()).setParentCode(option.getParentCode())
                .setName(option.getName()).setCalculateEnabled(option.getCalculateEnabled())));
    }

}
