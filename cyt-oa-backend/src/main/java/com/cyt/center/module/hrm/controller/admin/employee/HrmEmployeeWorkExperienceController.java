package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.workexperience.HrmEmployeeWorkExperienceRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.workexperience.HrmEmployeeWorkExperienceSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeWorkExperienceDO;
import com.cyt.center.module.hrm.service.employee.experience.HrmEmployeeWorkExperienceService;
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

@Tag(name = "管理后台 - HRM 员工工作经历")
@RestController
@RequestMapping("/hrm/employee/work-experience")
@Validated
public class HrmEmployeeWorkExperienceController {

    @Resource
    private HrmEmployeeWorkExperienceService workExperienceService;

    @GetMapping("/list")
    @Operation(summary = "获得员工工作经历列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeWorkExperienceRespVO>> getWorkExperienceList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeWorkExperienceDO> workExperiences =
                workExperienceService.getWorkExperienceListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(workExperiences, HrmEmployeeWorkExperienceRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工工作经历")
    @SaCheckPermission("hrm:employee:update")
    public Result<Long> createWorkExperience(
            @Valid @RequestBody HrmEmployeeWorkExperienceSaveReqVO reqVO) {
        return success(workExperienceService.createWorkExperience(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工工作经历")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> updateWorkExperience(
            @Valid @RequestBody HrmEmployeeWorkExperienceSaveReqVO reqVO) {
        workExperienceService.updateWorkExperience(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工工作经历")
    @Parameter(name = "id", description = "工作经历编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:delete")
    public Result<Boolean> deleteWorkExperience(@RequestParam("id") Long id) {
        workExperienceService.deleteWorkExperience(id);
        return success(true);
    }

}
