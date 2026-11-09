package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contract.HrmEmployeeContractRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contract.HrmEmployeeContractSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeContractDO;
import com.cyt.center.module.hrm.service.employee.employment.HrmEmployeeContractService;
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

@Tag(name = "管理后台 - HRM 员工合同")
@RestController
@RequestMapping("/hrm/employee/contract")
@Validated
public class HrmEmployeeContractController {

    @Resource
    private HrmEmployeeContractService contractService;

    @GetMapping("/list")
    @Operation(summary = "获得员工合同列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeContractRespVO>> getContractList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeContractDO> contracts = contractService.getContractListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(contracts, HrmEmployeeContractRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工合同")
    @SaCheckPermission("hrm:employee:update")
    public Result<Long> createContract(@Valid @RequestBody HrmEmployeeContractSaveReqVO reqVO) {
        return success(contractService.createContract(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工合同")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> updateContract(@Valid @RequestBody HrmEmployeeContractSaveReqVO reqVO) {
        contractService.updateContract(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工合同")
    @Parameter(name = "id", description = "合同编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:delete")
    public Result<Boolean> deleteContract(@RequestParam("id") Long id) {
        contractService.deleteContract(id);
        return success(true);
    }

}
