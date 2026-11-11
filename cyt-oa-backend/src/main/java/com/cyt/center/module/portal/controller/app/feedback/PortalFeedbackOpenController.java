package com.cyt.center.module.portal.controller.app.feedback;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.controller.admin.feedback.vo.PortalFeedbackSubmitReqVO;
import com.cyt.center.module.portal.dal.dataobject.feedback.PortalFeedbackDO;
import com.cyt.center.module.portal.service.feedback.PortalFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 员工反馈读侧/写入口（登录即可提交，无能力位要求）。
 *
 * 提交走 Open API（门户登录人拿普通令牌，管理令牌要求 device=admin）；
 * 查看全量在管理面（/admin-api/portal/feedback/page，can_view_dashboards）。
 */
@Tag(name = "门户 - 员工反馈")
@RestController
@RequestMapping("/api/portal/feedback")
@Validated
@RequiredArgsConstructor
public class PortalFeedbackOpenController {

    private final PortalFeedbackService feedbackService;

    @PostMapping("/submit")
    @Operation(summary = "提交一条员工反馈（任何登录用户可提）")
    public Result<Long> submit(@Valid @RequestBody PortalFeedbackSubmitReqVO reqVO) {
        PortalFeedbackDO fb = feedbackService.submit(getLoginUserId(), reqVO);
        return success(fb.getId());
    }

}
