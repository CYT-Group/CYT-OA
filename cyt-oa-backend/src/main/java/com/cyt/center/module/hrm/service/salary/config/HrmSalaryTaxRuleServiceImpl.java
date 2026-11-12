package com.cyt.center.module.hrm.service.salary.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.salary.vo.taxrule.HrmSalaryTaxRuleSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryTaxRuleDO;
import com.cyt.center.module.hrm.dal.postgresql.salary.config.HrmSalaryTaxRuleMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_TAX_RULE_NAME_DUPLICATE;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_TAX_RULE_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_TAX_RULE_USED;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 计税规则 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmSalaryTaxRuleServiceImpl implements HrmSalaryTaxRuleService {

    @Resource
    private HrmSalaryTaxRuleMapper salaryTaxRuleMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private HrmSalaryGroupService salaryGroupService;

    @Override
    @Transactional(rollbackFor = Exception.class)    public Long createSalaryTaxRule(HrmSalaryTaxRuleSaveReqVO createReqVO) {
        // 1. 校验计税规则名称唯一
        validateSalaryTaxRuleNameUnique(null, createReqVO.getName());

        // 2. 创建计税规则
        HrmSalaryTaxRuleDO salaryTaxRule = BeanUtils.toBean(createReqVO, HrmSalaryTaxRuleDO.class);
        salaryTaxRuleMapper.insert(salaryTaxRule);;
        return salaryTaxRule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)    public void updateSalaryTaxRule(HrmSalaryTaxRuleSaveReqVO updateReqVO) {
        // 1. 校验计税规则
        HrmSalaryTaxRuleDO oldSalaryTaxRule = validateSalaryTaxRuleExists(updateReqVO.getId());
        validateSalaryTaxRuleNameUnique(updateReqVO.getId(), updateReqVO.getName());

        // 2. 更新计税规则
        salaryTaxRuleMapper.updateById(BeanUtils.toBean(updateReqVO, HrmSalaryTaxRuleDO.class));;;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)    public void deleteSalaryTaxRule(Long id) {
        // 1. 校验计税规则存在且未被薪资组使用
        HrmSalaryTaxRuleDO salaryTaxRule = validateSalaryTaxRuleExists(id);
        if (salaryGroupService.getSalaryGroupCountByTaxRuleId(id) > 0) {
            throw exception(SALARY_TAX_RULE_USED);
        }

        // 2. 删除计税规则
        salaryTaxRuleMapper.deleteById(id);;
    }

    @Override
    public HrmSalaryTaxRuleDO getSalaryTaxRule(Long id) {
        return salaryTaxRuleMapper.selectById(id);
    }

    @Override
    public List<HrmSalaryTaxRuleDO> getSalaryTaxRuleList() {
        return salaryTaxRuleMapper.selectList();
    }

    @Override
    public List<HrmSalaryTaxRuleDO> getSalaryTaxRuleList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return salaryTaxRuleMapper.selectByIds(ids);
    }

    @Override
    public HrmSalaryTaxRuleDO validateSalaryTaxRuleExists(Long id) {
        HrmSalaryTaxRuleDO taxRule = salaryTaxRuleMapper.selectById(id);
        if (taxRule == null) {
            throw exception(SALARY_TAX_RULE_NOT_EXISTS);
        }
        return taxRule;
    }

    private void validateSalaryTaxRuleNameUnique(Long id, String name) {
        HrmSalaryTaxRuleDO salaryTaxRule = salaryTaxRuleMapper.selectByName(name);
        if (salaryTaxRule != null && ObjUtil.notEqual(salaryTaxRule.getId(), id)) {
            throw exception(SALARY_TAX_RULE_NAME_DUPLICATE);
        }
    }

}
