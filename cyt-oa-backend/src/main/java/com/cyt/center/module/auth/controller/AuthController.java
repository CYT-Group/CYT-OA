package com.cyt.center.module.auth.controller;

import com.cyt.center.core.integration.PortalSsoClient;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.auth.model.cmd.LoginCmd;
import com.cyt.center.module.auth.model.vo.LoginVO;
import com.cyt.center.module.auth.model.vo.MeVO;
import com.cyt.center.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 口令登录与身份查询。/api/auth/** 在拦截器里公开，因此本控制器自行决定哪些接口要求登录。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final PortalSsoClient portal;

    @GetMapping("/policy")
    public Result<Map<String, Object>> policy() {
        return Result.ok(authService.policy(portal.available()));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginCmd cmd) {
        return Result.ok(authService.loginByPassword(cmd));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<MeVO> me() {
        Principal principal = currentUser.require();
        return Result.ok(new MeVO(principal.userId(), principal.username(), principal.name(), principal.roles(),
                principal.permissions(), currentUser.deviceType()));
    }

}
