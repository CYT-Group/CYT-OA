package com.cyt.center.module.bpm.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

/**
 * 流程实例状态变化事件：审批通过/驳回/取消后由引擎发出，业务模块监听后回写单据状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmProcessInstanceStatusEvent extends ApplicationEvent {

    private String id;
    private String processDefinitionKey;
    private Integer status;
    private String reason;
    /** 业务标识，例如请假申请编号 */
    private String businessKey;

    public BpmProcessInstanceStatusEvent(Object source) {
        super(source);
    }

}
