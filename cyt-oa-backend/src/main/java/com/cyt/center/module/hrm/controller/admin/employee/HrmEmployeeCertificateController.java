package com.cyt.center.module.hrm.controller.admin.employee;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.certificate.HrmEmployeeCertificateRespVO;
import com.cyt.center.module.hrm.controller.admin.employee.vo.certificate.HrmEmployeeCertificateSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.experience.HrmEmployeeCertificateDO;
import com.cyt.center.module.hrm.service.employee.experience.HrmEmployeeCertificateService;
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

@Tag(name = "管理后台 - HRM 员工证书")
@RestController
@RequestMapping("/hrm/employee/certificate")
@Validated
public class HrmEmployeeCertificateController {

    @Resource
    private HrmEmployeeCertificateService certificateService;

    @GetMapping("/list")
    @Operation(summary = "获得员工证书列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:query")
    public Result<List<HrmEmployeeCertificateRespVO>> getCertificateList(
            @RequestParam("employeeId") Long employeeId) {
        List<HrmEmployeeCertificateDO> certificates = certificateService.getCertificateListByEmployeeId(employeeId);
        return success(BeanUtils.toBean(certificates, HrmEmployeeCertificateRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建员工证书")
    @SaCheckPermission("hrm:employee:update")
    public Result<Long> createCertificate(
            @Valid @RequestBody HrmEmployeeCertificateSaveReqVO reqVO) {
        return success(certificateService.createCertificate(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工证书")
    @SaCheckPermission("hrm:employee:update")
    public Result<Boolean> updateCertificate(
            @Valid @RequestBody HrmEmployeeCertificateSaveReqVO reqVO) {
        certificateService.updateCertificate(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工证书")
    @Parameter(name = "id", description = "证书编号", required = true, example = "1024")
    @SaCheckPermission("hrm:employee:delete")
    public Result<Boolean> deleteCertificate(@RequestParam("id") Long id) {
        certificateService.deleteCertificate(id);
        return success(true);
    }

}
