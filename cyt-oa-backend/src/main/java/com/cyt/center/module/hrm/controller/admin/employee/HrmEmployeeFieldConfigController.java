package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.controller.admin.employee.vo.config.HrmEmployeeArchiveFieldConfigSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.config.HrmEmployeeCreateFieldConfigSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.config.HrmEmployeeFieldConfigRespVO;
import com.cyt.center.module.hrm.service.employee.config.HrmEmployeeFieldConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 员工字段配置")
@RestController
@RequestMapping("/hrm/employee/config")
@Validated
public class HrmEmployeeFieldConfigController {

    @Resource
    private HrmEmployeeFieldConfigService employeeFieldConfigService;

    @GetMapping("/create-field/list")
    @Operation(summary = "获得新建员工字段配置")
    @Parameter(name = "entryStatus", description = "入职状态：1 在职，2 待入职", required = true, example = "1")
    @SaCheckPermission("hrm:employee:config:query")
    public Result<List<HrmEmployeeFieldConfigRespVO>> getEmployeeCreateFieldConfigList(
            @RequestParam("entryStatus") Integer entryStatus) {
        return success(employeeFieldConfigService.getEmployeeCreateFieldConfigList(entryStatus));
    }

    @PutMapping("/create-field/save")
    @Operation(summary = "保存新建员工字段配置")
    @SaCheckPermission("hrm:employee:config:update")
    public Result<Boolean> saveEmployeeCreateFieldConfig(
            @Valid @RequestBody HrmEmployeeCreateFieldConfigSaveReqVO reqVO) {
        employeeFieldConfigService.saveEmployeeCreateFieldConfig(reqVO);
        return success(true);
    }

    @GetMapping("/archive-field/list")
    @Operation(summary = "获得员工档案字段配置")
    @SaCheckPermission("hrm:employee:config:query")
    public Result<List<HrmEmployeeFieldConfigRespVO>> getEmployeeArchiveFieldConfigList() {
        return success(employeeFieldConfigService.getEmployeeArchiveFieldConfigList());
    }

    @PutMapping("/archive-field/save")
    @Operation(summary = "保存员工档案字段配置")
    @SaCheckPermission("hrm:employee:config:update")
    public Result<Boolean> saveEmployeeArchiveFieldConfig(
            @Valid @RequestBody HrmEmployeeArchiveFieldConfigSaveReqVO reqVO) {
        employeeFieldConfigService.saveEmployeeArchiveFieldConfig(reqVO);
        return success(true);
    }

}
