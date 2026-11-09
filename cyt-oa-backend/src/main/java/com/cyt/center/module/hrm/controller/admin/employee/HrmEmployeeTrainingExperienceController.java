package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.trainingexperience.HrmEmployeeTrainingExperienceRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.trainingexperience.HrmEmployeeTrainingExperienceSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeTrainingExperienceDO;
import com.cyt.center.module.hrm.service.employee.experience.HrmEmployeeTrainingExperienceService;
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

@Tag(name = "管理后台 - HRM 员工培训经历")
@RestController
@RequestMapping("/hrm/employee/training-experience")
@Validated
public class HrmEmployeeTrainingExperienceController {

    @Resource
    private HrmEmployeeTrainingExperienceService trainingExperienceService;

    @GetMapping("/list")
    @Operation(summary = "获得员工培训经历列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeTrainingExperienceRespVO>> getTrainingExperienceList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeTrainingExperienceDO> trainingExperiences =
                trainingExperienceService.getTrainingExperienceListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(trainingExperiences, HrmEmployeeTrainingExperienceRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工培训经历")
    @SaCheckPermission("hrm:employee:update")
    public Result<Long> createTrainingExperience(
            @Valid @RequestBody HrmEmployeeTrainingExperienceSaveReqVO reqVO) {
        return success(trainingExperienceService.createTrainingExperience(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工培训经历")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> updateTrainingExperience(
            @Valid @RequestBody HrmEmployeeTrainingExperienceSaveReqVO reqVO) {
        trainingExperienceService.updateTrainingExperience(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工培训经历")
    @Parameter(name = "id", description = "培训经历编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:delete")
    public Result<Boolean> deleteTrainingExperience(@RequestParam("id") Long id) {
        trainingExperienceService.deleteTrainingExperience(id);
        return success(true);
    }

}
