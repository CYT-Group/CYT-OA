package com.cyt.center.module.system.api.notify;

import com.cyt.center.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 站内信发送 API 实现类。
 *
 * @author xrcoder
 */
@Slf4j
@Service
public class NotifyMessageSendApiImpl implements NotifyMessageSendApi {

    @Override
    public Long sendSingleMessageToAdmin(NotifySendSingleToUserReqDTO reqDTO) {
        return send(reqDTO, "admin");
    }

    @Override
    public Long sendSingleMessageToMember(NotifySendSingleToUserReqDTO reqDTO) {
        return send(reqDTO, "member");
    }

    /** 企微网关的消息发送接口尚未配置（WecomGatewayClient 目前只验证过通讯录一个操作），此处只记录不外发。 */
    private Long send(NotifySendSingleToUserReqDTO reqDTO, String userType) {
        if (reqDTO == null) {
            return null;
        }
        log.info("[send][{} 用户({}) 模板({}) 参数({}) 未发送：企微网关消息接口未配置]",
                userType, reqDTO.getUserId(), reqDTO.getTemplateCode(), reqDTO.getTemplateParams());
        return null;
    }

}
