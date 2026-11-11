package com.cyt.center.module.portal.service.signoff;

import com.cyt.center.module.portal.controller.admin.signoff.vo.PortalSignoffSignReqVO;
import com.cyt.center.module.portal.dal.dataobject.signoff.PortalHandbookSignoffDO;

import java.util.List;
import java.util.Map;

/**
 * 制度签收用例（迁自 Portal 的 application/handbook_signoff.py）。
 *
 * 签收 / 我的签收 / 签收统计；催签（notify-unsigned）依赖企微推送，随推送切片落地。
 */
public interface PortalHandbookSignoffService {

    PortalHandbookSignoffDO sign(Long userId, PortalSignoffSignReqVO reqVO, String ip, String userAgent);

    MySignoffs mySignoffs(Long userId);

    Map<String, Object> stats(String document, Long documentId, String version);

    /** 统计范围内的已发布制度元数据，不要求文档编辑权。 */
    List<com.cyt.center.module.portal.domain.signoff.PortalSignoffDocumentOption> documentOptions();

    Map<String, Object> notifyUnsigned(Long actor, String document, Long documentId, String version, String customMessage);

    record MySignoffs(List<PortalHandbookSignoffDO> items, Map<String, String> currentVersions) {
    }

}
