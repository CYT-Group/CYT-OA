package com.cyt.center.module.hrm.service.employee.employment;

import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.contract.HrmEmployeeContractSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeContractDO;
import com.cyt.center.module.hrm.dal.postgresql.employee.employment.HrmEmployeeContractMapper;
import com.cyt.center.module.hrm.enums.employee.employment.HrmEmployeeContractTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.date.LocalDateTimeUtils.getDayBeginTime;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_CONTRACT_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_RESOURCE_BELONG_INVALID;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 员工合同 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmEmployeeContractServiceImpl implements HrmEmployeeContractService {

    @Resource
    private HrmEmployeeContractMapper contractMapper;
    @Resource
    private HrmEmployeeService employeeService;

    @Override    public Long createContract(HrmEmployeeContractSaveReqVO reqVO) {
        // 1. 校验员工
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());

        // 2. 创建合同
        HrmEmployeeContractDO contract = BeanUtils.toBean(reqVO, HrmEmployeeContractDO.class)
                .setStartTime(getDayBeginTime(reqVO.getStartTime()))
                .setEndTime(getDayBeginTime(reqVO.getEndTime()))
                .setSignTime(getDayBeginTime(reqVO.getSignTime()));
        if (HrmEmployeeContractTypeEnum.NON_FIXED_TERM_LABOR_CONTRACT.getType().equals(contract.getType())) {
            contract.setTerm(null);
        }
        contractMapper.insert(contract);;
        return contract.getId();
    }

    @Override    public void updateContract(HrmEmployeeContractSaveReqVO reqVO) {
        // 1. 校验员工和合同
        employeeService.validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeContractDO contract = validateContractExists(reqVO.getId());
        if (notEqual(contract.getEmployeeId(), reqVO.getEmployeeId())) {
            throw exception(EMPLOYEE_RESOURCE_BELONG_INVALID, "合同");
        }

        // 2. 更新合同
        contractMapper.updateById(BeanUtils.toBean(reqVO, HrmEmployeeContractDO.class)
                .setEmployeeId(null)
                .setStartTime(getDayBeginTime(reqVO.getStartTime()))
                .setEndTime(getDayBeginTime(reqVO.getEndTime()))
                .setSignTime(getDayBeginTime(reqVO.getSignTime())));
        if (HrmEmployeeContractTypeEnum.NON_FIXED_TERM_LABOR_CONTRACT.getType().equals(reqVO.getType())) {
            contractMapper.updateTermById(reqVO.getId(), null);
        };
    }

    @Override    public void deleteContract(Long id) {
        // 1. 校验员工合同存在
        HrmEmployeeContractDO contract = validateContractExists(id);

        // 2. 删除员工合同
        contractMapper.deleteById(id);;
    }

    @Override
    public HrmEmployeeContractDO validateContractExists(Long id) {
        HrmEmployeeContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(EMPLOYEE_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    public List<HrmEmployeeContractDO> getContractListByEmployeeId(Long employeeId) {
        return contractMapper.selectListByEmployeeId(employeeId);
    }

    @Override
    public Long getExpireEmployeeCountInMonth(LocalDateTime[] endTimes) {
        return contractMapper.selectCountByEndTimeBetween(endTimes);
    }

}
