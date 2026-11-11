package com.cyt.center.core.config;

import com.cyt.center.core.security.CurrentUser;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前后台同一套代码、两套登录态：设备类型写在令牌上，路径只决定校验强度。
 * 后台还要求令牌来自管理控制台入口，避免前台用户猜出 /admin-api 地址就能操作。
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {

    private final CurrentUser currentUser;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/admin-api/**").check(r -> {
                currentUser.require();
                currentUser.requireAdminConsole();
            });
            SaRouter.match("/api/**")
                    .notMatch("/api/auth/**", "/api/wecom/**", "/api/portal/media/images/*", "/api/portal/celebrations/cards/**")
                    .check(r -> currentUser.requireUserId());
        })).addPathPatterns("/**").excludePathPatterns("/error", "/favicon.ico");
    }

}
