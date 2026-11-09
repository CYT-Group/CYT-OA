package com.cyt.center.module.hrm.dal.mysql.salary.config;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.hrm.dal.dataobject.salary.config.HrmSalaryOptionTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmSalaryOptionTemplateMapper extends BaseMapperX<HrmSalaryOptionTemplateDO> {

    default List<HrmSalaryOptionTemplateDO> selectListOrderByCode() {
        return selectList(new LambdaQueryWrapperX<HrmSalaryOptionTemplateDO>()
                .orderByAsc(HrmSalaryOptionTemplateDO::getCode));
    }

}
