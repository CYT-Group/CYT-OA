package com.cyt.center.module.portal.dal.dataobject.hrdoc;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * HR 文档 CMS 主表（迁自 Portal 的 portal_hr_documents）。
 *
 * 草稿/发布双列：content_* 是员工读的已发布快照，draft_* 是 admin 改稿区；publish 把 draft_* 晋升为
 * 正式列后清空。双格式判别：content_blocks 非 NULL = 块格式（此时 content_html 存空串），NULL = 老 HTML 格式。
 *
 * 下面标了 updateStrategy = ALWAYS 的列就是「能显式清空 / 写 NULL」的那批：所有 draft_*、content_blocks、
 * dept_scope、tags 与下架/删除标记。默认的 NOT_NULL 策略会把 NULL 从 UPDATE 里省掉，清空等于没改；
 * 因此更新必须先 selectById 读整行再回写（服务层已保证）。
 */
@Data
@TableName(value = "portal_hr_documents", autoResultMap = true)
public class PortalHrDocumentDO {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_REJECTED = "rejected";

    /** 一级分类：handbook|incentive|hr-sop|hr|policy */
    public static final String CATEGORY_HR_SOP = "hr-sop";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 旧 URL 兼容身份；新文档不分配。删除不释放 slug */
    private String slug;

    private String title;

    private String category;

    /** 二级分类（制度中心 6 主题）；NULL = 存量回落 admin */
    private String topic;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String contentHtml;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftContent;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Object> contentBlocks;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Object> draftBlocks;

    private String version;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftVersion;

    private String status;

    private Boolean requiresSignoff;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> deptScope;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftTitle;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean draftRequiresSignoff;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> draftDeptScope;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String intro;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftIntro;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverImage;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftCoverImage;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String theme;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftTheme;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> tags;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> draftTags;

    private Boolean tocEnabled;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean draftTocEnabled;

    private String tocPosition;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String draftTocPosition;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private Map<String, Object> cover;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private Map<String, Object> draftCover;

    private Boolean downloadPdfEnabled;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean draftDownloadPdfEnabled;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private Map<String, Object> signoff;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private Map<String, Object> draftSignoff;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String updatedBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime updatedAt;

    private String submittedBy;

    private LocalDateTime submittedAt;

    private String publishedBy;

    private LocalDateTime publishedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime offlineAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String offlineBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String offlineReason;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime deletedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String deletedBy;

    private LocalDateTime createdAt;

    /** 是否已发布过快照（判据用 published_at 而非 status，改稿会切回 draft 但保留它） */
    public boolean publishedBefore() {
        return publishedAt != null;
    }

    /** 是否处于下架状态 */
    public boolean isOffline() {
        return offlineAt != null;
    }

    /** 是否已进回收站 */
    public boolean isDeleted() {
        return deletedAt != null;
    }

}
