package com.cyt.center.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.cyt.center.framework.mybatis.core.dataobject.BaseDO;
import com.cyt.center.framework.mybatis.core.handler.DefaultDBFieldHandler;
import com.cyt.center.framework.mybatis.core.type.NumericBooleanTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分页插件、逻辑删除与公共字段填充。
 * 不引入多租户、数据权限等与当前业务无关的拦截器。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public ConfigurationCustomizer numericBooleanTypeCustomizer() {
        return configuration -> {
            var handler = new NumericBooleanTypeHandler();
            configuration.getTypeHandlerRegistry().register(Boolean.class, handler);
            configuration.getTypeHandlerRegistry().register(boolean.class, handler);
        };
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * 从 ruoyi 迁移过来的表沿用 creator / create_time / updater / update_time / deleted 五个公共列，
     * 实体统一继承 {@link BaseDO}；个人中心自有的 sys_* 表使用 created_at / updated_at，
     * 不继承 BaseDO，也不会被下面的填充器命中（填充前会校验 hasGetter / hasSetter）。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new DefaultDBFieldHandler();
    }

    /**
     * 逻辑删除：列名 deleted，1 已删除、0 未删除，与 ruoyi 建表脚本一致。
     *
     * BaseDO 的 deleted 字段本身带 @TableLogic，这里再配置全局规则，
     * 让移植过来的表即使漏写注解也能按同一口径删除；MyBatis-Plus 3.5.6 没有
     * GlobalConfigCustomizer，改用官方提供的 MybatisPlusPropertiesCustomizer。
     */
    @Bean
    public MybatisPlusPropertiesCustomizer logicDeletePropertiesCustomizer() {
        return properties -> {
            GlobalConfig globalConfig = properties.getGlobalConfig();
            if (globalConfig == null) {
                globalConfig = new GlobalConfig();
                properties.setGlobalConfig(globalConfig);
            }
            GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();
            if (dbConfig == null) {
                dbConfig = new GlobalConfig.DbConfig();
                globalConfig.setDbConfig(dbConfig);
            }
            dbConfig.setLogicDeleteField("deleted");
            dbConfig.setLogicDeleteValue("1");
            dbConfig.setLogicNotDeleteValue("0");
        };
    }

}
