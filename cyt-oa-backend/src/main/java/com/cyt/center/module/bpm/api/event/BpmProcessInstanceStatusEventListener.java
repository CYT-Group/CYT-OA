package com.cyt.center.module.bpm.api.event;

import java.util.Objects;

import org.springframework.context.ApplicationListener;

/**
 * 只关心某一条流程的监听器基类：按流程标识过滤后再交给业务处理。
 */
public abstract class BpmProcessInstanceStatusEventListener
        implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    @Override
    public final void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        if (!Objects.equals(event.getProcessDefinitionKey(), getProcessDefinitionKey())) {
            return;
        }
        onEvent(event);
    }

    protected abstract String getProcessDefinitionKey();

    protected abstract void onEvent(BpmProcessInstanceStatusEvent event);

}
