package com.cyt.center.module.hrm.controller.admin.employee;

import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.hrm.service.employee.info.WecomHrSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/hrm/employee/wecom")
@RequiredArgsConstructor
public class WecomHrSyncController {
    private final WecomHrSyncService service;
    private final CurrentUser currentUser;
    private void authorize() {
        currentUser.requirePermission("hrm:employee:query");
        currentUser.requirePermission("hrm:employee:update");
    }
    @PostMapping("/sync")
    public Result<WecomHrSyncService.Result> sync() { authorize(); return Result.ok(service.sync()); }
    public record Confirm(@NotNull Long employeeId, @NotBlank String field,
                          @Size(max = 10) String currentValue, @NotBlank @Size(max = 10) String upstreamValue) { }
    @PostMapping("/confirm")
    public Result<Integer> confirm(@Valid @RequestBody Confirm command) {
        authorize(); return Result.ok(service.confirm(command.employeeId(), command.field(), command.currentValue(), command.upstreamValue()));
    }
}
