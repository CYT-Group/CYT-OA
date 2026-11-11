package com.cyt.center.module.portal.controller.admin.push;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.push.PortalContentPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/push")
@RequiredArgsConstructor
public class PortalContentPushController {
    private final PortalContentPushService service;
    @GetMapping("/latest")
    @SaCheckPermission(value={"can_review","can_create_content","can_edit_hr_documents"},mode=SaMode.OR)
    public Result<Map<String,Object>> latest(@RequestParam String trigger,@RequestParam String targetKey){return Result.success(service.latest(getLoginUserId(),trigger,targetKey));}
}
