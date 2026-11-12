package com.cyt.center.module.hrm.service.employee.employment;

import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import cn.hutool.core.collection.CollUtil;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.employee.vo.salarycard.HrmEmployeeSalaryCardSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeSalaryCardDO;
import com.cyt.center.module.hrm.dal.postgresql.employee.employment.HrmEmployeeSalaryCardMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.hutool.core.util.ObjectUtil.notEqual;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_RESOURCE_BELONG_INVALID;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.EMPLOYEE_SALARY_CARD_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.HRM_EMPLOYEE_SALARY_CARD_SAVE_SUB_TYPE;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.HRM_EMPLOYEE_SALARY_CARD_SAVE_SUCCESS;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.HRM_EMPLOYEE_SALARY_CARD_DELETE_SUB_TYPE;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.HRM_EMPLOYEE_SALARY_CARD_DELETE_SUCCESS;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.HRM_EMPLOYEE_TYPE;

/**
 * HRM 员工工资卡 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmEmployeeSalaryCardServiceImpl implements HrmEmployeeSalaryCardService {

    @Resource
    private HrmEmployeeSalaryCardMapper salaryCardMapper;
    @Resource
    private HrmEmployeeService employeeService;

    @Override    public Long saveSalaryCard(HrmEmployeeSalaryCardSaveReqVO reqVO) {
        // 1. 校验员工和工资卡
        HrmEmployeeDO employee = employeeService.validateEmployeeExists(reqVO.getEmployeeId());
        HrmEmployeeSalaryCardDO oldSalaryCard = reqVO.getId() == null
                ? salaryCardMapper.selectByEmployeeId(reqVO.getEmployeeId())
                : validateSalaryCardExists(reqVO.getId());
        if (oldSalaryCard != null && notEqual(oldSalaryCard.getEmployeeId(), reqVO.getEmployeeId())) {
            throw exception(EMPLOYEE_RESOURCE_BELONG_INVALID, "工资卡");
        }

        // 2. 保存员工工资卡
        HrmEmployeeSalaryCardDO salaryCard = BeanUtils.toBean(reqVO, HrmEmployeeSalaryCardDO.class);
        if (salaryCard.getId() == null && oldSalaryCard != null) {
            salaryCard.setId(oldSalaryCard.getId());
        }
        salaryCardMapper.insertOrUpdate(salaryCard);;;
        return salaryCard.getId();
    }

    @Override    public void deleteSalaryCardByEmployeeId(Long employeeId) {
        // 1. 校验员工和工资卡存在
        HrmEmployeeDO employee = employeeService.validateEmployeeExists(employeeId);
        HrmEmployeeSalaryCardDO salaryCard = salaryCardMapper.selectByEmployeeId(employeeId);
        if (salaryCard == null) {
            throw exception(EMPLOYEE_SALARY_CARD_NOT_EXISTS);
        }

        // 2. 删除工资卡
        salaryCardMapper.deleteById(salaryCard.getId());;
    }

    @Override
    public HrmEmployeeSalaryCardDO validateSalaryCardExists(Long id) {
        HrmEmployeeSalaryCardDO salaryCard = salaryCardMapper.selectById(id);
        if (salaryCard == null) {
            throw exception(EMPLOYEE_SALARY_CARD_NOT_EXISTS);
        }
        return salaryCard;
    }

    @Override
    public HrmEmployeeSalaryCardDO getSalaryCardByEmployeeId(Long employeeId) {
        return salaryCardMapper.selectByEmployeeId(employeeId);
    }

    @Override
    public List<HrmEmployeeSalaryCardDO> getSalaryCardList(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return Collections.emptyList();
        }
        // 历史重复数据按工资卡编号倒序只保留最新一张，维持员工单卡查询契约
        Map<Long, HrmEmployeeSalaryCardDO> salaryCardMap = new LinkedHashMap<>();
        for (HrmEmployeeSalaryCardDO salaryCard : salaryCardMapper.selectListByEmployeeIds(employeeIds)) {
            salaryCardMap.putIfAbsent(salaryCard.getEmployeeId(), salaryCard);
        }
        return CollUtil.newArrayList(salaryCardMap.values());
    }

}
