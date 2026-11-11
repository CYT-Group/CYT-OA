package com.cyt.center.module.portal.controller.admin.resumes;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.service.resumes.PortalResumeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/resumes")
@RequiredArgsConstructor
@SaCheckPermission("can_manage_resumes")
public class PortalResumeController {
    private final PortalResumeService service;
    public record Id(@NotNull @Positive Long id) {}
    public record Update(@NotNull @Positive Long id,@NotNull Map<String,Object> extracted) {}
    @GetMapping("/page") public Result<PageResult<Map<String,Object>>> page(@RequestParam(required=false) String q,@RequestParam(required=false) String status,@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize) { return Result.success(service.page(q,status,pageNo,pageSize)); }
    @GetMapping("/get") public Result<Map<String,Object>> get(@RequestParam long id) { return Result.success(service.get(id)); }
    @PostMapping("/parse") public Result<Map<String,Object>> parse(@RequestParam MultipartFile file) throws IOException {
        if(file.getSize()>10*1024*1024)throw new BizException("简历不能超过 10MB");
        String name=file.getOriginalFilename();if(name==null||name.isBlank()||name.length()>255)throw new BizException("简历文件名无效");
        return Result.success(service.parse(getLoginUserId(),name,file.getBytes()));
    }
    @PutMapping("/update") public Result<Map<String,Object>> update(@Valid @RequestBody Update r) { return Result.success(service.update(getLoginUserId(),r.id,r.extracted)); }
    @PostMapping("/confirm") public Result<Map<String,Object>> confirm(@Valid @RequestBody Id r) { return Result.success(service.confirm(getLoginUserId(),r.id)); }
    @PostMapping("/reparse") public Result<Map<String,Object>> reparse(@Valid @RequestBody Id r) { return Result.success(service.reparse(getLoginUserId(),r.id)); }
    @DeleteMapping("/delete") public Result<Boolean> delete(@RequestParam long id) { service.delete(getLoginUserId(),id);return Result.success(true); }
    @GetMapping("/file") public ResponseEntity<byte[]> file(@RequestParam long id) {
        var row=service.get(id);String name=(String)row.get("sourceFilename"),ext=(String)row.get("fileExt");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(service.mime(ext)))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").body(service.file(id));
    }
}
