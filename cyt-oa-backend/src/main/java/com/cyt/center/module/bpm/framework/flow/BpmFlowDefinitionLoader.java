package com.cyt.center.module.bpm.framework.flow;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.entity.Definition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 启动时把 classpath 下的流程定义导入引擎并发布。
 *
 * 流程定义由代码持有（db/flow-def/*.json，见 pom 注释：不引入设计器端点），
 * 首次启动导入，之后版本不变则跳过；改流程需要升版本时手动更新数据库或清表重导。
 */
@Slf4j
@Component
public class BpmFlowDefinitionLoader implements ApplicationRunner {

    private static final String FLOW_DEF_PATTERN = "classpath:db/flow-def/*.json";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(FLOW_DEF_PATTERN);
        int imported = 0;
        for (Resource resource : resources) {
            String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            DefJson defJson = FlowEngine.jsonConvert.strToBean(json, DefJson.class);
            if (FlowEngine.defService().getPublishByFlowCode(defJson.getFlowCode()) != null) {
                continue;
            }
            Definition definition = FlowEngine.defService().importDef(defJson);
            FlowEngine.defService().publish(definition.getId());
            imported++;
            log.info("[flow] 导入并发布流程定义：{} ({})", defJson.getFlowName(), defJson.getFlowCode());
        }
        if (imported > 0) {
            log.info("[flow] 本次共导入 {} 条流程定义", imported);
        }
    }

}
