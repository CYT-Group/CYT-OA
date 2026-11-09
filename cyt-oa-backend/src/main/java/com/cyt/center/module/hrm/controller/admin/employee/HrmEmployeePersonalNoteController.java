package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.hrm.controller.admin.employee.vo.personalnote.HrmEmployeePersonalNoteCreateReqVO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeePersonalNoteService;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - HRM 员工个人备忘")
@RestController
@RequestMapping("/hrm/employee/personal-note")
@Validated
public class HrmEmployeePersonalNoteController {

    @Resource
    private HrmEmployeePersonalNoteService personalNoteService;
    @Resource
    private HrmEmployeeService employeeService;

    @PostMapping("/create")
    @Operation(summary = "创建员工个人备忘")
    @SaCheckPermission("hrm:employee:personal-note:create")
    public Result<Long> createPersonalNote(
            @Valid @RequestBody HrmEmployeePersonalNoteCreateReqVO reqVO) {
        Long employeeId = employeeService.validateEmployeeBySelf(getLoginUserId()).getId();
        return success(personalNoteService.createPersonalNote(employeeId, reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工个人备忘")
    @Parameter(name = "id", description = "员工个人备忘编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:personal-note:delete")
    public Result<Boolean> deletePersonalNote(@RequestParam("id") Long id) {
        Long employeeId = employeeService.validateEmployeeBySelf(getLoginUserId()).getId();
        personalNoteService.deletePersonalNote(employeeId, id);
        return success(true);
    }

}
