package com.cyt.center.framework.dict.config;

import com.cyt.center.framework.dict.core.DictFrameworkUtils;
import com.cyt.center.module.system.api.dict.DictDataApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 字典能力的装配类。
 *
 * 芋道原版由 framework/dict 的 YudaoDictAutoConfiguration（自动配置 + spring.factories）完成，
 * 本项目是单模块、不使用自定义 starter，因此在 framework 内部用一个普通 @Configuration 完成同样的事：
 * 把 system 模块提供的 {@link DictDataApi} 注入静态工具类 DictFrameworkUtils。
 *
 * 用 ObjectProvider 而不是直接依赖，保证字典实现缺席时应用仍能启动（此时字典校验、Excel 字典列按空数据处理）。
 */
@Slf4j
@Configuration
public class DictFrameworkConfiguration {

    @Bean
    public DictFrameworkUtils dictFrameworkUtils(ObjectProvider<DictDataApi> dictDataProvider) {
        DictDataApi dictDataApi = dictDataProvider.getIfAvailable();
        if (dictDataApi != null) {
            DictFrameworkUtils.init(dictDataApi);
        } else {
            log.warn("[dictFrameworkUtils][未发现 DictDataApi 实现，字典相关能力将返回空数据]");
        }
        return new DictFrameworkUtils();
    }

}
