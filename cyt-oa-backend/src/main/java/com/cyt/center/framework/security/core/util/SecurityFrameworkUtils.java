package com.cyt.center.framework.security.core.util;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.lang.Nullable;

/**
 * 安全服务工具类（芋道同名类的薄适配）
 *
 * 本项目的登录态由 Sa-Token 承载，前后台共用同一套代码、两套入口设备（见
 * {@link com.cyt.center.core.config.SaTokenConfigure}），令牌里保存的就是 sys_user 主键。
 * 移植过来的 ruoyi 代码只按静态方法取“当前登录用户编号”，因此这里只暴露静态读法，
 * 不做 Spring Security 的 SecurityContext、也不引入芋道的 LoginUser 令牌体系。
 *
 * 注意：非 Web 线程（定时任务、异步线程）取不到上下文时返回 null，不抛异常，
 * 否则审计字段自动填充会把任务直接打断。
 */
public class SecurityFrameworkUtils {

    private SecurityFrameworkUtils() {
    }

    /**
     * 获得当前用户的编号，从上下文中
     *
     * @return 用户编号；未登录或无请求上下文时为 null
     */
    @Nullable
    public static Long getLoginUserId() {
        Object loginId = getLoginId();
        if (loginId == null) {
            return null;
        }
        String value = loginId.toString();
        // 令牌身份必须是数值编号；历史的非数值身份按“未登录”处理，避免抛NumberFormatException
        return value.matches("\\d{1,19}") ? Long.valueOf(value) : null;
    }

    /**
     * 获得当前登录标识（Sa-Token 的 loginId）
     *
     * @return 登录标识；未登录或无请求上下文时为 null
     */
    @Nullable
    private static Object getLoginId() {
        try {
            return StpUtil.getLoginIdDefaultNull();
        } catch (Exception ex) {
            // 无请求上下文（如定时任务）时，Sa-Token 会抛异常，这里统一降级为未登录
            return null;
        }
    }

}
