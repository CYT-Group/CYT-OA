package com.cyt.center.module.portal.service.feedback;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.portal.controller.admin.feedback.vo.PortalFeedbackSubmitReqVO;
import com.cyt.center.module.portal.dal.dataobject.feedback.PortalFeedbackDO;

/**
 * 员工反馈用例（迁自 Portal 的 application/feedback.py）。
 *
 * 提交与查看权限不对称：任何登录用户可提；查看全量要看板权限（can_view_dashboards）。
 */
public interface PortalFeedbackService {

    PortalFeedbackDO submit(Long userId, PortalFeedbackSubmitReqVO reqVO);

    PageResult<PortalFeedbackDO> page(PageParam pageParam);

}
