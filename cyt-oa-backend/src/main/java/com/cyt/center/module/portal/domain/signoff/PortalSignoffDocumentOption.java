package com.cyt.center.module.portal.domain.signoff;

import java.time.LocalDateTime;

/** 签收统计专用白名单读模型；不可携带文档正文、草稿或编辑设置。 */
public record PortalSignoffDocumentOption(Long id, String title, String version,
        Boolean requiresSignoff, LocalDateTime publishedAt, LocalDateTime offlineAt) {
}
