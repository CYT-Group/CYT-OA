package com.cyt.center.module.hrm.dal.postgresql.salary.config;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryChangeTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmSalaryChangeTemplateMapper extends BaseMapperX<HrmSalaryChangeTemplateDO> {

    default List<HrmSalaryChangeTemplateDO> selectListByIdDesc() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryChangeTemplateDO>()
                .orderByDesc(HrmSalaryChangeTemplateDO::getId));
    }

    default List<HrmSalaryChangeTemplateDO> selectListByDefaultStatus(Boolean defaultStatus) {
        return selectList(HrmSalaryChangeTemplateDO::getDefaultStatus, defaultStatus);
    }

}
