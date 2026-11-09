package com.cyt.center.module.oa.service.reimbursement.listener;

import com.cyt.center.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import com.cyt.center.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import com.cyt.center.module.oa.enums.BpmModelConstants;
import com.cyt.center.module.oa.service.reimbursement.OaReimbursementService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 费用报销审批结果监听器
 *
 * @author 芋道源码
 */
@Component
public class OaReimbursementStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private OaReimbursementService reimbursementService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmModelConstants.REIMBURSEMENT;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        reimbursementService.updateReimbursementStatus(Long.valueOf(event.getBusinessKey()), event.getStatus());
    }

}
