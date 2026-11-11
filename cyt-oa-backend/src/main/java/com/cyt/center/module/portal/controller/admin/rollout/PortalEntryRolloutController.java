package com.cyt.center.module.portal.controller.admin.rollout;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.admin.PortalEntryRolloutService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/portal/rollout")
@RequiredArgsConstructor
public class PortalEntryRolloutController {
    private final PortalEntryRolloutService service;

    public record Save(
            @NotNull Long revision,
            @NotNull @Size(min = 1, max = 64) Map<String, Object> entries) {}

    @GetMapping("/entries")
    public Result<PortalEntryRolloutService.Entries> entries() {
        return Result.ok(service.entries());
    }

    @GetMapping("/settings")
    public Result<PortalEntryRolloutService.Settings> settings() {
        return Result.ok(service.settings());
    }

    @PutMapping("/settings")
    public Result<PortalEntryRolloutService.Settings> save(@Valid @RequestBody Save body) {
        return Result.ok(service.save(body.revision(), body.entries()));
    }
}
