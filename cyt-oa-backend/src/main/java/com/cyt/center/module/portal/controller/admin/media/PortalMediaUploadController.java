package com.cyt.center.module.portal.controller.admin.media;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.result.Result;
import com.cyt.center.module.portal.service.support.PortalImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/portal/media")
@RequiredArgsConstructor
public class PortalMediaUploadController {
    private final PortalImageStorage storage;
    @PostMapping("/upload")
    @SaCheckPermission(value={"can_create_content","can_edit_hr_documents","can_manage_brand_docs","can_manage_training","can_manage_products","can_manage_materials"},mode=SaMode.OR)
    public Result<Map<String,String>> upload(@RequestParam MultipartFile file,@RequestParam(defaultValue="news") String folder) throws IOException {
        if(file.getSize()>20*1024*1024) throw new BizException("图片不能超过 20MB");
        return Result.success(Map.of("url",storage.upload(folder,file.getOriginalFilename(),file.getBytes())));
    }
}
