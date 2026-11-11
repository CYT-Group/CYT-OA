package com.cyt.center.module.portal.controller.app.celebrations;

import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.celebrations.PortalCelebrationCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** 不可猜 token 是单张贺卡的分享凭据；匿名响应不含账号、发送结果、生日年份或员工ID。 */
@RestController
@RequestMapping("/api/portal/celebrations/cards/{token}")
@RequiredArgsConstructor
public class PortalCelebrationCardOpenController {
    private final PortalCelebrationCardService service;
    @GetMapping public Result<Map<String,Object>> get(@PathVariable String token){return Result.success(service.publicCard(token));}
    @GetMapping("/page") public ResponseEntity<String> page(@PathVariable String token){return ResponseEntity.ok().header("Content-Type","text/html;charset=utf-8").header("X-Content-Type-Options","nosniff").header("Cache-Control","no-store").header("Content-Security-Policy","default-src 'none'; style-src 'unsafe-inline'; img-src 'self'; base-uri 'none'; frame-ancestors 'self'").body(service.html(token));}
    @GetMapping("/image") public ResponseEntity<byte[]> image(@PathVariable String token){return ResponseEntity.ok().header("Content-Type","image/jpeg").header("X-Content-Type-Options","nosniff").header("Cache-Control","private, max-age=3600").body(service.publicImage(token));}
}
