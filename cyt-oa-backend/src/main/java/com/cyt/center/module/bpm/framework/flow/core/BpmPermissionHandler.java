package com.cyt.center.module.bpm.framework.flow.core;

import com.cyt.center.framework.security.core.util.SecurityFrameworkUtils;
import com.cyt.center.module.system.mapper.SysUserRoleMapper;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.plugin.modes.sb.utils.SpringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 办理人权限处理器。
 *
 * 节点权限标识（流程 JSON 的 permissionFlag）写成 {@code role:<角色编码>}，多个用 @@ 分隔；
 * 任务创建时引擎调用 {@link #convertPermissions(List)} 把角色展开成具体用户编号。
 * 审批时 {@link #permissions()} 返回「当前用户编号 + 其角色标识」，供引擎与任务审批人做交集校验。
 *
 * warm-flow 按全类名反射实例化（见 application.yml 的 permission-handler-path），
 * 不走 Spring 容器，因此依赖一律运行时从 SpringUtil 取。
 */
public class BpmPermissionHandler implements PermissionHandler {

    private static final String ROLE_PREFIX = "role:";

    @Override
    public List<String> permissions() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        result.add(String.valueOf(userId));
        selectRoleCodes(userId).forEach(code -> result.add(ROLE_PREFIX + code));
        return result;
    }

    @Override
    public String getHandler() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? null : String.valueOf(userId);
    }

    @Override
    public List<String> convertPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()
                || permissions.stream().noneMatch(item -> item.startsWith(ROLE_PREFIX))) {
            return permissions;
        }
        LinkedHashSet<String> converted = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission.startsWith(ROLE_PREFIX)) {
                String roleCode = permission.substring(ROLE_PREFIX.length());
                selectUserIdsByRoleCode(roleCode).forEach(userId -> converted.add(String.valueOf(userId)));
            } else {
                converted.add(permission);
            }
        }
        return new ArrayList<>(converted);
    }

    private List<String> selectRoleCodes(Long userId) {
        try {
            return SpringUtil.getBean(SysUserRoleMapper.class).selectRoleCodes(userId);
        } catch (Exception ex) {
            // 引擎初始化早于容器可用时降级为空：不影响启动，业务调用时容器必然已就绪
            return Collections.emptyList();
        }
    }

    private List<Long> selectUserIdsByRoleCode(String roleCode) {
        try {
            return SpringUtil.getBean(SysUserRoleMapper.class).selectUserIdsByRoleCode(roleCode);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

}
