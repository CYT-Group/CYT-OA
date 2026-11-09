package com.cyt.center.module.hrm.dal.mysql.salary.slip;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.hrm.controller.admin.salary.vo.slip.sendrecord.HrmSalarySlipSendRecordPageReqVO;
import com.cyt.center.module.hrm.dal.dataobject.salary.slip.HrmSalarySlipSendRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HrmSalarySlipSendRecordMapper extends BaseMapperX<HrmSalarySlipSendRecordDO> {

    default PageResult<HrmSalarySlipSendRecordDO> selectPage(HrmSalarySlipSendRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmSalarySlipSendRecordDO>()
                .eqIfPresent(HrmSalarySlipSendRecordDO::getYear, reqVO.getYear())
                .eqIfPresent(HrmSalarySlipSendRecordDO::getMonth, reqVO.getMonth())
                .orderByDesc(HrmSalarySlipSendRecordDO::getYear)
                .orderByDesc(HrmSalarySlipSendRecordDO::getMonth));
    }

}
