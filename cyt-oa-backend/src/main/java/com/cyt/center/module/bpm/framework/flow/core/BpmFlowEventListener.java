package com.cyt.center.module.bpm.framework.flow.core;

import com.cyt.center.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.warm.plugin.modes.sb.utils.SpringUtil;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 引擎全局监听器：桥接 warm-flow 与业务事件模型。
 *
 * 1) assignment：把发起人自选审批人（流程变量 _assignees）覆盖到新生成的任务上，
 *    发起与每个节点通过时都会触发，替代 FlowParams.nextHandler 的逐调用传递。
 * 2) finish：实例到达终态时发布 {@link BpmProcessInstanceStatusEvent}，
 *    移植过来的业务监听器按流程标识回写单据状态。
 *
 * warm-flow 按全类名反射实例化（见 application.yml 的 global-listener-path）。
 */
public class BpmFlowEventListener implements GlobalListener {

    /** 已发布过终态事件的实例，防止同一实例多个任务完成时重复发布 */
    private final Set<Long> published = ConcurrentHashMap.newKeySet();

    @Override
    public void assignment(ListenerVariable listenerVariable) {
        Map<String, Object> variable = listenerVariable.getVariable();
        List<Task> nextTasks = listenerVariable.getNextTasks();
        if (variable == null || nextTasks == null) {
            return;
        }
        Object assignees = variable.get(BpmFlowConstants.ASSIGNEE_VAR_KEY);
        if (!(assignees instanceof Map<?, ?> assigneeMap)) {
            return;
        }
        for (Task nextTask : nextTasks) {
            Object ids = assigneeMap.get(nextTask.getNodeCode());
            if (!(ids instanceof Collection<?> collection) || collection.isEmpty()) {
                continue;
            }
            List<String> permissionList = new ArrayList<>();
            for (Object id : collection) {
                if (id instanceof Number number) {
                    permissionList.add(String.valueOf(number.longValue()));
                } else if (id != null && !String.valueOf(id).isBlank()) {
                    permissionList.add(String.valueOf(id));
                }
            }
            if (!permissionList.isEmpty()) {
                nextTask.setPermissionList(permissionList);
            }
        }
    }

    @Override
    public void finish(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        if (instance == null) {
            return;
        }
        Integer status = mapTerminalStatus(instance);
        if (status == null || !published.add(instance.getId())) {
            return;
        }
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(this);
        event.setId(String.valueOf(instance.getId()));
        event.setProcessDefinitionKey(listenerVariable.getDefinition().getFlowCode());
        event.setStatus(status);
        event.setBusinessKey(instance.getBusinessId());
        event.setReason(listenerVariable.getFlowParams() == null ? null
                : listenerVariable.getFlowParams().getMessage());
        ApplicationContext context = SpringUtil.getApplicationContext();
        if (context != null) {
            context.publishEvent(event);
        }
    }

    /**
     * warm-flow 状态到 yudao 终态的映射。
     * 9（已退回）只有实例停在结束节点时才是终态「审批不通过」；退回中间节点重走不发事件。
     */
    private Integer mapTerminalStatus(Instance instance) {
        String flowStatus = instance.getFlowStatus();
        if (FlowStatus.PASS.getKey().equals(flowStatus)
                || FlowStatus.AUTO_PASS.getKey().equals(flowStatus)
                || FlowStatus.FINISHED.getKey().equals(flowStatus)) {
            return BpmProcessInstanceStatusEnum.APPROVE.getStatus();
        }
        if (FlowStatus.TERMINATE.getKey().equals(flowStatus)
                || FlowStatus.NULLIFY.getKey().equals(flowStatus)
                || FlowStatus.CANCEL.getKey().equals(flowStatus)
                || FlowStatus.INVALID.getKey().equals(flowStatus)) {
            return BpmProcessInstanceStatusEnum.CANCEL.getStatus();
        }
        if (FlowStatus.REJECT.getKey().equals(flowStatus) && NodeType.isEnd(instance.getNodeType())) {
            return BpmProcessInstanceStatusEnum.REJECT.getStatus();
        }
        return null;
    }

}
