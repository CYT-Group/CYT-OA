package com.cyt.center.module.bpm.api.task;

import com.cyt.center.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import jakarta.validation.Valid;

/**
 * 流程引擎对业务模块暴露的门面。OA / HR 的申请单只依赖这几个方法，
 * 底层换成哪个流程引擎不影响业务代码。
 */
public interface BpmProcessInstanceApi {

    /**
     * 发起流程实例
     *
     * @param userId 发起人
     * @param reqDTO 流程标识、变量与业务编号
     * @return 流程实例编号
     */
    String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO);

    /**
     * 发起人撤销仍在流程中的实例
     */
    void cancelProcessInstanceByStartUser(Long userId, String processInstanceId, String reason);

}
