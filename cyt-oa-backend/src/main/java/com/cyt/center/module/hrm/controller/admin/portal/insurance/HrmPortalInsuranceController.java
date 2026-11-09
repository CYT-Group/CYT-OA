package com.cyt.center.module.hrm.controller.admin.portal.insurance;

import cn.dev33.satoken.annotation.SaCheckPermission;

import cn.hutool.core.util.BooleanUtil;
import com.cyt.center.core.result.Result;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.framework.ip.core.utils.AreaUtils;
import com.cyt.center.module.hrm.controller.admin.portal.insurance.vo.HrmPortalInsuranceRecordRespVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.dataobject.insurance.monthrecord.HrmInsuranceMonthEmployeeRecordDO;
import com.cyt.center.module.hrm.dal.dataobject.insurance.config.HrmInsuranceSchemeDO;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.module.hrm.service.insurance.monthrecord.HrmInsuranceMonthEmployeeRecordService;
import com.cyt.center.module.hrm.service.insurance.config.HrmInsuranceSchemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.INSURANCE_MONTH_EMP_RECORD_NOT_EXISTS;

@Tag(name = "管理后台 - HRM 员工端社保")
@RestController
@RequestMapping("/hrm/portal/insurance/record")
@Validated
public class HrmPortalInsuranceController {

    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    private HrmInsuranceMonthEmployeeRecordService insuranceMonthEmployeeRecordService;
    @Resource
    private HrmInsuranceSchemeService insuranceSchemeService;

    @GetMapping("/list")
    @Operation(summary = "获得我的社保记录")
    @Parameter(name = "year", description = "年份", example = "2026")
    @SaCheckPermission("hrm:portal:query")
    public Result<List<HrmPortalInsuranceRecordRespVO>> getInsuranceRecordList(
            @RequestParam(value = "year", required = false) Integer year) {
        HrmEmployeeDO employee = employeeService.validateEmployeeBySelf(getLoginUserId());
        List<HrmInsuranceMonthEmployeeRecordDO> records =
                insuranceMonthEmployeeRecordService.getMonthEmployeeRecordListByEmployeeIdAndYear(
                        employee.getId(), year);
        // 拼接 VO
        return success(buildInsuranceRecordRespVOList(records));
    }

    @GetMapping("/get")
    @Operation(summary = "获得我的社保记录详情")
    @Parameter(name = "id", description = "社保记录编号", required = true, example = "1024")
    @SaCheckPermission("hrm:portal:query")
    public Result<HrmPortalInsuranceRecordRespVO> getInsuranceRecord(@RequestParam("id") Long id) {
        HrmEmployeeDO employee = employeeService.validateEmployeeBySelf(getLoginUserId());
        HrmInsuranceMonthEmployeeRecordDO record = insuranceMonthEmployeeRecordService.getMonthEmployeeRecord(id);
        if (record == null || notEqual(record.getEmployeeId(), employee.getId())) {
            throw exception(INSURANCE_MONTH_EMP_RECORD_NOT_EXISTS);
        }
        // 拼接 VO
        return success(buildInsuranceRecordRespVO(
                record, insuranceSchemeService.getScheme(record.getSchemeId()), true));
    }

    // ==================== 拼接 VO ====================

    /**
     * 构建社保记录响应列表
     *
     * @param records 社保记录列表
     * @return 社保记录响应列表
     */
    private List<HrmPortalInsuranceRecordRespVO> buildInsuranceRecordRespVOList(
            List<HrmInsuranceMonthEmployeeRecordDO> records) {
        Map<Long, HrmInsuranceSchemeDO> schemeMap = insuranceSchemeService.getSchemeMap(
                convertSet(records, HrmInsuranceMonthEmployeeRecordDO::getSchemeId));
        return convertList(records, record ->
                buildInsuranceRecordRespVO(record, schemeMap.get(record.getSchemeId()), false));
    }

    /**
     * 构建社保记录响应
     *
     * @param record 社保记录
     * @param scheme 社保方案
     * @param includeProjects 是否返回社保项目
     * @return 社保记录响应
     */
    private HrmPortalInsuranceRecordRespVO buildInsuranceRecordRespVO(
            HrmInsuranceMonthEmployeeRecordDO record, HrmInsuranceSchemeDO scheme,
            boolean includeProjects) {
        HrmPortalInsuranceRecordRespVO respVO = BeanUtils.toBean(record, HrmPortalInsuranceRecordRespVO.class);
        if (scheme != null) {
            respVO.setSchemeName(scheme.getName()).setSchemeType(scheme.getType())
                    .setSchemeCity(AreaUtils.format(scheme.getAreaId()));
        }
        if (BooleanUtil.isFalse(includeProjects)) {
            respVO.setProjects(null);
        }
        return respVO;
    }

}
