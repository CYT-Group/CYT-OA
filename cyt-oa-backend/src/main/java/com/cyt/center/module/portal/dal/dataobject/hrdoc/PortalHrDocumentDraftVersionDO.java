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
 * HR 文档草稿版本快照（迁自 Portal 的 portal_hr_document_draft_versions）。
 *
 * admin 每次保存草稿自动存一份，可回看/恢复/删除（每文档只留最新 30 条）。content_blocks 非 NULL
 * = 块格式快照（判别同主表）。
 */
@Data
@TableName(value = "portal_hr_document_draft_versions", autoResultMap = true)
public class PortalHrDocumentDraftVersionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String slug;

    private String title;

    private String contentHtml;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Object> contentBlocks;

    private String savedBy;

    private LocalDateTime createdAt;

}
