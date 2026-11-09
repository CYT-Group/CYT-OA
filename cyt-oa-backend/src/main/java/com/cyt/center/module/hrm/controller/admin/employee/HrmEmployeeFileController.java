package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.file.HrmEmployeeFileRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.file.HrmEmployeeFileSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeFileDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeFileService;
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

@Tag(name = "管理后台 - HRM 员工材料附件")
@RestController
@RequestMapping("/hrm/employee/file")
@Validated
public class HrmEmployeeFileController {

    @Resource
    private HrmEmployeeFileService employeeFileService;

    @GetMapping("/list")
    @Operation(summary = "获得员工材料附件列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeFileRespVO>> getEmployeeFileList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeFileDO> employeeFiles = employeeFileService.getEmployeeFileList(employeeId);
        return success(BeanUtils.toBean(employeeFiles, HrmEmployeeFileRespVO.class));
    }

    @PutMapping("/save")
    @Operation(summary = "保存员工材料附件")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> saveEmployeeFiles(@Valid @RequestBody HrmEmployeeFileSaveReqVO reqVO) {
        employeeFileService.saveEmployeeFiles(reqVO);
        return success(true);
    }

}
