package com.cyt.center.module.portal.dal.dataobject.wiki;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档（迁自 Portal 的 portal_wiki）。
 *
 * 状态机与公司动态一致，但没有 published_at 与推送意图列；详情接口自增 viewCount。
 *
 * ALWAYS 的列同 portal_news：PUT 是全量覆盖，正文 / 标签 / 外链这些要能显式清空。
 */
@Data
@TableName(value = "portal_wiki", autoResultMap = true)
public class PortalWikiDO {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_REJECTED = "rejected";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String content;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String category;

    /** 单值归属部门（可见范围口径），不是 portal_categories 的行 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String department;

    /** 外链文档地址，正文可以为空只给链接 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String docUrl;

    /** 标签数组（原 Postgres ARRAY(String)），接口进出都是数组 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> tags;

    private String authorUsername;

    private String status;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
