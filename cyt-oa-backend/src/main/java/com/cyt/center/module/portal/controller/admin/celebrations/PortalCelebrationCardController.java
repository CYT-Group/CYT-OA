package com.cyt.center.module.portal.controller.admin.celebrations;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyt.center.core.result.Result;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.portal.service.celebrations.PortalCelebrationCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static com.cyt.center.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/portal/celebrations/cards")
@SaCheckPermission("can_manage_celebrations")
@RequiredArgsConstructor
public class PortalCelebrationCardController {
    private final PortalCelebrationCardService service;
    public record Create(Long userId,@Size(max=100) String recipientName,@NotBlank String kind,@NotBlank @Size(max=100) String title,
                         @NotBlank @Size(max=500) String message,@Min(1) @Max(100) Integer years,@NotBlank String format,boolean send,@Size(max=4194404) String avatarData) {}
    @GetMapping public Result<PageResult<Map<String,Object>>> page(@RequestParam(defaultValue="1") int pageNo,@RequestParam(defaultValue="20") int pageSize){return Result.success(service.page(pageNo,pageSize));}
    @PostMapping public Result<Map<String,Object>> create(@Valid @RequestBody Create r){return Result.success(service.create(getLoginUserId(),r.userId,r.recipientName,r.kind,r.title,r.message,r.years,r.format,r.send,r.avatarData));}
    @PostMapping("/{id}/send") public Result<Map<String,Object>> send(@PathVariable long id){return Result.success(service.send(getLoginUserId(),id));}
    @GetMapping("/{id}/image") public ResponseEntity<byte[]> image(@PathVariable long id){return ResponseEntity.ok().header("Content-Type","image/jpeg").header("Cache-Control","no-store").body(service.image(id));}
}
