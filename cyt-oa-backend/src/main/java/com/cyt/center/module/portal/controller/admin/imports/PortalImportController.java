package com.cyt.center.module.portal.controller.admin.imports;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.admin.PortalImportService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/portal/import")
@RequiredArgsConstructor
public class PortalImportController {
    private final PortalImportService service;

    @PostMapping
    public Result<Map<String, Object>> run(
            @RequestParam String type,
            @RequestPart MultipartFile file,
            @RequestParam(defaultValue = "true") boolean preview) {
        return Result.ok(service.run(type, file, preview));
    }
}
