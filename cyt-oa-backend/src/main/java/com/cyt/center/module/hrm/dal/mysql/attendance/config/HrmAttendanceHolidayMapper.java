package com.cyt.center.module.hrm.dal.mysql.attendance.config;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.hrm.controller.admin.attendance.vo.holiday.HrmAttendanceHolidayPageReqVO;
import com.cyt.center.module.hrm.dal.dataobject.attendance.config.HrmAttendanceHolidayDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HrmAttendanceHolidayMapper extends BaseMapperX<HrmAttendanceHolidayDO> {

    default PageResult<HrmAttendanceHolidayDO> selectPage(HrmAttendanceHolidayPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmAttendanceHolidayDO>()
                .eqIfPresent(HrmAttendanceHolidayDO::getType, reqVO.getType())
                .betweenIfPresent(HrmAttendanceHolidayDO::getDate, reqVO.getDate())
                .orderByDesc(HrmAttendanceHolidayDO::getDate)
                .orderByDesc(HrmAttendanceHolidayDO::getId));
    }

    default HrmAttendanceHolidayDO selectByDate(LocalDateTime date) {
        return selectLastOne(new LambdaQueryWrapperX<HrmAttendanceHolidayDO>()
                .eq(HrmAttendanceHolidayDO::getDate, date)
                .orderByAsc(HrmAttendanceHolidayDO::getId));
    }

    default List<HrmAttendanceHolidayDO> selectListByDateRange(LocalDateTime[] dateTimes) {
        return selectList(new LambdaQueryWrapperX<HrmAttendanceHolidayDO>()
                .betweenIfPresent(HrmAttendanceHolidayDO::getDate, dateTimes)
                .orderByAsc(HrmAttendanceHolidayDO::getDate)
                .orderByDesc(HrmAttendanceHolidayDO::getId));
    }

}
