package com.cyt.center.module.system.service;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.mapper.*;
import com.cyt.center.module.system.model.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DirectorySnapshotWriterTest {
    final SysUserMapper users = mock(SysUserMapper.class);
    final SysDeptMapper depts = mock(SysDeptMapper.class);
    final EmployeeDirectoryArchiveService archives = mock(EmployeeDirectoryArchiveService.class);
    final DirectorySnapshotWriter writer = new DirectorySnapshotWriter(users, depts,
            mock(SysUserDeptMapper.class), mock(SysUserService.class), new CenterProperties(), archives);
    final ObjectMapper json = new ObjectMapper();

    @Test void syncDoesNotRestoreDisabledAccountsAndFallsBackToFirstDepartment() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setUsername("alice"); old.setWecomUserid("alice"); old.setEnabled(false);
        when(users.selectList(any())).thenReturn(List.of(old)); when(depts.selectList(any())).thenReturn(List.of());
        TransactionSynchronizationManager.initSynchronization();
        try {
            writer.apply(json.readTree(snapshot("[{\"userid\":\"alice\",\"name\":\"Alice\",\"status\":1,\"department_ids\":[1]}]")));
            var change = org.mockito.ArgumentCaptor.forClass(SysUser.class);
            verify(users).updateById(change.capture());
            assertFalse(change.getValue().getEnabled()); assertEquals(1L, change.getValue().getMainDeptId());
            verify(archives).syncIdentity(change.getValue());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    /** uk_sys_user_wecom 是 utf8mb4_0900_ai_ci：企微把 zhangwei 写成 ZhangWei 时必须是更新，不能插第二条同名账号。 */
    @Test void useridCaseChangeUpdatesTheExistingAccountInsteadOfInsertingADuplicate() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setUsername("admin"); old.setWecomUserid("zhangwei"); old.setEnabled(true);
        when(users.selectList(any())).thenReturn(List.of(old)); when(depts.selectList(any())).thenReturn(List.of());
        var result = writer.apply(json.readTree(snapshot("[{\"userid\":\"ZhangWei\",\"name\":\"张蔚\",\"status\":1,\"department_ids\":[1]}]")));
        var change = org.mockito.ArgumentCaptor.forClass(SysUser.class);
        verify(users).updateById(change.capture()); verify(users, never()).insert(any(SysUser.class));
        assertEquals(7L, change.getValue().getId()); assertEquals("ZhangWei", change.getValue().getWecomUserid());
        assertTrue(result.conflicts().isEmpty(), "同人改大小写不算冲突账号");
    }

    @Test void explicitMainDepartmentMustBelongToMembership() throws Exception {
        when(users.selectList(any())).thenReturn(List.of());
        assertThrows(BizException.class, () -> writer.apply(json.readTree(snapshot("[{\"userid\":\"alice\",\"name\":\"Alice\",\"status\":1,\"main_department\":2,\"department_ids\":[1]}]"))));
        verify(users, never()).insert(any(SysUser.class)); verifyNoInteractions(archives);
    }

    @Test void missingMemberIsReportedWithoutDeletingOrDisablingTheirHistory() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setWecomUserid("missing"); old.setEnabled(true);
        when(users.selectList(any())).thenReturn(List.of(old)); when(depts.selectList(any())).thenReturn(List.of());
        var result = writer.apply(json.readTree(snapshot("[]")));
        assertEquals(List.of("missing"),result.missing()); verify(users,never()).updateById(any(SysUser.class)); verifyNoInteractions(archives);
    }

    /** 已经全表读过部门，就不该再为每个部门单点查一次存在性。 */
    @Test void existingDepartmentsAreUpdatedWithoutPerDepartmentLookup() throws Exception {
        SysDept old = new SysDept(); old.setId(1L); old.setParentId(0L); old.setName("Company");
        when(users.selectList(any())).thenReturn(List.of());
        when(depts.selectList(any())).thenReturn(List.of(old));
        writer.apply(json.readTree(snapshot("[]")));
        verify(depts, never()).selectById(any());
        verify(depts, never()).insert(any(SysDept.class));
        verify(depts).updateById(any(SysDept.class));
    }

    @Test void cyclicDepartmentsAreRejectedBeforeWriting() throws Exception {
        var data = json.readTree("{\"departments\":[{\"id\":1,\"parent_id\":1,\"name\":\"A\",\"path\":\"A\"}],\"users\":[],\"counts\":{\"users\":0,\"departments\":1}}");
        assertThrows(BizException.class, () -> writer.apply(data)); verifyNoInteractions(users,archives);
    }

    String snapshot(String users) throws Exception {
        return "{\"departments\":[{\"id\":1,\"parent_id\":0,\"name\":\"Company\",\"path\":\"Company\"}],\"users\":"+users+",\"counts\":{\"users\":"+json.readTree(users).size()+",\"departments\":1}}";
    }
}
