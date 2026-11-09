package com.cyt.center.module.hrm.dal.mysql.salary.monthrecord;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.hrm.controller.admin.salary.vo.monthrecord.HrmSalaryMonthRecordPageReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.monthrecord.HrmSalaryMonthRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmSalaryMonthRecordMapper extends BaseMapperX<HrmSalaryMonthRecordDO> {

    default HrmSalaryMonthRecordDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(HrmSalaryMonthRecordDO::getId, id);
    }

    default HrmSalaryMonthRecordDO selectByYearMonth(Integer year, Integer month) {
        return selectLastOne(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .eq(HrmSalaryMonthRecordDO::getYear, year)
                .eq(HrmSalaryMonthRecordDO::getMonth, month)
                .orderByAsc(HrmSalaryMonthRecordDO::getId));
    }

    default HrmSalaryMonthRecordDO selectLast() {
        return selectFirstOne(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .orderByDesc(HrmSalaryMonthRecordDO::getYear)
                .orderByDesc(HrmSalaryMonthRecordDO::getMonth));
    }

    default PageResult<HrmSalaryMonthRecordDO> selectPage(HrmSalaryMonthRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .eqIfPresent(HrmSalaryMonthRecordDO::getYear, reqVO.getYear())
                .eqIfPresent(HrmSalaryMonthRecordDO::getMonth, reqVO.getMonth())
                .eqIfPresent(HrmSalaryMonthRecordDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrmSalaryMonthRecordDO::getYear)
                .orderByDesc(HrmSalaryMonthRecordDO::getMonth));
    }

    default List<HrmSalaryMonthRecordDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<HrmSalaryMonthRecordDO>()
                .eqIfPresent(HrmSalaryMonthRecordDO::getStatus, status)
                .orderByDesc(HrmSalaryMonthRecordDO::getYear)
                .orderByDesc(HrmSalaryMonthRecordDO::getMonth));
    }

}
