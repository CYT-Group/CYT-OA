package com.cyt.center.module.system.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.postgresql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.hrm.dal.postgresql.employee.employment.HrmEmployeeChangeRecordMapper;
import com.cyt.center.module.system.model.entity.SysUser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeDirectoryArchiveServiceTest {
    @org.junit.jupiter.api.BeforeAll static void initializeMapping() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"), HrmEmployeeDO.class);
    }
    final HrmEmployeeMapper employees = mock(HrmEmployeeMapper.class);
    final HrmEmployeeChangeRecordMapper changes = mock(HrmEmployeeChangeRecordMapper.class);
    final EmployeeDirectoryArchiveService service = new EmployeeDirectoryArchiveService(employees,changes);
    @Test void createsAnIndependentArchiveWithoutInventingEmploymentDetails() {
        when(employees.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        SysUser user=new SysUser();user.setId(7L);user.setName("Alice");user.setMainDeptId(1L);user.setEnabled(true);
        service.syncIdentity(user);
        var capture=org.mockito.ArgumentCaptor.forClass(HrmEmployeeDO.class);verify(employees).insert(capture.capture());
        HrmEmployeeDO archive=capture.getValue();assertEquals(7L,archive.getUserId());assertEquals(0,archive.getEntryStatus());
        assertNull(archive.getEntryTime());assertNull(archive.getBirthday());assertNull(archive.getType());assertNull(archive.getStatus());
        verifyNoInteractions(changes);
    }
    @Test void refusesAmbiguousArchiveLinksInsteadOfChoosingOne() {
        when(employees.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(new HrmEmployeeDO(),new HrmEmployeeDO()));
        SysUser user=new SysUser();user.setId(7L);
        assertThrows(BizException.class,()->service.syncIdentity(user));verifyNoInteractions(changes);
        verify(employees,never()).insert(any(HrmEmployeeDO.class));
    }
    @Test void repeatedSyncKeepsArchiveIdentityAndDoesNotOverwriteHrFields() {
        when(employees.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(new HrmEmployeeDO().setId(42L).setDeptId(1L).setPostName("Engineer")
                        .setEntryStatus(4).setEntryTime(java.time.LocalDateTime.of(2020,1,1,0,0))));
        SysUser user=new SysUser();user.setId(7L);user.setName("Alice");user.setMainDeptId(1L);user.setPosition("Engineer");
        service.syncIdentity(user);
        var capture=org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(employees).update(isNull(),capture.capture());
        String columns=capture.getValue().getSqlSet();
        assertTrue(columns.contains("dept_id"));assertFalse(columns.contains("entry_time"));
        assertFalse(columns.contains("entry_status"));assertFalse(columns.contains("birthday"));assertFalse(columns.contains("leader_employee_id"));
        verify(employees,never()).insert(any(HrmEmployeeDO.class));verifyNoInteractions(changes);
    }
    @Test void batchReadsOnceAndSkipsUnchangedArchives() {
        when(employees.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(new HrmEmployeeDO().setId(42L).setUserId(7L).setName("Alice")));
        SysUser alice = new SysUser(); alice.setId(7L); alice.setName("Alice");
        SysUser bob = new SysUser(); bob.setId(8L); bob.setName("Bob");
        service.syncIdentities(List.of(alice, bob));
        verify(employees, times(1)).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(employees, never()).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        var inserted = org.mockito.ArgumentCaptor.forClass(HrmEmployeeDO.class);
        verify(employees).insert(inserted.capture()); assertEquals(8L, inserted.getValue().getUserId());
        verifyNoInteractions(changes);
    }
    @Test void emptySnapshotIdentityListDoesNotTouchHistoricalArchives() {
        service.syncIdentities(List.of());
        verifyNoInteractions(employees, changes);
    }
    @Test void disabledDirectoryIdentityDoesNotOverwriteHistoricalOffboarding() {
        var old=new HrmEmployeeDO().setId(42L).setUserId(7L).setName("Alice").setEntryStatus(4)
                .setLeaveTime(java.time.LocalDateTime.of(2020,1,1,0,0));
        when(employees.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(old));
        SysUser user=new SysUser();user.setId(7L);user.setName("Alice");user.setEnabled(false);user.setWecomStatus(5);
        service.syncIdentity(user);
        verify(employees,never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(employees,never()).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verifyNoInteractions(changes);assertEquals(4,old.getEntryStatus());assertEquals(java.time.LocalDateTime.of(2020,1,1,0,0),old.getLeaveTime());
    }
}
