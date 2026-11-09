package com.cyt.center.core.config;

import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 客户端微调。
 */
@Configuration
public class RedisConfiguration {

    /**
     * 空密码按未配置处理。yml 里 password: ${CYT_REDIS_PASSWORD:} 在未设环境变量时解析成空串，
     * starter 会把空串设到 Config 顶层，而 Redisson 对非 null 密码一律发 AUTH，无密码的 Redis 会拒绝；
     * 置 null 后连接时不发 AUTH。注意 Redisson 4.x 的密码以 Config 顶层为准，
     * useSingleServer().setPassword 已废弃，不能在那里清。
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonBlankPasswordCustomizer() {
        return configuration -> {
            String password = configuration.getPassword();
            if (password != null && password.isEmpty()) {
                configuration.setPassword(null);
            }
        };
    }

}
