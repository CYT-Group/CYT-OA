package com.cyt.center.module.portal.controller.admin.signoff;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.service.signoff.PortalHandbookSignoffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.cyt.center.core.result.Result.success;

/**
 * 制度签收管理接口（迁自 Portal 的 routers/handbook_signoff.py 的 admin 端）。
 *
 * 签收覆盖率 / 未签清单（can_view_signoff_stats）；催签（notify-unsigned）依赖企微推送，随推送切片落地。
 */
@Tag(name = "管理后台 - 制度签收")
@RestController
@RequestMapping("/portal/handbook")
@Validated
@RequiredArgsConstructor
public class PortalHandbookSignoffController {

    private final PortalHandbookSignoffService signoffService;

    @GetMapping("/documents/options")
    @Operation(summary = "签收统计可选制度（正式快照元数据，不含正文或草稿）")
    @SaCheckPermission(PortalCapability.VIEW_SIGNOFF_STATS)
    public Result<java.util.List<com.cyt.center.module.portal.domain.signoff.PortalSignoffDocumentOption>> documentOptions() {
        return success(signoffService.documentOptions());
    }

    public record NotifyRequest(String document, Long documentId, String version,
            @jakarta.validation.constraints.Size(max=500) String customMessage) {}

    @org.springframework.web.bind.annotation.PostMapping("/notify-unsigned")
    @SaCheckPermission(PortalCapability.VIEW_SIGNOFF_STATS)
    public Result<Map<String, Object>> notifyUnsigned(@jakarta.validation.Valid
            @org.springframework.web.bind.annotation.RequestBody NotifyRequest body) {
        return success(signoffService.notifyUnsigned(
                com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId(),
                body.document(), body.documentId(), body.version(), body.customMessage()));
    }

    @GetMapping("/signoff-stats")
    @Operation(summary = "签收覆盖率 + 未签人员清单（document/document_id 二选一）")
    @SaCheckPermission(PortalCapability.VIEW_SIGNOFF_STATS)
    public Result<Map<String, Object>> signoffStats(
            @Parameter(description = "文档标识（slug 或 legacy key）")
            @RequestParam(value = "document", required = false) String document,
            @Parameter(description = "稳定文档 ID")
            @RequestParam(value = "documentId", required = false) Long documentId,
            @Parameter(description = "版本号（不传 = 当前 published 版本）")
            @RequestParam(value = "version", required = false) String version) {
        return success(signoffService.stats(document, documentId, version));
    }

}
