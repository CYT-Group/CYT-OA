package com.cyt.center.module.system.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.mapper.*;
import com.cyt.center.module.system.model.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SysUserStatusTest {
    final SysUserMapper users=mock(SysUserMapper.class);
    final SysUserRoleMapper roles=mock(SysUserRoleMapper.class);
    final SysUserService service=new SysUserService(users,roles,new DeptNamePathResolver(mock(SysDeptMapper.class)),mock(SysRoleService.class));
    @Test void cannotDisableSelfOrAdministrator() {
        SysUser user=new SysUser();user.setId(7L);when(users.selectById(7L)).thenReturn(user);
        assertThrows(BizException.class,()->service.changeStatus(7L,false,7L));
        when(roles.selectRoleCodes(7L)).thenReturn(List.of("admin"));
        assertThrows(BizException.class,()->service.changeStatus(7L,false,1L));
        verify(users,never()).updateById(any(SysUser.class));
    }
    @Test void cannotRestoreInactiveWecomIdentity() {
        SysUser user=new SysUser();user.setId(7L);user.setWecomUserid("alice");user.setWecomStatus(2);
        when(users.selectById(7L)).thenReturn(user);
        assertThrows(BizException.class,()->service.changeStatus(7L,true,1L));
        verify(users,never()).updateById(any(SysUser.class));
    }
    @Test void explicitRestoreUpdatesOnlyLoginStatusAndInvalidatesAfterCommit() {
        SysUser user=new SysUser();user.setId(7L);user.setWecomUserid("alice");user.setWecomStatus(1);
        when(users.selectById(7L)).thenReturn(user);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.changeStatus(7L,true,1L);
            var change=org.mockito.ArgumentCaptor.forClass(SysUser.class);verify(users).updateById(change.capture());
            assertTrue(change.getValue().getEnabled());assertNull(change.getValue().getWecomUserid());assertNull(change.getValue().getName());
            assertEquals(1,TransactionSynchronizationManager.getSynchronizations().size());
        } finally {TransactionSynchronizationManager.clearSynchronization();}
    }
}
