package com.cyt.center.module.bpm.api.task.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建流程实例的请求。
 */
@Data
public class BpmProcessInstanceCreateReqDTO {

    /**
     * 流程定义标识，例如 oa_leave
     */
    @NotEmpty(message = "流程定义的标识不能为空")
    private String processDefinitionKey;

    /**
     * 流程变量，审批条件与表单内容都靠它传递
     */
    private Map<String, Object> variables;

    /**
     * 业务唯一标识，例如请假申请编号，用于反查实例
     */
    @NotEmpty(message = "业务的唯一标识")
    private String businessKey;

    /**
     * 发起人自选审批人：key 为节点 taskKey，value 为审批人用户编号
     */
    private Map<String, List<Long>> startUserSelectAssignees;

}
