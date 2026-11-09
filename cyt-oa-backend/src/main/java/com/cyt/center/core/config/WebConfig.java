package com.cyt.center.core.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域来源只由服务端配置列出，且必须允许携带登录 Cookie。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CenterProperties properties;

    @PostConstruct
    public void logCorsOrigins() {
        log.info("[cors-debug] allowedOriginList={}", properties.getWeb().allowedOriginList());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (properties.getWeb().allowedOriginList().isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(properties.getWeb().allowedOriginList().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 从 ruoyi 移植过来的后台接口都放在 controller.admin 包里，源码里没有写 /admin-api 前缀
     * （上游靠同样的约定补齐）。加上前缀它们才会落进 Sa-Token 的后台保护范围。
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/admin-api",
                controllerClass -> controllerClass.getPackageName().startsWith("com.cyt.center.module.")
                        && controllerClass.getPackageName().contains(".controller.admin"));
    }

}
