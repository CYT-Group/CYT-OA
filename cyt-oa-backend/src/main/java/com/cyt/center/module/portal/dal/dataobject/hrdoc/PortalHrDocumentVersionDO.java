package com.cyt.center.module.portal.dal.dataobject.hrdoc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HR 文档发布版本快照（迁自 Portal 的 portal_hr_document_versions）。
 *
 * publish 时写一份，劳动仲裁举证用；只增不改不可删。content_blocks 非 NULL = 块格式版本
 * （此时 content_html 存空串），NULL = 老 HTML 版本。
 */
@Data
@TableName(value = "portal_hr_document_versions", autoResultMap = true)
public class PortalHrDocumentVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    /** 发布时 slug 快照；新文档可为 NULL */
    private String slug;

    private String version;

    private String title;

    private String contentHtml;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Object> contentBlocks;

    private LocalDateTime publishedAt;

    private String publishedBy;

    private String approvedBy;

    private String submittedBy;

}
