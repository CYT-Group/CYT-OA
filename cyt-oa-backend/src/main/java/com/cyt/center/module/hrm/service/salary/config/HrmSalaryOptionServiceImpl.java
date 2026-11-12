package com.cyt.center.module.hrm.service.salary.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.cyt.center.module.hrm.controller.admin.salary.vo.option.HrmSalaryOptionSaveReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionTemplateDO;
import com.cyt.center.module.hrm.dal.postgresql.salary.config.HrmSalaryOptionMapper;
import com.cyt.center.module.hrm.dal.postgresql.salary.config.HrmSalaryOptionTemplateMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertMap;
import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertSet;
import static com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO.ADJUSTABLE_CATEGORY_MAX_CODE;
import static com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO.ADJUSTABLE_CATEGORY_MIN_CODE;
import static com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO.CUSTOM_OPTION_CODE_BASE;
import static com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionDO.ROOT_PARENT_CODE;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_OPTION_CATEGORY_INVALID;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_OPTION_CODE_OCCUPIED;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_OPTION_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.SALARY_OPTION_STANDARD_CANNOT_MODIFY;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * HRM 工资表薪资项 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmSalaryOptionServiceImpl implements HrmSalaryOptionService {

    @Resource
    private HrmSalaryOptionMapper salaryOptionMapper;
    @Resource
    private HrmSalaryOptionTemplateMapper salaryOptionTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)    public Long createSalaryOption(HrmSalaryOptionSaveReqVO createReqVO) {
        // 1. 校验薪资项分类
        HrmSalaryOptionDO category = validateSalaryOptionCategory(createReqVO.getParentCode());

        // 2. 创建薪资项
        HrmSalaryOptionDO option = new HrmSalaryOptionDO()
                .setCode(generateCustomSalaryOptionCode(category.getCode())).setParentCode(category.getCode())
                .setName(createReqVO.getName()).setRemark(createReqVO.getRemark())
                .setSystemFlag(false).setType(category.getType()).setTaxEnabled(category.getTaxEnabled())
                .setVisible(true).setCalculateEnabled(category.getCalculateEnabled()).setEnabled(true);
        salaryOptionMapper.insert(option);;
        return option.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)    public void updateSalaryOptionEnabled(Long id, Boolean enabled) {
        // 1. 校验薪资项
        HrmSalaryOptionDO option = validateSalaryOptionExists(id);
        if (Boolean.TRUE.equals(option.getSystemFlag())) {
            throw exception(SALARY_OPTION_STANDARD_CANNOT_MODIFY);
        }

        // 2. 更新启用状态
        salaryOptionMapper.updateById(new HrmSalaryOptionDO().setId(id).setEnabled(enabled));;;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)    public void updateSalaryOptionVisible(Long id, Boolean visible) {
        // 1. 校验薪资项
        HrmSalaryOptionDO option = validateSalaryOptionExists(id);
        if (Boolean.FALSE.equals(option.getSystemFlag())) {
            throw exception(SALARY_OPTION_STANDARD_CANNOT_MODIFY);
        }

        // 2. 更新显示状态
        salaryOptionMapper.updateById(new HrmSalaryOptionDO().setId(id).setVisible(visible));;;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)    public void deleteSalaryOption(Long id) {
        // 1. 校验薪资项允许删除
        HrmSalaryOptionDO option = validateSalaryOptionExists(id);
        validateCustomSalaryOption(option);

        // 2. 删除薪资项
        salaryOptionMapper.deleteById(id);;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void syncSalaryOption() {
        // 1. 查询标准薪资项目录
        List<HrmSalaryOptionTemplateDO> templates = salaryOptionTemplateMapper.selectListOrderByCode();
        if (CollUtil.isEmpty(templates)) {
            return;
        }

        // 2. 计算待新增和待更新的标准薪资项
        Map<Integer, HrmSalaryOptionDO> optionMap = convertMap(
                salaryOptionMapper.selectListByEnabledAndVisible(null, null), HrmSalaryOptionDO::getCode);
        List<HrmSalaryOptionDO> createOptions = new ArrayList<>();
        List<HrmSalaryOptionDO> updateOptions = new ArrayList<>();
        for (HrmSalaryOptionTemplateDO template : templates) {
            HrmSalaryOptionDO option = optionMap.get(template.getCode());
            if (option == null) {
                HrmSalaryOptionDO occupiedOption = salaryOptionMapper.selectByCodeIncludeDeleted(template.getCode());
                if (occupiedOption != null) {
                    throw exception(SALARY_OPTION_CODE_OCCUPIED, template.getCode());
                }
                createOptions.add(buildSalaryOption(template).setEnabled(true).setVisible(template.getVisible()));
                continue;
            }
            if (option.getTemplateId() == null && Boolean.FALSE.equals(option.getSystemFlag())) {
                throw exception(SALARY_OPTION_CODE_OCCUPIED, template.getCode());
            }
            updateOptions.add(buildSalaryOption(template).setId(option.getId()));
        }

        // 3. 批量同步标准薪资项，保留租户自行维护的 enabled 和 visible
        if (CollUtil.isNotEmpty(createOptions)) {
            salaryOptionMapper.insertBatch(createOptions);
        }
        if (CollUtil.isNotEmpty(updateOptions)) {
            salaryOptionMapper.updateBatch(updateOptions);
        }
    }

    @Override
    public List<HrmSalaryOptionDO> getSalaryOptionList(Boolean adjustable, Boolean visible) {
        List<HrmSalaryOptionDO> options = salaryOptionMapper.selectListByEnabledAndVisible(true, visible);
        // 过滤所属分类已停用的薪资项
        Set<Integer> enabledCategoryCodes = convertSet(options, HrmSalaryOptionDO::getCode,
                option -> ObjectUtil.equal(option.getParentCode(), ROOT_PARENT_CODE));
        options.removeIf(option -> ObjectUtil.notEqual(option.getParentCode(), ROOT_PARENT_CODE)
                && !enabledCategoryCodes.contains(option.getParentCode()));
        // 调薪场景额外只保留可调薪分类下的薪资项
        if (Boolean.TRUE.equals(adjustable)) {
            options.removeIf(option -> option.getParentCode() == null
                    || option.getParentCode() < ADJUSTABLE_CATEGORY_MIN_CODE
                    || option.getParentCode() > ADJUSTABLE_CATEGORY_MAX_CODE);
        }
        return options;
    }

    @Override
    public List<HrmSalaryOptionDO> getSalaryOptionList() {
        return salaryOptionMapper.selectListByEnabledAndVisible(null, null);
    }

    private HrmSalaryOptionDO buildSalaryOption(HrmSalaryOptionTemplateDO template) {
        return new HrmSalaryOptionDO().setCode(template.getCode()).setParentCode(template.getParentCode())
                .setName(template.getName()).setRemark(template.getRemark())
                .setSystemFlag(template.getSystemFlag()).setTemplateId(template.getId()).setType(template.getType())
                .setTaxEnabled(Boolean.TRUE.equals(template.getTaxEnabled()))
                .setCalculateEnabled(template.getCalculateEnabled());
    }

    /**
     * 生成企业自定义薪资项编码
     *
     * <p>分类行通过悲观锁串行生成编码；编码使用企业自定义高位区间，并包含已逻辑删除记录，
     * 避免与现在或未来同步的标准目录编码发生碰撞。</p>
     *
     * @param parentCode 父薪资项编码
     * @return 自定义薪资项编码
     */
    private Integer generateCustomSalaryOptionCode(Integer parentCode) {
        int firstCode = CUSTOM_OPTION_CODE_BASE + parentCode * 10_000 + 1;
        Integer maxCode = salaryOptionMapper.selectMaxCodeByParentCodeAndCodeGreaterThanOrEqual(
                parentCode, firstCode);
        return maxCode == null ? firstCode : Math.max(firstCode, maxCode + 1);
    }

    private HrmSalaryOptionDO validateSalaryOptionExists(Long id) {
        HrmSalaryOptionDO option = salaryOptionMapper.selectById(id);
        if (option == null) {
            throw exception(SALARY_OPTION_NOT_EXISTS);
        }
        return option;
    }

    private HrmSalaryOptionDO validateSalaryOptionCategory(Integer code) {
        HrmSalaryOptionDO category = salaryOptionMapper.selectByCodeForUpdate(code);
        // 1. 校验薪资项分类存在
        if (category == null) {
            throw exception(SALARY_OPTION_CATEGORY_INVALID);
        }
        // 2. 校验选择的是企业可选根分类
        if (ObjectUtil.notEqual(category.getParentCode(), ROOT_PARENT_CODE)) {
            throw exception(SALARY_OPTION_CATEGORY_INVALID);
        }
        if (category.getTemplateId() == null || Boolean.TRUE.equals(category.getSystemFlag())) {
            throw exception(SALARY_OPTION_CATEGORY_INVALID);
        }
        // 3. 校验薪资项分类已启用
        if (ObjectUtil.notEqual(category.getEnabled(), true)) {
            throw exception(SALARY_OPTION_CATEGORY_INVALID);
        }
        return category;
    }

    private void validateCustomSalaryOption(HrmSalaryOptionDO option) {
        // 1. 系统默认项不允许删除
        if (Boolean.TRUE.equals(option.getSystemFlag())) {
            throw exception(SALARY_OPTION_STANDARD_CANNOT_MODIFY);
        }
        // 2. 标准薪资项通过 enabled 管理，不允许物理删除
        if (option.getTemplateId() != null) {
            throw exception(SALARY_OPTION_STANDARD_CANNOT_MODIFY);
        }
    }

}
