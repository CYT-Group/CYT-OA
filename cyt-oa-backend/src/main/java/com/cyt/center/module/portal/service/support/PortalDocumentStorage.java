package com.cyt.center.module.portal.service.support;

import com.cyt.center.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 管理文档私有落盘；部署时将 PORTAL_DOCUMENT_ROOT 指向持久卷。文件只能经鉴权接口读取。 */
@Component
public class PortalDocumentStorage {
    public Path root() {
        String configured = System.getenv("PORTAL_DOCUMENT_ROOT");
        return Path.of(configured == null || configured.isBlank() ? "data/portal-documents" : configured)
                .toAbsolutePath().normalize();
    }

    public String store(String folder, String extension, byte[] bytes) {
        String key = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return key;
        } catch (IOException ex) {
            throw new BizException("文档存储失败，请检查持久卷权限和容量");
        }
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException ex) {
            throw new BizException("原文件未迁入 OA 文档存储，请先迁移附件");
        }
    }

    public Path resolve(String key) {
        if (key == null || !key.matches("(?:brand-docs|resumes|images|cards)/[a-zA-Z0-9_-]+\\.[a-z0-9]+")) {
            throw new BizException("无效的文档对象键");
        }
        Path target = root().resolve(key).normalize();
        if (!target.startsWith(root())) throw new BizException("无效的文档路径");
        return target;
    }
}
