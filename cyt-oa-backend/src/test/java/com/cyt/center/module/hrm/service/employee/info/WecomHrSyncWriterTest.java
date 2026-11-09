package com.cyt.center.module.hrm.service.employee.info;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.*;
import com.cyt.center.module.hrm.dal.mysql.employee.info.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class WecomHrSyncWriterTest {
    final HrmEmployeeMapper employees=mock(HrmEmployeeMapper.class);
    final HrmEmployeeWecomSyncMapper states=mock(HrmEmployeeWecomSyncMapper.class);
    final WecomHrSyncWriter writer=new WecomHrSyncWriter(employees,states);
    @BeforeAll static void mapping(){com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"test"),HrmEmployeeDO.class);}
    @Test void missingSchemaStopsSyncWithUpgradeInstructions(){
        var error = assertThrows(BizException.class, writer::requireSchema);
        assertTrue(error.getMessage().contains("db/wecom-hr-sync-schema.sql"));
        verifyNoInteractions(employees);
        verify(states).countSyncTable();
        verifyNoMoreInteractions(states);
    }
    @Test void syncChecksSchemaBeforeReadingGatewayAndReleasesRunningFlag(){
        var gateway = mock(com.cyt.center.core.integration.WecomGatewayClient.class);
        var users = mock(com.cyt.center.module.system.mapper.SysUserMapper.class);
        var service = new WecomHrSyncService(gateway, employees, users, writer);
        assertTrue(assertThrows(BizException.class, service::sync).getMessage().contains("数据库升级"));
        assertTrue(assertThrows(BizException.class, service::sync).getMessage().contains("数据库升级"));
        verifyNoInteractions(gateway, employees, users);
        verify(states, times(2)).countSyncTable();
        verifyNoMoreInteractions(states);
    }
    @Test void confirmationChecksSchemaBeforeReadingEmployeeAndGateway(){
        var gateway = mock(com.cyt.center.core.integration.WecomGatewayClient.class);
        var users = mock(com.cyt.center.module.system.mapper.SysUserMapper.class);
        var service = new WecomHrSyncService(gateway, employees, users, writer);
        assertThrows(BizException.class, () -> service.confirm(1L, "birthday", null, "1990-01-01"));
        verifyNoInteractions(gateway, employees, users);
        verify(states).countSyncTable();
        verifyNoMoreInteractions(states);
    }
    @Test void existingSchemaPassesReadOnlyCheck(){
        when(states.countSyncTable()).thenReturn(1);
        assertDoesNotThrow(writer::requireSchema);
        verifyNoInteractions(employees);
        verify(states).countSyncTable();
        verifyNoMoreInteractions(states);
    }
    @Test void fillsMissingDatesWithoutInferringEmploymentStatus(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setEntryStatus(0));
        var r=writer.apply(1L,7L,Map.of("entryTime",LocalDate.of(2020,1,1)),null);
        assertEquals(1,r.updatedFields());assertTrue(r.conflicts().isEmpty());
        var c=org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(isNull(),c.capture());assertFalse(c.getValue().getSqlSet().contains("entry_status"));
    }
    @Test void preservesManualDatesAndReportsConflict(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setBirthday(LocalDateTime.of(1990,1,1,0,0)));
        var r=writer.apply(1L,7L,Map.of("birthday",LocalDate.of(1991,1,1)),null);
        assertEquals(0,r.updatedFields());assertEquals(1,r.conflicts().size());
        verify(employees,never()).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
    @Test void laterUpstreamChangesFollowLastAppliedSourceValue(){
        var old=LocalDateTime.of(1990,1,1,0,0);
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setBirthday(old));
        var state=new HrmEmployeeWecomSyncDO();state.setEmployeeId(1L);state.setBirthday(old);when(states.selectById(1L)).thenReturn(state);
        assertEquals(1,writer.apply(1L,7L,Map.of("birthday",LocalDate.of(1991,1,1)),null).updatedFields());
    }
    @Test void confirmationRechecksLocalValueBeforeOverwrite(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setBirthday(LocalDateTime.of(1992,1,1,0,0)));
        assertThrows(BizException.class,()->writer.apply(1L,7L,Map.of("birthday",LocalDate.of(1991,1,1)),Map.of("birthday","1990-01-01")));
        verify(employees,never()).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
