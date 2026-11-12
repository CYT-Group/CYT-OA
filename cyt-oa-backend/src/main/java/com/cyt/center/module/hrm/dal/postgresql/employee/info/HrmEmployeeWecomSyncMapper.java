package com.cyt.center.module.hrm.dal.postgresql.employee.info;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeWecomSyncDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HrmEmployeeWecomSyncMapper extends BaseMapper<HrmEmployeeWecomSyncDO> {
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'hrm_employee_wecom_sync'")
    int countSyncTable();
}
