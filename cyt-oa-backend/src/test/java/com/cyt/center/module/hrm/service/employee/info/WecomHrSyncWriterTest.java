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
    final com.cyt.center.module.hrm.dal.mysql.employee.employment.HrmEmployeeChangeRecordMapper changes=mock(com.cyt.center.module.hrm.dal.mysql.employee.employment.HrmEmployeeChangeRecordMapper.class);
    final WecomHrSyncWriter writer=new WecomHrSyncWriter(employees,states,changes);
    @BeforeEach void updates(){when(employees.update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(1);}
    @BeforeAll static void mapping(){com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"test"),HrmEmployeeDO.class);com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"state"),HrmEmployeeWecomSyncDO.class);}
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
    @Test void latestUpstreamDateOverwritesManualDate(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setBirthday(LocalDateTime.of(1990,1,1,0,0)));
        var r=writer.apply(1L,7L,Map.of("birthday",LocalDate.of(1991,1,1)),null);
        assertEquals(1,r.updatedFields());assertTrue(r.conflicts().isEmpty());
        verify(employees).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
    @Test void laterUpstreamChangesFollowLastAppliedSourceValue(){
        var old=LocalDateTime.of(1990,1,1,0,0);
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setBirthday(old));
        var state=new HrmEmployeeWecomSyncDO();state.setEmployeeId(1L);state.setBirthday(old);when(states.selectById(1L)).thenReturn(state);
        assertEquals(1,writer.apply(1L,7L,Map.of("birthday",LocalDate.of(1991,1,1)),null).updatedFields());
    }
    @Test void explicitEmptyClearsDateButMissingFieldPreservesIt() {
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L)
                .setBirthday(LocalDateTime.of(1990,1,1,0,0)).setEntryTime(LocalDateTime.of(2020,1,1,0,0)));
        Map<String, LocalDate> dates = new HashMap<>(); dates.put("birthday", null);
        assertEquals(1, writer.apply(1L, 7L, dates, null).updatedFields());
        var change = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(isNull(), change.capture());
        assertTrue(change.getValue().getSqlSet().contains("birthday"));
        assertFalse(change.getValue().getSqlSet().contains("entry_time"));
        assertTrue(change.getValue().getParamNameValuePairs().containsValue(null));
    }
    @Test void confirmationRechecksLocalValueBeforeOverwrite(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setBirthday(LocalDateTime.of(1992,1,1,0,0)));
        assertThrows(BizException.class,()->writer.apply(1L,7L,Map.of("birthday",LocalDate.of(1991,1,1)),Map.of("birthday","1990-01-01")));
        verify(employees,never()).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test void writesRealHrStateProbationAndFutureRegularDateInOneEmployeePatch(){
        var employee=new HrmEmployeeDO().setId(1L).setUserId(7L).setEntryStatus(0).setStatus(1).setProbation(0);
        when(employees.selectByIdForUpdate(1L)).thenReturn(employee);
        var profile=new com.cyt.center.core.integration.WecomHrDates.Profile(
                Map.of("entryStatus",1,"type",1,"status",2,"probation",3,"regularTime",LocalDate.of(2027,2,1)),List.of(),3);
        var result=writer.applyProfile(1L,7L,profile);
        assertEquals(5,result.updatedFields());
        var change=org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(isNull(),change.capture());
        assertTrue(change.getValue().getSqlSet().contains("entry_status"));
        assertTrue(change.getValue().getSqlSet().contains("type"));
        assertTrue(change.getValue().getParamNameValuePairs().containsValue(1));
        assertTrue(change.getValue().getSqlSet().contains("status"));
        assertTrue(change.getValue().getSqlSet().contains("probation"));
        assertTrue(change.getValue().getSqlSet().contains("regular_time"));
        assertFalse(change.getValue().getSqlSet().contains("birthday"));
        var audit=org.mockito.ArgumentCaptor.forClass(com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO.class);
        verify(changes).insert(audit.capture());assertEquals(11,audit.getValue().getType());
        assertTrue(audit.getValue().getRemark().contains("转正日期"));
        assertFalse(audit.getValue().getRemark().contains("2027"));
    }

    @Test void repeatedEquivalentProfileDoesNotUpdateEmployeeOrAddAudit(){
        var date=LocalDateTime.of(2027,2,1,0,0);
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L)
                .setEntryStatus(1).setType(1).setStatus(2).setProbation(3).setRegularTime(date));
        var profile=new com.cyt.center.core.integration.WecomHrDates.Profile(
                Map.of("entryStatus",1,"type",1,"status",2,"probation",3,"regularTime",date.toLocalDate()),List.of(),3);
        assertEquals(0,writer.applyProfile(1L,7L,profile).updatedFields());
        verify(employees,never()).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verifyNoInteractions(changes);
    }

    @Test void explicitFormalEmploymentCorrectsIncompatibleOldEmployeeType(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L)
                .setEntryStatus(1).setType(2).setStatus(2));
        var profile=new com.cyt.center.core.integration.WecomHrDates.Profile(
                Map.of("entryStatus",1,"type",1,"status",2),List.of(),1);
        assertEquals(1,writer.applyProfile(1L,7L,profile).updatedFields());
        var change=org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(isNull(),change.capture());
        assertTrue(change.getValue().getSqlSet().contains("type"));
        assertFalse(change.getValue().getSqlSet().contains("status"));
    }

    @Test void missingHrStateDoesNotOverwriteItAndExplicitNullCanClearOnlyTheDate(){
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L)
                .setEntryStatus(3).setStatus(2).setProbation(3).setRegularTime(LocalDateTime.of(2027,2,1,0,0)));
        Map<String,Object> values=new HashMap<>();values.put("regularTime",null);
        var result=writer.applyProfile(1L,7L,new com.cyt.center.core.integration.WecomHrDates.Profile(values,List.of(),1));
        assertEquals(1,result.updatedFields());
        var change=org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(isNull(),change.capture());
        assertTrue(change.getValue().getSqlSet().contains("regular_time"));
        assertFalse(change.getValue().getSqlSet().contains("entry_status"));
        assertFalse(change.getValue().getSqlSet().contains("probation"));
    }

    @Test void employmentWritesAndAuditUseSameTransactionalBoundary() throws Exception {
        var method=WecomHrSyncWriter.class.getMethod("applyProfile",Long.class,Long.class,com.cyt.center.core.integration.WecomHrDates.Profile.class);
        var transactional=method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertNotNull(transactional);assertTrue(List.of(transactional.rollbackFor()).contains(Exception.class));
        when(employees.selectByIdForUpdate(1L)).thenReturn(new HrmEmployeeDO().setId(1L).setUserId(7L).setEntryStatus(1));
        when(changes.insert(any(com.cyt.center.module.hrm.dal.dataobject.employee.employment.HrmEmployeeChangeRecordDO.class))).thenThrow(new IllegalStateException("audit unavailable"));
        assertThrows(IllegalStateException.class,()->writer.applyProfile(1L,7L,
                new com.cyt.center.core.integration.WecomHrDates.Profile(Map.of("entryStatus",4),List.of(),1)));
        verify(states,never()).insert(any(HrmEmployeeWecomSyncDO.class));
    }
}
