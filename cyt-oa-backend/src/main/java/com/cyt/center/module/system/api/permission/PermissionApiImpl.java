package com.cyt.center.module.system.api.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import com.cyt.center.module.system.mapper.SysUserDeptMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.mapper.SysUserRoleMapper;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.model.entity.SysUserDept;
import com.cyt.center.module.system.model.entity.SysUserRole;
import com.cyt.center.module.system.service.PrincipalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限 API 实现类：权限串来自 sys_menu / sys_role_menu，角色来自 sys_user_role，
 * 判定口径与 Principal 完全一致，避免出现第二套鉴权规则。
 *
 * @author 芋道源码
 */
@Service
@RequiredArgsConstructor
public class PermissionApiImpl implements PermissionApi {

    private final PrincipalService principalService;
    private final SysUserMapper users;
    private final SysUserRoleMapper userRoles;
    private final SysUserDeptMapper userDepartments;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        Principal principal = principalService.load(userId);
        if (principal == null || permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank() && principal.has(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        if (userId == null || roles == null || roles.length == 0) {
            return false;
        }
        List<String> owned = userRoles.selectRoleCodes(userId);
        for (String role : roles) {
            if (role != null && owned.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /** 新库没有角色数据范围配置，只能给出最保守的口径：管理员全量，其他人本部门 + 本人。 */
    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        Principal principal = principalService.load(userId);
        if (principal == null) {
            result.setSelf(true);
            return result;
        }
        if (principal.has("*:*:*")) {
            result.setAll(true);
            result.setSelf(true);
            return result;
        }
        result.setSelf(true);
        SysUser user = users.selectById(userId);
        if (user != null && user.getMainDeptId() != null) {
            result.getDeptIds().add(user.getMainDeptId());
        }
        for (SysUserDept binding : userDepartments.selectList(new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, userId))) {
            result.getDeptIds().add(binding.getDeptId());
        }
        return result;
    }

    @Override
    public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        Set<Long> distinct = new LinkedHashSet<>();
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                if (roleId != null) {
                    distinct.add(roleId);
                }
            }
        }
        Set<Long> userIds = new LinkedHashSet<>();
        if (distinct.isEmpty()) {
            return userIds;
        }
        for (SysUserRole binding : userRoles.selectList(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, distinct)
                .orderByAsc(SysUserRole::getUserId))) {
            userIds.add(binding.getUserId());
        }
        return userIds;
    }

}
