package com.cyt.center.module.portal.dal.dataobject.hrdoc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HR 文档旧地址别名（迁自 Portal 的 portal_hr_document_aliases）。
 *
 * 历史 slug / key / path → 稳定文档 ID 的兼容映射。alias 唯一；删除文档不释放 slug，别名一并保留。
 */
@Data
@TableName("portal_hr_document_aliases")
public class PortalHrDocumentAliasDO {

    public static final String TYPE_SLUG = "slug";
    public static final String TYPE_LEGACY_KEY = "legacy_key";
    public static final String TYPE_LEGACY_PATH = "legacy_path";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String alias;

    private String aliasType;

    private Long documentId;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
