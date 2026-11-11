package com.cyt.center.module.portal.controller.admin.roles;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.admin.PortalRoleService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/portal/roles")
@RequiredArgsConstructor
public class PortalRoleController {
    private final PortalRoleService service;

    public record Save(
            Long id,
            String code,
            @NotBlank @Size(max = 64) String name,
            @Size(max = 255) String remark,
            @NotNull Set<String> permissions) {}

    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        return Result.ok(service.list());
    }

    @PostMapping("/save")
    public Result<Boolean> save(@Valid @RequestBody Save body) {
        service.save(body.id(), body.code(), body.name(), body.remark(), body.permissions());
        return Result.ok(true);
    }

    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam long id) {
        service.delete(id);
        return Result.ok(true);
    }
}
