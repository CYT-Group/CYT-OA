package com.cyt.center.module.portal.controller.admin.hrdoc;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrEditorRepository;
import com.cyt.center.module.portal.service.hrdoc.PortalHrEditorCompatibilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/hrdoc")
@RequiredArgsConstructor
@SaCheckPermission("can_edit_hr_documents")
public class PortalHrEditorCompatibilityController {
    private final PortalHrEditorCompatibilityService service;
    private final PortalHrEditorRepository repository;
    public record Push(String audience,List<String> notifyDeptScope,List<String> userIds,boolean force,@Size(max=300) String customMessage) {}
    public record Preview(@Size(max=200) String title,@Size(max=16) String version,Boolean requiresSignoff) {}

    @GetMapping("/departments") public Result<List<String>> departments(){return Result.success(repository.departments());}
    @GetMapping("/departments/stats") public Result<List<Map<String,Object>>> departmentStats(){return Result.success(repository.departmentStats());}
    @GetMapping("/users/search") public Result<List<Map<String,Object>>> users(@RequestParam(defaultValue="") String q){return Result.success(repository.searchPushUsers(q));}
    @PostMapping("/compat/notify") public Result<Map<String,Object>> push(@RequestParam long id,@Valid @RequestBody Push request){return Result.success(service.notify(getLoginUserId(),id,request.audience,request.notifyDeptScope,request.userIds,request.force,request.customMessage));}
    @PostMapping("/compat/test-push-to-me") public Result<Map<String,Object>> test(@RequestParam long id,@Valid @RequestBody(required=false) Preview request){return Result.success(service.test(getLoginUserId(),id,request==null?null:request.title,request==null?null:request.version,request==null?null:request.requiresSignoff));}
}
