package com.cyt.center.module.portal.controller.admin.hrdoc;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.hrdoc.PortalHrLayoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/hrdoc")
@SaCheckPermission("can_edit_hr_documents")
@RequiredArgsConstructor
public class PortalHrLayoutController {
    private final PortalHrLayoutService service;
    public record Layout(@NotBlank @Size(max=20000) String text,String sourceType,@Size(max=255) String sourceName){}
    @PostMapping("/layout") public Result<Map<String,Object>> layout(@Valid @RequestBody Layout request){return Result.success(service.synchronous(getLoginUserId(),request.text));}
    @PostMapping("/layout-file") public Result<Map<String,Object>> file(@RequestParam MultipartFile file){return Result.success(service.synchronousFile(getLoginUserId(),file));}
    @PostMapping("/layout/jobs") public Result<Map<String,Object>> create(@Valid @RequestBody Layout request){return Result.success(service.create(getLoginUserId(),request.text,request.sourceType,request.sourceName));}
    @PostMapping("/layout/jobs/file") public Result<Map<String,Object>> createFile(@RequestParam MultipartFile file){return Result.success(service.createFile(getLoginUserId(),file));}
    @GetMapping("/layout/jobs") public Result<List<Map<String,Object>>> list(){return Result.success(service.list(getLoginUserId()));}
    @GetMapping("/layout/jobs/{id}") public Result<Map<String,Object>> get(@PathVariable long id){return Result.success(service.detail(getLoginUserId(),id));}
    @DeleteMapping("/layout/jobs/{id}") public Result<Boolean> delete(@PathVariable long id){service.delete(getLoginUserId(),id);return Result.success(true);}
    @PostMapping("/layout/jobs/{id}/retry") public Result<Map<String,Object>> retry(@PathVariable long id){return Result.success(service.retry(getLoginUserId(),id));}
}
