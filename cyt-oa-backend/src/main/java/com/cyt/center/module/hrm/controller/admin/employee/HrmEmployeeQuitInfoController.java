package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.quitinfo.HrmEmployeeQuitInfoRespVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeQuitInfoDO;
import com.cyt.center.module.hrm.service.employee.employment.HrmEmployeeQuitInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 员工离职信息")
@RestController
@RequestMapping("/hrm/employee/quit-info")
@Validated
public class HrmEmployeeQuitInfoController {

    @Resource
    private HrmEmployeeQuitInfoService quitInfoService;

    @GetMapping("/get")
    @Operation(summary = "获得员工离职信息")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<HrmEmployeeQuitInfoRespVO> getQuitInfo(
            @RequestParam("employeeId") Long employeeId) {
        HrmEmployeeQuitInfoDO quitInfo = quitInfoService.getQuitInfoByEmployeeId(employeeId);
        return success(BeanUtils.toBean(quitInfo, HrmEmployeeQuitInfoRespVO.class));
    }

}
