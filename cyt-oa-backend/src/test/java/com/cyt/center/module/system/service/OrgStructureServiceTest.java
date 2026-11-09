package com.cyt.center.module.system.service;

import com.cyt.center.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.cyt.center.module.hrm.dal.mysql.employee.info.HrmEmployeeMapper;
import com.cyt.center.module.system.mapper.*;
import com.cyt.center.module.system.model.entity.SysDept;
import com.cyt.center.module.system.model.entity.SysUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrgStructureServiceTest {
    @Test void directoryResponseExcludesPrivateArchiveFieldsEvenWhenStored() throws Exception {
        var users=mock(SysUserMapper.class);var employees=mock(HrmEmployeeMapper.class);
        var memberships=mock(SysUserDeptMapper.class);
        SysUser user=new SysUser();user.setId(7L);user.setName("Alice");user.setEnabled(false);
        when(users.selectById(7L)).thenReturn(user);when(memberships.selectList(any())).thenReturn(List.of());
        when(employees.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(new HrmEmployeeDO().setId(42L).setUserId(7L)
                .setIdNumber("private-id").setAddress("private-address").setBirthday(LocalDateTime.of(1990,1,1,0,0))
                .setLeaderEmployeeId(99L).setEntryStatus(1)));
        when(employees.selectById(99L)).thenReturn(new HrmEmployeeDO().setId(99L).setName("Manager"));
        var deptMapper=mock(SysDeptMapper.class);
        var service=new OrgStructureService(deptMapper,users,memberships,employees,mock(SysUserService.class),new DeptNamePathResolver(deptMapper));
        var result=service.detail(7L);
        assertEquals(42L,result.getEmployeeId());assertEquals("Manager",result.getManagerName());
        assertEquals(1,result.getEntryStatus());assertFalse(result.getEnabled());
        String body=new ObjectMapper().writeValueAsString(result);
        assertFalse(body.contains("idNumber"));assertFalse(body.contains("birthday"));assertFalse(body.contains("address"));assertFalse(body.contains("private-"));
    }

    /** 部门人数改走 GROUP BY 聚合：不能为了计数把 sys_user_dept 整张表拉回内存。 */
    @Test void departmentTreeUsesAggregatedMemberCounts() {
        var deptMapper = mock(SysDeptMapper.class);
        var memberships = mock(SysUserDeptMapper.class);
        var accounts = mock(SysUserService.class);
        SysDept root = new SysDept(); root.setId(1L); root.setParentId(0L); root.setName("Company"); root.setSortOrder(0L);
        SysDept child = new SysDept(); child.setId(2L); child.setParentId(1L); child.setName("Market"); child.setSortOrder(1L);
        when(deptMapper.selectList(any())).thenReturn(List.of(root, child));
        when(memberships.countByDept()).thenReturn(List.of(
                Map.of("dept_id", 1L, "member_count", 3L),
                Map.of("dept_id", 2L, "member_count", 7L)));
        // 不可变 Map 的 get(null) 会抛 NPE，这里给可变 Map 以匹配真实服务返回。
        when(accounts.namesByWecomUserids(any())).thenReturn(new HashMap<>());
        var service = new OrgStructureService(deptMapper, mock(SysUserMapper.class), memberships,
                mock(HrmEmployeeMapper.class), accounts, new DeptNamePathResolver(deptMapper));

        var tree = service.tree();
        assertEquals(2, tree.size());
        assertEquals(3L, tree.get(0).memberCount());
        assertEquals(7L, tree.get(1).memberCount());
        assertEquals("Company/Market", tree.get(1).namePath());
        verify(memberships, never()).selectList(any());
        verify(memberships).countByDept();
    }
}
