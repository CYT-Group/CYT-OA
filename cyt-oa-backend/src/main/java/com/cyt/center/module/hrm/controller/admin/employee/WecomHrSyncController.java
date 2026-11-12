package com.cyt.center.module.hrm.controller.admin.employee;

import com.cyt.center.core.result.Result;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.hrm.service.employee.info.WecomHrSyncService;
import com.cyt.center.module.hrm.service.employee.info.WecomRosterSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hrm/employee/wecom")
@RequiredArgsConstructor
public class WecomHrSyncController {
    private final WecomHrSyncService service;
    private final WecomRosterSyncService roster;
    private final CurrentUser currentUser;
    private void authorize() {
        currentUser.requirePermission("hrm:employee:query");
        currentUser.requirePermission("hrm:employee:update");
    }
    @PostMapping("/sync")
    public Result<WecomRosterSyncService.Run> sync() {
        authorize();
        currentUser.requirePermission("system:directory:sync");
        var actor = currentUser.require();
        return Result.ok(roster.start("manual", actor.name() + "#" + actor.userId()));
    }
    @GetMapping("/sync-status")
    public Result<WecomRosterSyncService.Run> status(@RequestParam(name = "runId", required = false) @Size(max = 36) String runId) {
        currentUser.requirePermission("hrm:employee:query");
        return Result.ok(roster.status(runId));
    }
    public record Confirm(@NotNull Long employeeId, @NotBlank String field,
                          @Size(max = 10) String currentValue, @NotBlank @Size(max = 10) String upstreamValue) { }
    @PostMapping("/confirm")
    public Result<Integer> confirm(@Valid @RequestBody Confirm command) {
        authorize(); return Result.ok(service.confirm(command.employeeId(), command.field(), command.currentValue(), command.upstreamValue()));
    }
}
