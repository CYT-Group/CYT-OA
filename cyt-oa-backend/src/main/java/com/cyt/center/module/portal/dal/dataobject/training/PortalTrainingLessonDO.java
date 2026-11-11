package com.cyt.center.module.portal.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训课时（迁自 Portal 的 portal_training_lessons）。
 *
 * kind 决定哪几列有值（口径 docs/Portal管理面迁移契约.md §一「培训中心」）：
 * - video / pdf：object_key 是 MinIO 文件键
 * - article：两种载体互斥——object_key 是自包含 output.html 的键（pdf_key 是附带的 PDF，可无），
 *   或 pages 是图片键数组（图片序列型图文）
 * - cards：pages 是图片键数组（按文件名自然序翻页）
 * - link：url 是外部地址（老课程 doc_url/video_url 迁移产物，也可手建）
 * 库里只存对象键不存 URL——MinIO 地址与凭据会变，URL 由后端按需签发预签名。
 */
@Data
@TableName(value = "portal_training_lessons", autoResultMap = true)
public class PortalTrainingLessonDO {

    public static final String KIND_VIDEO = "video";
    public static final String KIND_PDF = "pdf";
    public static final String KIND_ARTICLE = "article";
    public static final String KIND_CARDS = "cards";
    public static final String KIND_LINK = "link";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Integer sortOrder;

    private String title;

    private String kind;

    /** 键名允许中文与空格（bucket 现状），拒绝 ..、以 / 开头、超 500 字节（校验在 service 层） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String objectKey;

    /** html 型 article 附带的 PDF 键，可无；重扫改判载体时可能要清回 NULL */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String pdfKey;

    /** 图片键数组（cards / 图片序列型 article）；cards 改成别的 kind 时显式置 NULL */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> pages;

    private String url;

    /** 视频时长（秒）：导入时拿不到（不解析媒体），播放器首次心跳回报后写入 */
    private Integer durationSec;

    private Long sizeBytes;

    /** 导入幂等键（课时目录/文件键），手建课时为 NULL */
    private String sourcePrefix;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
