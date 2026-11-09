package com.cyt.center.module.oa.service.seal.listener;

import com.cyt.center.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.cyt.center.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.cyt.center.module.oa.enums.BpmModelConstants;
import com.cyt.center.module.oa.service.seal.OaSealApplyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 用印申请审批结果监听器
 *
 * @author 芋道源码
 */
@Component
public class OaSealApplyStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private OaSealApplyService sealApplyService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmModelConstants.SEAL_APPLY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        sealApplyService.updateSealApplyStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
