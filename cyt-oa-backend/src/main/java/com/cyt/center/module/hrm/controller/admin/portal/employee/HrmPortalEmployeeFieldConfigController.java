package com.cyt.center.module.hrm.controller.admin.portal.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.controller.admin.employee.vo.config.HrmEmployeeFieldConfigRespVO;
import com.cyt.center.module.hrm.service.employee.config.HrmEmployeeFieldConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 员工端档案字段配置")
@RestController
@RequestMapping("/hrm/portal/employee/field-config")
@Validated
public class HrmPortalEmployeeFieldConfigController {

    @Resource
    private HrmEmployeeFieldConfigService employeeFieldConfigService;

    @GetMapping("/list")
    @Operation(summary = "获得我的员工档案字段配置")
    @SaCheckPermission("hrm:portal:query")
    public Result<List<HrmEmployeeFieldConfigRespVO>> getEmployeeFieldConfigList() {
        return success(employeeFieldConfigService.getEmployeeArchiveFieldConfigList());
    }

}
