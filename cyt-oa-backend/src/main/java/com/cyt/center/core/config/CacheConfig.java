package com.cyt.center.core.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 开启 Spring Cache。
 *
 * 移植过来的社保标准数据 Service 用 {@code @Cacheable} 缓存第三方页面抓来的参保类型与缴费基数（上游 TTL 7 天），
 * 否则每次打开社保配置页都要重新抓一次外部接口。本项目单实例部署，用 Caffeine 进程内缓存即可，
 * 不需要分布式缓存；过期策略见 application.yml 的 spring.cache.caffeine.spec。
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
