package com.cyt.center.module.portal.controller.admin.celebrations;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.service.celebrations.PortalCelebrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/celebrations")
@RequiredArgsConstructor
@SaCheckPermission("can_manage_celebrations")
public class PortalCelebrationController {
    private final PortalCelebrationService service;
    public record Settings(boolean enabled,@Min(0) @Max(23) int sendHour,@Min(0) @Max(59) int sendMinute,boolean birthdayEnabled,boolean birthdayCardEnabled,Map<String,String> templates) {}
    public record Send(@NotNull @Positive Long userId,@NotBlank String kind,@NotBlank @Size(max=100) String title,@NotBlank @Size(max=2000) String message) {}
    public record Preference(@NotNull @Positive Long userId,boolean optOut) {}
    public record Employee(@NotNull @Positive Long userId,String birthday,String hireDate,boolean optOut) {}
    @GetMapping("/calendar") public Result<List<Map<String,Object>>> calendar(@RequestParam String month) { return Result.success(service.calendar(month)); }
    @GetMapping("/employees") public Result<List<Map<String,Object>>> employees(@RequestParam(required=false) String q) { return Result.success(service.employees(q)); }
    @GetMapping("/settings") public Result<Map<String,Object>> settings() { return Result.success(service.settings()); }
    @PutMapping("/settings") public Result<Map<String,Object>> settings(@Valid @RequestBody Settings r) { return Result.success(service.settings(getLoginUserId(),r.enabled,r.sendHour,r.sendMinute,r.birthdayEnabled,r.birthdayCardEnabled,r.templates)); }
    @PutMapping("/preference") public Result<Boolean> preference(@Valid @RequestBody Preference r) { service.preference(getLoginUserId(),r.userId,r.optOut);return Result.success(true); }
    @PutMapping("/employee") public Result<Map<String,Object>> employee(@Valid @RequestBody Employee r) { return Result.success(service.employee(getLoginUserId(),r.userId,r.birthday,r.hireDate,r.optOut)); }
    @PostMapping("/send") public Result<Map<String,Object>> send(@Valid @RequestBody Send r) { return Result.success(service.send(getLoginUserId(),r.userId,r.kind,r.title,r.message)); }
    @GetMapping("/push-logs") public Result<PageResult<Map<String,Object>>> logs(@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize) { return Result.success(service.logs(pageNo,pageSize)); }
    @PostMapping("/run-now") public Result<Map<String,Object>> run() { return Result.success(service.runDue(true)); }
}
