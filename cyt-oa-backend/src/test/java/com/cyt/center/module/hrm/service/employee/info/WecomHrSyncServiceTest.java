package com.cyt.center.module.hrm.service.employee.info;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.*;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.postgresql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.model.entity.SysUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WecomHrSyncServiceTest {
    final ObjectMapper json = new ObjectMapper();
    final WecomGatewayClient gateway = mock(WecomGatewayClient.class);
    final HrmEmployeeMapper employees = mock(HrmEmployeeMapper.class);
    final SysUserMapper users = mock(SysUserMapper.class);
    final WecomHrSyncWriter writer = mock(WecomHrSyncWriter.class);
    final WecomHrSyncService service = new WecomHrSyncService(gateway,employees,users,writer);

    void staffFields() throws Exception {
        when(gateway.hrFields()).thenReturn(json.readTree("""
                {"group_list":[{"field_list":[{"field_name":"员工状态","fieldid":904,"field_type":2,"value_type":3,
                "option_list":[{"id":88,"value":"试用"},{"id":99,"value":"已离职"}]}]}]}
                """));
    }
    HrmEmployeeDO employee(long id, Long userId) { return new HrmEmployeeDO().setId(id).setName("员工"+id).setUserId(userId); }
    SysUser user(long id,String userid) { var user=new SysUser();user.setId(id);user.setWecomUserid(userid);user.setDirectorySyncedAt(java.time.LocalDateTime.of(2026,11,1,0,0));return user; }
    void archives(List<HrmEmployeeDO> archives, List<SysUser> accounts) {
        when(employees.selectList(any(Wrapper.class))).thenReturn(archives);
        when(users.selectBatchIds(anyCollection())).thenReturn(accounts);
    }

    @Test void missingHistoricalAccountIsSkippedButPresentAccount60111IsAVisibleFailure() throws Exception {
        staffFields();
        archives(List.of(employee(1,11L),employee(2,12L),employee(3,13L),employee(4,null)),
                List.of(user(11,"HISTORICAL"),user(12,"CURRENT"),user(13,"TRIAL")));
        when(gateway.hrStaff(eq("HISTORICAL"),anyCollection())).thenThrow(new WecomGatewayException("upstream_error","trace-old","WECOM_60111","raw"));
        when(gateway.hrStaff(eq("CURRENT"),anyCollection())).thenThrow(new WecomGatewayException("upstream_error","trace-new","WECOM_60111","raw"));
        when(gateway.hrStaff(eq("TRIAL"),anyCollection())).thenReturn(json.readTree("{\"field_info\":[{\"fieldid\":904,\"result\":1,\"value_type\":3,\"value_uint32\":88}]}"));
        when(writer.applyProfile(eq(3L),eq(13L),any())).thenReturn(new WecomHrSyncWriter.Written(2,List.of()));
        var result=service.sync(Set.of("current","trial"));
        assertEquals(4,result.scanned());assertEquals(3,result.requested());
        assertEquals(1,result.succeeded());assertEquals(1,result.failed());assertEquals(2,result.skipped());
        assertEquals(result.scanned(),result.succeeded()+result.failed()+result.skipped());
        assertEquals(1,result.updatedEmployees());assertEquals(2,result.updatedFields());
        assertEquals("trace-new",result.failures().getFirst().traceId());
        assertTrue(result.skippedEmployees().stream().anyMatch(row -> row.employeeId()==1L && row.code().equals("WECOM_60111")));
        verify(writer,never()).applyProfile(eq(1L),anyLong(),any());
        verify(writer,never()).applyProfile(eq(2L),anyLong(),any());
    }

    @Test void historicalEmployeeStillReadableFromHrUsesActualLeftState() throws Exception {
        staffFields();archives(List.of(employee(1,11L)),List.of(user(11,"historical")));
        when(gateway.hrStaff(eq("historical"),anyCollection())).thenReturn(json.readTree("{\"field_info\":[{\"fieldid\":904,\"result\":1,\"value_type\":3,\"value_uint32\":99}]}"));
        when(writer.applyProfile(eq(1L),eq(11L),any())).thenReturn(new WecomHrSyncWriter.Written(1,List.of()));
        var result=service.sync(Set.of());
        assertEquals(1,result.succeeded());assertEquals(0,result.skipped());assertEquals(1,result.requested());
        var profile=ArgumentCaptor.forClass(WecomHrDates.Profile.class);
        verify(writer).applyProfile(eq(1L),eq(11L),profile.capture());
        assertEquals(Map.of("entryStatus",4),profile.getValue().values());
    }

    @Test void unlinkedLocalRecordsAreCountedAndPreservedWithoutCallingHr() {
        archives(List.of(employee(1,null),employee(2,12L)),List.of(user(12,null)));
        var result=service.sync(Set.of());
        assertEquals(2,result.scanned());assertEquals(0,result.requested());assertEquals(2,result.skipped());
        verifyNoInteractions(gateway);verify(writer,never()).applyProfile(anyLong(),anyLong(),any());
    }

    @Test void unchangedReadableEmployeeCountsAsSucceededWithoutInventingChanges() throws Exception {
        staffFields();archives(List.of(employee(1,11L)),List.of(user(11,"trial")));
        when(gateway.hrStaff(anyString(),anyCollection())).thenReturn(json.readTree("{\"field_info\":[{\"fieldid\":904,\"result\":1,\"value_type\":3,\"value_uint32\":88}]}"));
        when(writer.applyProfile(anyLong(),anyLong(),any())).thenReturn(new WecomHrSyncWriter.Written(0,List.of()));
        var result=service.sync(Set.of("TRIAL"));
        assertEquals(1,result.succeeded());assertEquals(1,result.unchanged());assertEquals(0,result.updatedEmployees());assertEquals(0,result.updatedFields());
    }

    @Test void noSupportedConfigurationStopsBeforeAnyEmployeeRead() throws Exception {
        archives(List.of(employee(1,11L)),List.of(user(11,"trial")));
        when(gateway.hrFields()).thenReturn(json.readTree("{\"group_list\":[{\"field_list\":[]}]}"));
        assertThrows(BizException.class,()->service.sync(Set.of("trial")));
        verify(gateway,never()).hrStaff(anyString(),anyCollection());
    }

    @Test void allFieldReadsUnavailableAreAFailureAndDoNotOverwriteEmployee() throws Exception {
        staffFields();archives(List.of(employee(1,11L)),List.of(user(11,"trial")));
        when(gateway.hrStaff(anyString(),anyCollection())).thenReturn(json.readTree("{\"field_info\":[{\"fieldid\":904,\"result\":2}]}"));
        var result=service.sync(Set.of("trial"));
        assertEquals(1,result.failed());assertEquals(0,result.succeeded());assertEquals(0,result.updatedFields());
        assertTrue(result.conflicts().stream().anyMatch(row -> row.field().equals("employeeStatus")));
        verify(writer,never()).applyProfile(anyLong(),anyLong(),any());
    }

    @Test void unconfirmedHistoricalDemoIdentityIsSkippedBeforeReadingEvenIfUseridExists() {
        var account=user(11,"same-name-demo");account.setDirectorySyncedAt(null);
        archives(List.of(employee(1,11L)),List.of(account));
        var result=service.sync(Set.of("real-member"));
        assertEquals(1,result.scanned());assertEquals(0,result.requested());assertEquals(1,result.skipped());
        assertTrue(result.skippedEmployees().getFirst().reason().contains("尚未经通讯录确认"));
        verifyNoInteractions(gateway);
    }

    @Test void currentDirectoryPresenceConfirmsIdentityEvenWithoutPreviousTimestamp() throws Exception {
        staffFields();var account=user(11,"CuRrEnT");account.setDirectorySyncedAt(null);
        archives(List.of(employee(1,11L)),List.of(account));
        when(gateway.hrStaff(anyString(),anyCollection())).thenReturn(json.readTree("{\"field_info\":[{\"fieldid\":904,\"result\":1,\"value_type\":3,\"value_uint32\":88}]}"));
        when(writer.applyProfile(anyLong(),anyLong(),any())).thenReturn(new WecomHrSyncWriter.Written(2,List.of()));
        var result=service.sync(Set.of("current"));
        assertEquals(1,result.requested());assertEquals(1,result.succeeded());assertEquals(0,result.skipped());
        var profile=ArgumentCaptor.forClass(WecomHrDates.Profile.class);
        verify(writer).applyProfile(eq(1L),eq(11L),profile.capture());
        assertEquals(Map.of("entryStatus",1,"type",1,"status",2),profile.getValue().values());
    }

    @Test void quotaSpacingIsSharedAcrossConsecutiveRuns() throws Exception {
        staffFields();archives(List.of(employee(1,11L)),List.of(user(11,"trial")));
        List<Long> sent=Collections.synchronizedList(new ArrayList<>());
        var response=json.readTree("{\"field_info\":[{\"fieldid\":904,\"result\":1,\"value_type\":3,\"value_uint32\":88}]}");
        when(gateway.hrStaff(anyString(),anyCollection())).thenAnswer(invocation->{sent.add(System.nanoTime());return response;});
        when(writer.applyProfile(anyLong(),anyLong(),any())).thenReturn(new WecomHrSyncWriter.Written(0,List.of()));
        service.sync(Set.of("trial"));service.sync(Set.of("trial"));
        assertEquals(2,sent.size());
        assertTrue(sent.get(1)-sent.get(0)>=java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(225),"连续同步应共享250ms配额预留");
    }

    @Test void readableBirthdayIsWrittenEvenWhenEmploymentOptionCannotBeMapped() throws Exception {
        archives(List.of(employee(1,11L)),List.of(user(11,"trial")));
        when(gateway.hrFields()).thenReturn(json.readTree("""
                {"group_list":[{"field_list":[
                 {"field_name":"生日","fieldid":7,"field_type":3,"value_type":4},
                 {"field_name":"员工状态","fieldid":904,"field_type":2,"value_type":3,"option_list":[{"id":88,"value":"未适配状态"}]}
                ]}]}
                """));
        when(gateway.hrStaff(anyString(),anyCollection())).thenReturn(json.readTree("""
                {"field_info":[{"fieldid":7,"result":1,"value_type":4,"value_int64":null},
                {"fieldid":904,"result":1,"value_type":3,"value_uint32":88}]}
                """));
        when(writer.applyProfile(anyLong(),anyLong(),any())).thenReturn(new WecomHrSyncWriter.Written(1,List.of()));
        var result=service.sync(Set.of("trial"));
        assertEquals(1,result.succeeded());assertEquals(0,result.failed());
        assertTrue(result.conflicts().stream().anyMatch(row->row.field().equals("employeeStatus") && row.reason().contains("映射")));
        var profile=ArgumentCaptor.forClass(WecomHrDates.Profile.class);
        verify(writer).applyProfile(anyLong(),anyLong(),profile.capture());
        assertTrue(profile.getValue().values().containsKey("birthday"));
        assertFalse(profile.getValue().values().containsKey("entryStatus"));
    }

    @Test void overFiveHundredActualIdentitiesMustBeSplitBeforeAnyHrRead() {
        List<HrmEmployeeDO> archives=new ArrayList<>();List<SysUser> accounts=new ArrayList<>();Set<String> present=new HashSet<>();
        for(long id=1;id<=501;id++){archives.add(employee(id,id));accounts.add(user(id,"member"+id));present.add("member"+id);}
        archives(archives,accounts);
        assertTrue(assertThrows(BizException.class,()->service.sync(present)).getMessage().contains("500"));
        verifyNoInteractions(gateway);
    }

    @Test void legacyConfirmationCannotBypassUnconfirmedIdentityProtection() {
        var account=user(11,"same-name-demo");account.setDirectorySyncedAt(null);
        when(employees.selectById(1L)).thenReturn(employee(1,11L));
        when(users.selectById(11L)).thenReturn(account);
        var error=assertThrows(BizException.class,()->service.confirm(1L,"birthday",null,"1990-01-01"));
        assertTrue(error.getMessage().contains("企微身份尚未确认"));
        verifyNoInteractions(gateway);
        verify(writer).requireSchema();
        verifyNoMoreInteractions(writer);
    }
}
