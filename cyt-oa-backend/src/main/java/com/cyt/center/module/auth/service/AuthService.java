package com.cyt.center.module.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.core.security.Principal;
import com.cyt.center.module.auth.model.cmd.LoginCmd;
import com.cyt.center.module.auth.model.vo.LoginVO;
import com.cyt.center.module.system.service.PrincipalService;
import com.cyt.center.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 登录策略的唯一出口：线上只有企微，本机才允许口令；两条通道都只认服务端账号表，
 * 绝不接受前端传来的姓名、部门或角色。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CenterProperties properties;
    private final SysUserService users;
    private final PrincipalService principals;

    public Map<String, Object> policy(boolean wecomAvailable) {
        return Map.of(
                "passwordLoginEnabled", properties.getAuth().isPasswordLoginEnabled(),
                "wecomEnabled", wecomAvailable);
    }

    public LoginVO loginByPassword(LoginCmd cmd) {
        if (!properties.getAuth().isPasswordLoginEnabled()) {
            throw new BizException("当前环境仅允许企业微信扫码登录");
        }
        var user = users.authenticate(cmd.getUsername().trim(), cmd.getPassword());
        return issue(user.getId(), cmd.getDevice());
    }

    public static final String DEVICE_AUTO = "auto";

    /**
     * 建立登录态并返回令牌。
     * 管理控制台只创建会话、不写 Cookie，避免把管理员令牌落到浏览器个人中心的 Cookie 里。
     */
    public LoginVO issue(Long userId, String device) {
        Principal principal = principals.load(userId);
        if (principal == null) {
            throw AuthException.forbidden("账号不存在或已停用，请联系管理员");
        }
        String deviceType = resolveDevice(device, principal);
        if (CurrentUser.DEVICE_ADMIN.equals(deviceType) && !principal.canAccessConsole()) {
            throw AuthException.forbidden("该账号没有管理控制台权限");
        }
        users.markLogin(userId);
        if (CurrentUser.DEVICE_ADMIN.equals(deviceType)) {
            String token = StpUtil.createLoginSession(userId, new SaLoginParameter().setDeviceType(deviceType));
            return new LoginVO(token, deviceType, principal.userId(), principal.name(), principal.roles(),
                    principal.permissions());
        }
        // 换人登录时必须清掉浏览器上残留的上一位身份。
        StpUtil.logout();
        StpUtil.login(userId, new SaLoginParameter().setDeviceType(deviceType));
        return new LoginVO(StpUtil.getTokenValue(), deviceType, principal.userId(), principal.name(),
                principal.roles(), principal.permissions());
    }

    public void logout() {
        StpUtil.logout();
    }

    private static String normalizeDevice(String device) {
        if (device == null || device.isBlank() || CurrentUser.DEVICE_FRONT.equals(device)) {
            return CurrentUser.DEVICE_FRONT;
        }
        if (CurrentUser.DEVICE_ADMIN.equals(device) || DEVICE_AUTO.equals(device)) {
            return device;
        }
        throw new BizException("登录入口无效");
    }

    /**
     * auto 让一个登录页同时服务员工个人中心和管理控制台：有管理角色的进管理台，
     * 其余落到前台。角色只认服务端账号表，不看前端传来的任何声明。
     */
    private static String resolveDevice(String device, Principal principal) {
        String requested = normalizeDevice(device);
        if (!DEVICE_AUTO.equals(requested)) {
            return requested;
        }
        return principal.canAccessConsole()
                ? CurrentUser.DEVICE_ADMIN : CurrentUser.DEVICE_FRONT;
    }

}
