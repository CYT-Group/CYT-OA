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
    final SysUserDeptMapper bindings = mock(SysUserDeptMapper.class);
    final SysUserService accounts = mock(SysUserService.class);
    final DirectorySnapshotWriter writer = new DirectorySnapshotWriter(users, depts,
            bindings, accounts, new CenterProperties(), archives);
    final ObjectMapper json = new ObjectMapper();

    @org.junit.jupiter.api.BeforeAll static void mapping() {
        for (Class<?> entity : List.of(SysUser.class, SysUserDept.class, SysDept.class))
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                    new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"), entity);
    }

    @org.junit.jupiter.api.BeforeEach void beginTransaction() { TransactionSynchronizationManager.initSynchronization(); }
    @org.junit.jupiter.api.AfterEach void endTransaction() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test void syncDoesNotRestoreDisabledAccountsAndFallsBackToFirstDepartment() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setUsername("alice"); old.setWecomUserid("alice"); old.setEnabled(false);
        when(users.selectList(any())).thenReturn(List.of(old)); when(depts.selectList(any())).thenReturn(List.of());
        writer.apply(json.readTree(snapshot("[{\"userid\":\"alice\",\"name\":\"Alice\",\"status\":1,\"department_ids\":[1]}]")));
        var changes = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(archives).syncIdentities(changes.capture());
        SysUser change = (SysUser) changes.getValue().getFirst();
        assertFalse(change.getEnabled()); assertEquals(1L, change.getMainDeptId());
    }

    /** uk_sys_user_wecom 是 utf8mb4_0900_ai_ci：企微把 zhangwei 写成 ZhangWei 时必须是更新，不能插第二条同名账号。 */
    @Test void useridCaseChangeUpdatesTheExistingAccountInsteadOfInsertingADuplicate() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setUsername("admin"); old.setWecomUserid("zhangwei"); old.setEnabled(true);
        when(users.selectList(any())).thenReturn(List.of(old)); when(depts.selectList(any())).thenReturn(List.of());
        var result = writer.apply(json.readTree(snapshot("[{\"userid\":\"ZhangWei\",\"name\":\"张蔚\",\"status\":1,\"department_ids\":[1]}]")));
        var changes = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(archives).syncIdentities(changes.capture()); verify(users, never()).insert(any(SysUser.class));
        SysUser change = (SysUser) changes.getValue().getFirst();
        assertEquals(7L, change.getId()); assertEquals("ZhangWei", change.getWecomUserid());
        assertTrue(result.missing().isEmpty());
        assertTrue(result.conflicts().isEmpty(), "同人改大小写不算冲突账号");
    }

    @Test void explicitMainDepartmentMustBelongToMembership() throws Exception {
        when(users.selectList(any())).thenReturn(List.of());
        assertThrows(BizException.class, () -> writer.apply(json.readTree(snapshot("[{\"userid\":\"alice\",\"name\":\"Alice\",\"status\":1,\"main_department\":2,\"department_ids\":[1]}]"))));
        verify(users, never()).insert(any(SysUser.class)); verifyNoInteractions(archives);
    }

    @Test void missingMemberIsDeletedEvenWhenNoMembershipRemains() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setWecomUserid("missing"); old.setEnabled(true);
        when(users.selectList(any())).thenReturn(List.of(old)); when(depts.selectList(any())).thenReturn(List.of());
        var result = writer.apply(json.readTree(snapshot("[]")));
        assertEquals(List.of("missing"), result.missing());
        verify(archives).deleteByUserIds(List.of(7L));
        verify(accounts).deleteDirectoryUsers(List.of(7L));
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
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

    @Test void unchangedMembershipsAvoidDeleteAndInsert() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setWecomUserid("alice"); old.setEnabled(true);
        SysUserDept binding = new SysUserDept(); binding.setUserId(7L); binding.setDeptId(1L);
        when(users.selectList(any())).thenReturn(List.of(old));
        when(bindings.selectList(any())).thenReturn(List.of(binding));
        writer.apply(json.readTree(snapshot("[{\"userid\":\"alice\",\"name\":\"Alice\",\"status\":1,\"department_ids\":[1]}]")));
        verify(bindings, never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(bindings, never()).insertBindings(anyList());
    }

    @Test void latestSnapshotClearsOldMembershipAndMainDepartment() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setWecomUserid("alice"); old.setEnabled(true); old.setMainDeptId(1L);
        SysUserDept binding = new SysUserDept(); binding.setUserId(7L); binding.setDeptId(1L);
        when(users.selectList(any())).thenReturn(List.of(old)); when(bindings.selectList(any())).thenReturn(List.of(binding));
        writer.apply(json.readTree(snapshot("[{\"userid\":\"alice\",\"name\":\"Alice\",\"status\":1,\"department_ids\":[]}]")));
        var change = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(users).update(isNull(), change.capture());
        assertTrue(change.getValue().getSqlSet().contains("main_dept_id"));
        assertTrue(change.getValue().getParamNameValuePairs().containsValue(null));
        verify(bindings).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(bindings, never()).insertBindings(anyList());
    }

    @Test void missingMemberRemovesMembershipAccountArchiveAndLeaderReference() throws Exception {
        SysUser old = new SysUser(); old.setId(7L); old.setWecomUserid("missing"); old.setEnabled(true);
        SysUserDept binding = new SysUserDept(); binding.setUserId(7L); binding.setDeptId(1L);
        when(users.selectList(any())).thenReturn(List.of(old)); when(bindings.selectList(any())).thenReturn(List.of(binding));
        assertEquals(List.of("missing"), writer.apply(json.readTree(snapshot("[]"))).missing());
        verify(bindings).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(users, never()).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(archives).deleteByUserIds(List.of(7L));
        verify(accounts).deleteDirectoryUsers(List.of(7L));
        var leaderChange = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(depts).update(isNull(), leaderChange.capture());
        assertTrue(leaderChange.getValue().getSqlSet().contains("leader_userid"));
        assertTrue(leaderChange.getValue().getParamNameValuePairs().containsValue(null));
    }

    @Test void unrelatedLocalAccountsAreNotDeleted() throws Exception {
        SysUser local = new SysUser(); local.setId(1L); local.setUsername("admin"); local.setEnabled(true);
        SysUser blank = new SysUser(); blank.setId(2L); blank.setWecomUserid("");
        when(users.selectList(any())).thenReturn(List.of(local, blank));
        assertTrue(writer.apply(json.readTree(snapshot("[]"))).missing().isEmpty());
        verify(accounts, never()).deleteDirectoryUsers(anyList());
        verify(archives, never()).deleteByUserIds(anyList());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test void malformedSnapshotCannotTriggerDeletion() throws Exception {
        var data = json.readTree(snapshot("[]"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) data.path("counts")).put("users", 99);
        assertThrows(BizException.class, () -> writer.apply(data));
        verifyNoInteractions(users, accounts, archives, bindings);
    }

    String snapshot(String users) throws Exception {
        return "{\"departments\":[{\"id\":1,\"parent_id\":0,\"name\":\"Company\",\"path\":\"Company\"}],\"users\":"+users+",\"counts\":{\"users\":"+json.readTree(users).size()+",\"departments\":1}}";
    }
}
