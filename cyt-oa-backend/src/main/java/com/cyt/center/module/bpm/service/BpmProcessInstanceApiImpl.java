package com.cyt.center.module.bpm.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.bpm.api.task.BpmProcessInstanceApi;
import com.cyt.center.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.cyt.center.module.bpm.framework.flow.core.BpmFlowConstants;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程引擎对业务模块暴露的门面实现（warm-flow）。
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Override
    public String createProcessInstance(Long userId, BpmProcessInstanceCreateReqDTO reqDTO) {
        Map<String, Object> variables = reqDTO.getVariables() == null
                ? new HashMap<>() : new HashMap<>(reqDTO.getVariables());
        if (reqDTO.getStartUserSelectAssignees() != null && !reqDTO.getStartUserSelectAssignees().isEmpty()) {
            variables.put(BpmFlowConstants.ASSIGNEE_VAR_KEY, reqDTO.getStartUserSelectAssignees());
        }
        FlowParams flowParams = FlowParams.build()
                .flowCode(reqDTO.getProcessDefinitionKey())
                .handler(String.valueOf(userId))
                .variable(variables);
        try {
            Instance instance = FlowEngine.insService().start(reqDTO.getBusinessKey(), flowParams);
            return String.valueOf(instance.getId());
        } catch (FlowException ex) {
            throw new BizException(ex.getMessage());
        }
    }

    @Override
    public void cancelProcessInstanceByStartUser(Long userId, String processInstanceId, String reason) {
        Instance instance = FlowEngine.insService().getById(Long.valueOf(processInstanceId));
        if (instance == null) {
            throw new BizException("流程实例不存在");
        }
        if (!String.valueOf(userId).equals(instance.getCreateBy())) {
            throw new BizException("只有发起人可以撤销流程");
        }
        if (NodeType.isEnd(instance.getNodeType())) {
            throw new BizException("流程已结束，无法撤销");
        }
        try {
            FlowEngine.taskService().revoke(Long.valueOf(processInstanceId), FlowParams.build()
                    .handler(String.valueOf(userId))
                    .message(reason));
        } catch (FlowException ex) {
            throw new BizException(ex.getMessage());
        }
    }

}
