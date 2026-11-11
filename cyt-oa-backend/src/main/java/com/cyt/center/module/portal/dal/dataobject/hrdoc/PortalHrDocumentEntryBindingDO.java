package com.cyt.center.module.portal.dal.dataobject.hrdoc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HR 文档固定入口绑定（迁自 Portal 的 portal_hr_document_entry_bindings）。
 *
 * 业务入口 key（如 hr/index 页面的固定栏目）→ 稳定文档 ID 的显式绑定。
 */
@Data
@TableName("portal_hr_document_entry_bindings")
public class PortalHrDocumentEntryBindingDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entryKey;

    private Long documentId;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
