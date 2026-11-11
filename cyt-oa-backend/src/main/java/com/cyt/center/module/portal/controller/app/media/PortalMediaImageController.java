package com.cyt.center.module.portal.controller.app.media;

import com.cyt.center.module.portal.service.support.PortalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** UUID 不可枚举的内容图片，供文章正文和封面直接引用。只读白名单图片，绝不提供 HTML/简历目录。 */
@RestController
@RequestMapping("/api/portal/media/images")
@RequiredArgsConstructor
public class PortalMediaImageController {
    private final PortalImageStorage storage;
    @GetMapping("/{name}") public ResponseEntity<byte[]> get(@PathVariable String name) {
        return ResponseEntity.ok().header("Content-Type",storage.mime(name)).header("X-Content-Type-Options","nosniff")
                .header("Cache-Control","public, max-age=31536000, immutable").body(storage.read(name));
    }
}
