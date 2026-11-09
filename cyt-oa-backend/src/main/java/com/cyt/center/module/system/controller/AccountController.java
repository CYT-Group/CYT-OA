package com.cyt.center.module.system.controller;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.system.model.cmd.AssignRolesCmd;
import com.cyt.center.module.system.model.entity.SysRole;
import com.cyt.center.module.system.model.query.SysUserQuery;
import com.cyt.center.module.system.model.vo.OptionVO;
import com.cyt.center.module.system.model.vo.SysUserVO;
import com.cyt.center.module.system.service.SysRoleService;
import com.cyt.center.module.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 账号管理：档案只读（来自企微），可改的只有角色。
 */
@Slf4j
@RestController
@RequestMapping("/admin-api/system")
@RequiredArgsConstructor
public class AccountController {

    private static final String PERMISSION_USER_QUERY = "system:user:query";
    private static final String PERMISSION_USER_ASSIGN = "system:user:assign-role";
    private static final String PERMISSION_ROLE_QUERY = "system:role:query";

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final CurrentUser currentUser;

    @GetMapping("/users")
    public Result<PageResult<SysUserVO>> users(@Valid SysUserQuery query) {
        currentUser.requirePermission(PERMISSION_USER_QUERY);
        return Result.ok(userService.page(query));
    }

    /** 指派部门负责人用的下拉：一次给全，不走分页，否则只能在前 20 个人里挑。 */
    @GetMapping("/users/leader-candidates")
    public Result<List<OptionVO>> leaderCandidates() {
        currentUser.requirePermission(PERMISSION_USER_QUERY);
        return Result.ok(userService.leaderCandidates());
    }

    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        currentUser.requirePermission(PERMISSION_ROLE_QUERY);
        return Result.ok(roleService.list());
    }

    @PutMapping("/users/{userId}/roles")
    public Result<Void> assignRoles(@PathVariable("userId") Long userId, @Valid @RequestBody AssignRolesCmd cmd) {
        currentUser.requirePermission(PERMISSION_USER_ASSIGN);
        userService.assignRoles(userId, cmd.getRoles());
        var actor = currentUser.require();
        log.info("账号角色调整 actor={}, userId={}, roles={}", actor.userId(), userId, cmd.getRoles());
        return Result.ok();
    }

    public record AccountStatusCmd(@jakarta.validation.constraints.NotNull Boolean enabled) { }

    @PutMapping("/users/{userId}/status")
    public Result<Void> status(@PathVariable("userId") Long userId, @Valid @RequestBody AccountStatusCmd cmd) {
        currentUser.requirePermission(PERMISSION_USER_ASSIGN);
        userService.changeStatus(userId, cmd.enabled(), currentUser.requireUserId());
        return Result.ok();
    }

}
