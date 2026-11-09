package com.cyt.center.module.hrm.service.employee.info;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.controller.admin.employee.vo.employee.HrmEmployeeSupplementReqVO;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.hrm.dal.mysql.employee.employment.HrmEmployeeChangeRecordMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HrmEmployeeSupplementServiceTest {
    @org.junit.jupiter.api.BeforeAll static void initializeMapping() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"), HrmEmployeeDO.class);
    }
    final HrmEmployeeMapper employees=mock(HrmEmployeeMapper.class);
    final HrmEmployeeChangeRecordMapper changes=mock(HrmEmployeeChangeRecordMapper.class);
    final HrmEmployeeSupplementService service=new HrmEmployeeSupplementService(employees,changes);
    @Test void rejectsSelfAsManager() {
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setEntryStatus(0));
        assertThrows(BizException.class,()->service.update(cmd(1L,0,null,null,null)));
        verifyNoInteractions(changes);
    }
    @Test void rejectsCyclesThroughOtherEmployees() {
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setEntryStatus(0));
        when(employees.selectByIdForUpdate(2L)).thenReturn(new HrmEmployeeDO().setId(2L).setLeaderEmployeeId(1L));
        assertThrows(BizException.class,()->service.update(cmd(2L,0,null,null,null)));
        verifyNoInteractions(changes);
    }
    @Test void employmentCannotBeInitializedWithoutCompleteInformation() {
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setEntryStatus(0));
        assertThrows(BizException.class,()->service.update(cmd(null,1,null,1,1)));verifyNoInteractions(changes);
    }
    @Test void establishedEmploymentCannotBeChangedThroughSupplementEndpoint() {
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setEntryStatus(1).setType(1).setStatus(2));
        assertThrows(BizException.class,()->service.update(cmd(null,4,null,1,2)));verifyNoInteractions(changes);
    }
    @Test void localSupplementCanBeSavedWithoutWritingWecomIdentity() {
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setEntryStatus(0));
        service.update(cmd(null,0,null,null,null));
        var capture=org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(org.mockito.ArgumentMatchers.isNull(),capture.capture());
        String columns=capture.getValue().getSqlSet();
        assertFalse(columns.contains("user_id"));assertFalse(columns.contains("dept_id"));assertFalse(columns.contains("mobile"));assertFalse(columns.contains("post_name"));
        verify(changes).insert(org.mockito.ArgumentMatchers.any(com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO.class));
    }
    HrmEmployeeSupplementReqVO cmd(Long manager,Integer entry,LocalDateTime time,Integer type,Integer status) {
        return new HrmEmployeeSupplementReqVO(1L,null,manager,time,null,entry,type,status,null,null,null,null);
    }
}
