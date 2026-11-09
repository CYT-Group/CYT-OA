package com.cyt.center.module.hrm.controller.admin.salary;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.salary.vo.config.HrmSalaryConfigCreateReqVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.config.HrmSalaryConfigRespVO;
import com.cyt.center.module.hrm.controller.admin.salary.vo.config.HrmSalaryConfigUpdateReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryConfigDO;
import com.cyt.center.module.hrm.service.salary.config.HrmSalaryConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;

@Tag(name = "管理后台 - HRM 薪资配置")
@RestController
@RequestMapping("/hrm/salary/config")
@Validated
public class HrmSalaryConfigController {

    @Resource
    private HrmSalaryConfigService salaryConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建薪资配置")
    @SaCheckPermission("hrm:salary:config:update")
    public Result<Long> createSalaryConfig(@Valid @RequestBody HrmSalaryConfigCreateReqVO reqVO) {
        return success(salaryConfigService.createSalaryConfig(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新薪资配置")
    @SaCheckPermission("hrm:salary:config:update")
    public Result<Boolean> updateSalaryConfig(@Valid @RequestBody HrmSalaryConfigUpdateReqVO reqVO) {
        salaryConfigService.updateSalaryConfig(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得薪资配置")
    @SaCheckPermission("hrm:salary:config:query")
    public Result<HrmSalaryConfigRespVO> getSalaryConfig() {
        HrmSalaryConfigDO config = salaryConfigService.getSalaryConfig();
        return success(BeanUtils.toBean(config, HrmSalaryConfigRespVO.class));
    }

}
