package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.service.support.PortalAudit;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 角色管理权与密钥管理权只属于系统管理员，不能通过能力矩阵转授。 */
@Service
@RequiredArgsConstructor
public class PortalRoleService {
    private final JdbcTemplate jdbc;
    private final CurrentUser currentUser;
    private final PortalAudit audit;
    private static final Set<String> FIXED = Set.of("can_manage_roles", "can_manage_ai_keys");

    public void requireAdmin() {
        if (!currentUser.require().isAdmin()) throw AuthException.forbidden("仅系统管理员可维护角色权限");
    }

    public Map<String, Object> list() {
        requireAdmin();
        var roles =
                jdbc.queryForList(
                        "SELECT id,code,name,built_in AS builtIn,remark FROM sys_role ORDER BY id");
        for (var role : roles)
            role.put("permissions", permissions(((Number) role.get("id")).longValue()));
        return Map.of(
                "roles",
                roles,
                "capabilities",
                jdbc.queryForList(
                        "SELECT DISTINCT permission,name FROM sys_menu WHERE"
                            + " LEFT(permission,4)='can_' ORDER BY permission"));
    }

    private List<String> permissions(long roleId) {
        return jdbc.queryForList(
                "SELECT DISTINCT m.permission FROM sys_menu m JOIN sys_role_menu rm ON"
                    + " rm.menu_id=m.id WHERE rm.role_id=? AND LEFT(m.permission,4)='can_' ORDER BY"
                    + " m.permission",
                String.class,
                roleId);
    }

    @Transactional
    public void save(Long id, String code, String name, String remark, Set<String> requested) {
        requireAdmin();
        if (name == null || name.isBlank() || name.length() > 64)
            throw new BizException("请输入1至64字角色名称");
        if (requested == null) throw new BizException("缺少能力清单");
        var capabilities =
                jdbc.queryForList(
                        "SELECT DISTINCT permission FROM sys_menu WHERE LEFT(permission,4)='can_'",
                        String.class);
        if (!new HashSet<>(capabilities).containsAll(requested))
            throw new BizException("包含未注册的能力位");
        Map<String, Object> before = null;
        if (id == null) {
            if (code == null || !code.matches("[a-z][a-z0-9_-]{1,63}"))
                throw new BizException("角色标识须为2至64位小写字母、数字、下划线或连字符");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE code=?", Long.class, code)
                    > 0) throw new BizException("角色标识已存在");
            jdbc.update(
                    "INSERT INTO sys_role(code,name,remark,built_in) VALUES(?,?,?,0)",
                    code,
                    name.trim(),
                    remark == null ? "" : remark);
            id = jdbc.queryForObject("SELECT id FROM sys_role WHERE code=?", Long.class, code);
        } else {
            var rows = jdbc.queryForList("SELECT * FROM sys_role WHERE id=? FOR UPDATE", id);
            if (rows.isEmpty()) throw new BizException("角色不存在");
            before = rows.getFirst();
            code = String.valueOf(before.get("code"));
            if ("admin".equals(code)) throw new BizException("管理员拥有全部能力，无需编辑授权");
            jdbc.update(
                    "UPDATE sys_role SET name=?,remark=? WHERE id=?",
                    name.trim(),
                    remark == null ? "" : remark,
                    id);
        }
        if (!"admin".equals(code) && requested.stream().anyMatch(FIXED::contains))
            throw new BizException("角色管理和密钥管理能力不能转授");
        // 保留角色已有的 OA 权限，仅替换 Portal 能力位。
        jdbc.update(
                "DELETE rm FROM sys_role_menu rm JOIN sys_menu m ON m.id=rm.menu_id WHERE"
                    + " rm.role_id=? AND LEFT(m.permission,4)='can_'",
                id);
        for (String permission : requested)
            jdbc.update(
                    "INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT ?,id FROM sys_menu"
                        + " WHERE permission=?",
                    id,
                    permission);
        audit.record(
                currentUser.requireUserId(),
                "role_permissions",
                "sys_role",
                id,
                JsonUtils.toJsonString(before),
                JsonUtils.toJsonString(requested),
                name);
    }

    @Transactional
    public void delete(long id) {
        requireAdmin();
        var rows = jdbc.queryForList("SELECT * FROM sys_role WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new BizException("角色不存在");
        Object builtin = rows.getFirst().get("built_in");
        if (Boolean.TRUE.equals(builtin) || (builtin instanceof Number n && n.intValue() != 0))
            throw new BizException("内置角色不能删除");
        if (jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_user_role WHERE role_id=?", Long.class, id)
                > 0) throw new BizException("请先移除该角色的成员");
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id=?", id);
        jdbc.update("DELETE FROM sys_role WHERE id=?", id);
        audit.record(
                currentUser.requireUserId(),
                "role_delete",
                "sys_role",
                id,
                JsonUtils.toJsonString(rows.getFirst()),
                null,
                null);
    }
}
