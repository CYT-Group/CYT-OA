package com.cyt.center.module.hrm.controller.admin.salary;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.salary.vo.slip.template.HrmSalarySlipTemplateOptionVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.slip.template.HrmSalarySlipTemplateRespVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.slip.template.HrmSalarySlipTemplateSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipTemplateDO;
import com.cyt.center.module.hrm.service.salary.slip.HrmSalarySlipTemplateService;
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

import java.util.ArrayList;
import java.util.List;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO.ROOT_PARENT_CODE;

@Tag(name = "管理后台 - HRM 工资条模板")
@RestController
@RequestMapping("/hrm/salary/slip-template")
@Validated
public class HrmSalarySlipTemplateController {

    @Resource
    private HrmSalarySlipTemplateService salarySlipTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建工资条模板")
    @SaCheckPermission("hrm:salary:slip:update")
    public Result<Long> createSalarySlipTemplate(
            @Valid @RequestBody HrmSalarySlipTemplateSaveReqVO createReqVO) {
        return success(salarySlipTemplateService.createSalarySlipTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工资条模板")
    @SaCheckPermission("hrm:salary:slip:update")
    public Result<Boolean> updateSalarySlipTemplate(
            @Valid @RequestBody HrmSalarySlipTemplateSaveReqVO updateReqVO) {
        salarySlipTemplateService.updateSalarySlipTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工资条模板")
    @Parameter(name = "id", description = "模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:salary:slip:delete")
    public Result<Boolean> deleteSalarySlipTemplate(@RequestParam("id") Long id) {
        salarySlipTemplateService.deleteSalarySlipTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工资条模板")
    @Parameter(name = "id", description = "工资条模板编号", required = true, example = "1024")
    @SaCheckPermission("hrm:salary:slip:query")
    public Result<HrmSalarySlipTemplateRespVO> getSalarySlipTemplate(@RequestParam("id") Long id) {
        return success(buildSalarySlipTemplateRespVO(
                salarySlipTemplateService.getSalarySlipTemplate(id)));
    }

    @GetMapping("/list")
    @Operation(summary = "获得工资条模板列表")
    @SaCheckPermission("hrm:salary:slip:query")
    public Result<List<HrmSalarySlipTemplateRespVO>> getSalarySlipTemplateList() {
        return success(convertList(salarySlipTemplateService.getSalarySlipTemplateList(),
                this::buildSalarySlipTemplateRespVO));
    }

    // ==================== 拼接 VO ====================

    private HrmSalarySlipTemplateRespVO buildSalarySlipTemplateRespVO(HrmSalarySlipTemplateDO template) {
        if (template == null) {
            return null;
        }
        return BeanUtils.toBean(template, HrmSalarySlipTemplateRespVO.class)
                .setOptions(buildSalarySlipTemplateOptionRespVOList(template.getOptions()));
    }

    private List<HrmSalarySlipTemplateOptionVO> buildSalarySlipTemplateOptionRespVOList(
            List<HrmSalarySlipTemplateDO.Option> options) {
        List<HrmSalarySlipTemplateOptionVO> result = new ArrayList<>(convertList(options,
                option -> BeanUtils.toBean(option, HrmSalarySlipTemplateOptionVO.class)
                        .setParentCode(ROOT_PARENT_CODE)));
        for (HrmSalarySlipTemplateDO.Option option : options) {
            result.addAll(convertList(option.getChildren(), child ->
                    BeanUtils.toBean(child, HrmSalarySlipTemplateOptionVO.class)
                            .setParentCode(option.getCode())));
        }
        return result;
    }

}
