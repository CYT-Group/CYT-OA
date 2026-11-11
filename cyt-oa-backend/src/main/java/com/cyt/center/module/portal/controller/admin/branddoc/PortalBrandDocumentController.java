package com.cyt.center.module.portal.controller.admin.branddoc;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.service.branddoc.PortalBrandDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/brand-docs")
@RequiredArgsConstructor
@SaCheckPermission("can_manage_brand_docs")
public class PortalBrandDocumentController {
    private final PortalBrandDocumentService service;
    public record Update(@NotNull @Positive Long id,@Size(max=120) String title,String status) {}
    public record Rollback(@NotNull @Positive Long id,@NotNull @Positive Long versionId) {}
    @GetMapping("/page")
    public Result<PageResult<Map<String,Object>>> page(@RequestParam(required=false) String q,@RequestParam(required=false) String brandCode,@RequestParam(required=false) String status,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize) {
        return Result.success(service.page(q,brandCode,status,pageNo,pageSize));
    }
    @PostMapping("/upload")
    public Result<Map<String,Object>> upload(@RequestParam MultipartFile file,@RequestParam String brandCode,@RequestParam String docType,@RequestParam String title) throws IOException {
        if(file.getSize()>20*1024*1024) throw new com.cyt.center.core.exception.BizException("文件大小不能超过 20MB");
        return Result.success(service.upload(getLoginUserId(),brandCode,docType,title,file.getOriginalFilename(),file.getBytes()));
    }
    @PutMapping("/update") public Result<Map<String,Object>> update(@Valid @RequestBody Update r) { return Result.success(service.update(getLoginUserId(),r.id,r.title,r.status)); }
    @GetMapping("/versions") public Result<List<Map<String,Object>>> versions(@RequestParam long id) { return Result.success(service.versions(id)); }
    @PostMapping("/rollback") public Result<Map<String,Object>> rollback(@Valid @RequestBody Rollback r) { return Result.success(service.rollback(getLoginUserId(),r.id,r.versionId)); }
    @DeleteMapping("/delete") public Result<Boolean> delete(@RequestParam long id) { service.delete(getLoginUserId(),id); return Result.success(true); }
    @GetMapping("/file") public ResponseEntity<byte[]> file(@RequestParam long id) {
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=brand-document.html")
                .header("Content-Type","text/html; charset=utf-8").header("X-Content-Type-Options","nosniff")
                .header("Content-Security-Policy","sandbox allow-scripts").header("Cache-Control","no-store").body(service.file(id));
    }
}
