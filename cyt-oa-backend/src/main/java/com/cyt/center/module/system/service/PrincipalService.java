package com.cyt.center.module.system.service;

import com.cyt.center.core.security.Principal;
import com.cyt.center.core.security.PrincipalRepository;
import com.cyt.center.module.system.mapper.SysMenuMapper;
import com.cyt.center.module.system.mapper.SysUserMapper;
import com.cyt.center.module.system.mapper.SysUserRoleMapper;
import com.cyt.center.module.system.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/**
 * 身份装载：姓名等档案来自企微同步后的账号表，角色与权限始终在本地库。
 * 账号停用（企微非激活成员）后一律视为不存在，已发令牌由同步流程主动踢出。
 */
@Service
@RequiredArgsConstructor
public class PrincipalService implements PrincipalRepository {

    public static final String ROLE_ADMIN = "admin";

    private final SysUserMapper users;
    private final SysUserRoleMapper userRoles;
    private final SysMenuMapper menus;

    @Override
    public Principal load(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = users.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return null;
        }
        List<String> roles = userRoles.selectRoleCodes(userId);
        // Sa-Token 的 *:*:* 按冒号分段匹配，只能兜住 oa:/system: 这类带冒号的权限串；
        // Portal 搬来的能力位是无冒号的 can_*，落不进通配，所以管理员也要吃自己名下的显式授权。
        List<String> granted = menus.selectPermissions(userId);
        List<String> permissions = roles.contains(ROLE_ADMIN)
                ? Stream.concat(Stream.of("*:*:*"), granted.stream()).toList()
                : granted;
        return new Principal(user.getId(), user.getUsername(), user.getName(), roles, permissions);
    }

}
