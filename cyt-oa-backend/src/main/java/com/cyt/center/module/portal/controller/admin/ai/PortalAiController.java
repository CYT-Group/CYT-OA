package com.cyt.center.module.portal.controller.admin.ai;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.admin.PortalAiService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/portal/ai")
@RequiredArgsConstructor
public class PortalAiController {
    private final PortalAiService service;

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.ok(service.config());
    }

    @PostMapping("/tasks/save")
    public Result<Boolean> task(@RequestBody Map<String, Object> b) {
        service.saveTask(b);
        return Result.ok(true);
    }

    @GetMapping("/tasks/versions")
    public Result<List<Map<String, Object>>> versions(@RequestParam String taskName) {
        return Result.ok(service.versions(taskName));
    }

    @PostMapping("/tasks/restore")
    public Result<Boolean> restore(@RequestBody Restore b) {
        service.restore(b.taskName(), b.version());
        return Result.ok(true);
    }

    public record Restore(String taskName, long version) {}

    @PostMapping("/models/save")
    public Result<Boolean> model(@RequestBody Map<String, Object> b) {
        service.saveModel(b);
        return Result.ok(true);
    }

    @PostMapping("/models/sync")
    public Result<Map<String, Object>> sync() {
        return Result.ok(service.syncModels());
    }

    @DeleteMapping("/models/delete")
    public Result<Boolean> delete(@RequestParam long id) {
        service.deleteModel(id);
        return Result.ok(true);
    }

    @PutMapping("/provider")
    public Result<Boolean> provider(@RequestBody Map<String, Object> b) {
        service.saveProvider(b);
        return Result.ok(true);
    }

    public record Key(String key) {}

    @PutMapping("/key")
    public Result<Boolean> key(@RequestBody Key b) {
        service.saveKey(b.key());
        return Result.ok(true);
    }

    @DeleteMapping("/key")
    public Result<Boolean> clear() {
        service.clearKey();
        return Result.ok(true);
    }

    @PostMapping("/test")
    public Result<String> test(@RequestBody Map<String, Object> b) {
        return Result.ok(service.test(b));
    }

    public record Text(String text, String kind) {}

    @PostMapping("/polish")
    public Result<String> polish(@RequestBody Text b) {
        return Result.ok(service.polish(b.text(), b.kind()));
    }

    @PostMapping("/layout")
    public Result<Map<String, Object>> layout(@RequestBody Text b) {
        return Result.ok(service.layout(b.text()));
    }

    @GetMapping("/costs")
    public Result<Map<String, Object>> costs(@RequestParam String month) {
        return Result.ok(service.costs(month));
    }
}
