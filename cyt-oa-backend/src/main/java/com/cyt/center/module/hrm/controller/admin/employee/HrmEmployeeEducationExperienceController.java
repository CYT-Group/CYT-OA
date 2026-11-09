package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.educationexperience.HrmEmployeeEducationExperienceRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.educationexperience.HrmEmployeeEducationExperienceSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeEducationExperienceDO;
import com.cyt.center.module.hrm.service.employee.experience.HrmEmployeeEducationExperienceService;
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

@Tag(name = "管理后台 - HRM 员工教育经历")
@RestController
@RequestMapping("/hrm/employee/education-experience")
@Validated
public class HrmEmployeeEducationExperienceController {

    @Resource
    private HrmEmployeeEducationExperienceService educationExperienceService;

    @GetMapping("/list")
    @Operation(summary = "获得员工教育经历列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeEducationExperienceRespVO>> getEducationExperienceList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeEducationExperienceDO> educationExperiences =
                educationExperienceService.getEducationExperienceListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(educationExperiences, HrmEmployeeEducationExperienceRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工教育经历")
    @SaCheckPermission("hrm:employee:update")
    public Result<Long> createEducationExperience(
            @Valid @RequestBody HrmEmployeeEducationExperienceSaveReqVO reqVO) {
        return success(educationExperienceService.createEducationExperience(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工教育经历")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> updateEducationExperience(
            @Valid @RequestBody HrmEmployeeEducationExperienceSaveReqVO reqVO) {
        educationExperienceService.updateEducationExperience(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工教育经历")
    @Parameter(name = "id", description = "教育经历编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:delete")
    public Result<Boolean> deleteEducationExperience(@RequestParam("id") Long id) {
        educationExperienceService.deleteEducationExperience(id);
        return success(true);
    }

}
