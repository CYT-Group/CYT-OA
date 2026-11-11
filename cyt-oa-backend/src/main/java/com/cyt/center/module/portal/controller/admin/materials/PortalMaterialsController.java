package com.cyt.center.module.portal.controller.admin.materials;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.admin.PortalMaterialsService;
import com.cyt.center.module.portal.service.admin.PortalRows;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/portal/materials")
@RequiredArgsConstructor
public class PortalMaterialsController {
    private final PortalMaterialsService service;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "sample") String kind,
            @RequestParam(defaultValue = "inventory") String resource,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(service.page(kind, resource, q, archived, pageNo, pageSize));
    }

    @GetMapping("/options")
    public Result<Map<String, Object>> options(@RequestParam String kind) {
        return Result.ok(service.options(kind));
    }

    @GetMapping("/apply")
    public Result<Map<String, Object>> detail(@RequestParam long id) {
        return Result.ok(service.detail(id));
    }

    @PostMapping("/products/save")
    public Result<Long> product(@RequestBody Map<String, Object> b) {
        return Result.ok(service.saveProduct(b));
    }

    @PostMapping("/inventory/save")
    public Result<Long> sku(@RequestBody Map<String, Object> b) {
        return Result.ok(service.saveSku(b));
    }

    public record RecordAction(String resource, long id, boolean restore) {}

    @PostMapping("/archive")
    public Result<Boolean> archive(@RequestBody RecordAction b) {
        service.archive(b.resource(), b.id(), b.restore());
        return Result.ok(true);
    }

    @DeleteMapping("/delete")
    public Result<Boolean> delete(@RequestParam String resource, @RequestParam long id) {
        service.delete(resource, id);
        return Result.ok(true);
    }

    public record Owner(String kind, String site, String username) {}

    @PutMapping("/owner")
    public Result<Boolean> owner(@RequestBody Owner b) {
        service.saveOwner(b.kind(), b.site(), b.username());
        return Result.ok(true);
    }

    @PostMapping("/applies/create")
    public Result<Long> create(@RequestBody Map<String, Object> b) {
        return Result.ok(service.createApply(b));
    }

    @PostMapping("/applies/action")
    public Result<Boolean> action(@RequestBody Map<String, Object> b) {
        service.action(PortalRows.number(b.get("id")), PortalRows.text(b, "action", ""), b);
        return Result.ok(true);
    }
}
