package com.cyt.center.module.portal.controller.admin.feedback;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.controller.admin.feedback.vo.PortalFeedbackRespVO;
import com.cyt.center.module.portal.dal.dataobject.feedback.PortalFeedbackDO;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.feedback.PortalFeedbackService;
import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cyt.center.core.result.Result.success;

/**
 * 员工反馈管理接口（迁自 Portal 的 routers/feedback.py 的查看端）。
 *
 * 提交与查看权限不对称：人人可提（走 Open API），查看全量要看板权限 can_view_dashboards。
 */
@Tag(name = "管理后台 - 员工反馈")
@RestController
@RequestMapping("/portal/feedback")
@Validated
@RequiredArgsConstructor
public class PortalFeedbackController {

    private final PortalFeedbackService feedbackService;

    @GetMapping("/page")
    @Operation(summary = "分页查看员工反馈全量（按创建时间倒序）")
    @SaCheckPermission(PortalCapability.VIEW_DASHBOARDS)
    public Result<PageResult<PortalFeedbackRespVO>> page(PageParam pageParam) {
        PageResult<PortalFeedbackDO> rows = feedbackService.page(pageParam);
        return success(new PageResult<>(rows.getList().stream().map(this::toVO).toList(), rows.getTotal()));
    }

    private PortalFeedbackRespVO toVO(PortalFeedbackDO fb) {
        PortalFeedbackRespVO vo = new PortalFeedbackRespVO();
        vo.setId(fb.getId());
        vo.setReporterName(fb.getReporterName());
        vo.setDepartment(fb.getDepartment());
        vo.setCategory(fb.getCategory());
        vo.setSubCategory(fb.getSubCategory());
        vo.setBrand(fb.getBrand());
        vo.setProductName(fb.getProductName());
        vo.setRawMessage(fb.getRawMessage());
        vo.setImageUrls(fb.getImageUrls());
        vo.setStatus(fb.getStatus());
        vo.setCreatedAt(fb.getCreatedAt());
        return vo;
    }

}
