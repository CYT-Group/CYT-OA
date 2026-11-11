package com.cyt.center.module.portal.dal.dataobject.signoff;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 制度签收记录（迁自 Portal 的 portal_handbook_signoff）。
 *
 * UNIQUE(user_id, document, version)：一人对一版本只签一次（幂等）。
 * document_id 是稳定对账键，document 保留为历史字符串快照（slug）。
 */
@Data
@TableName("portal_handbook_signoff")
public class PortalHandbookSignoffDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private Long documentId;

    private String document;

    private String version;

    private LocalDateTime signedAt;

    private String ip;

    private String userAgent;

}
