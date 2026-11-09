package com.cyt.center.module.hrm.controller.admin.salary;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.salary.vo.changetemplate.HrmSalaryChangeTemplateRespVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.changetemplate.HrmSalaryChangeTemplateSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryChangeTemplateDO;
import com.cyt.center.module.hrm.service.salary.config.HrmSalaryChangeTemplateService;
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

@Tag(name = "管理后台 - HRM 调薪模板")
@RestController
@RequestMapping("/hrm/salary/change-template")
@Validated
public class HrmSalaryChangeTemplateController {

    @Resource
    private HrmSalaryChangeTemplateService salaryChangeTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建调薪模板")
    @SaCheckPermission("hrm:salary:change-template:create")
    public Result<Long> createSalaryChangeTemplate(
            @Valid @RequestBody HrmSalaryChangeTemplateSaveReqVO createReqVO) {
        return success(salaryChangeTemplateService.createSalaryChangeTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新调薪模板")
    @SaCheckPermission("hrm:salary:change-template:update")
    public Result<Boolean> updateSalaryChangeTemplate(
            @Valid @RequestBody HrmSalaryChangeTemplateSaveReqVO updateReqVO) {
        salaryChangeTemplateService.updateSalaryChangeTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除调薪模板")
    @Parameter(name = "id", description = "调薪模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:salary:change-template:delete")
    public Result<Boolean> deleteSalaryChangeTemplate(@RequestParam("id") Long id) {
        salaryChangeTemplateService.deleteSalaryChangeTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得调薪模板")
    @Parameter(name = "id", description = "调薪模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:salary:change-template:query")
    public Result<HrmSalaryChangeTemplateRespVO> getSalaryChangeTemplate(@RequestParam("id") Long id) {
        HrmSalaryChangeTemplateDO template = salaryChangeTemplateService.getSalaryChangeTemplate(id);
        return success(BeanUtils.toBean(template, HrmSalaryChangeTemplateRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得调薪模板列表")
    @SaCheckPermission("hrm:salary:change-template:query")
    public Result<List<HrmSalaryChangeTemplateRespVO>> getSalaryChangeTemplateList() {
        List<HrmSalaryChangeTemplateDO> list = salaryChangeTemplateService.getSalaryChangeTemplateList();
        return success(BeanUtils.toBean(list, HrmSalaryChangeTemplateRespVO.class));
    }

}
