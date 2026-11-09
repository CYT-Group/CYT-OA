package com.cyt.center.module.portal.dal.dataobject.news;

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
 * 公司动态（迁自 Portal 的 portal_news）。
 *
 * 与 OA 系统公告 oa_announcement 是两套东西：公告没有状态机，动态走
 * draft → pending → published/rejected 审批，还带模板 blocks 与企微推送意图，
 * 判定口径见 docs/Portal管理面迁移契约.md §一「内容管理」。
 *
 * 作者用 username（企微 userid）而不是 sys_user.id：账号注销后历史稿仍要能显示是谁写的。
 *
 * 下面标了 updateStrategy = ALWAYS 的列就是 PUT 接口「全量覆盖」的那批：这些列要能显式写
 * NULL（清空摘要 / 换封面为无 / 驳回稿退回未发布），默认的 NOT_NULL 策略会把 NULL 从
 * UPDATE 语句里省掉，改回去等于什么都没改。因此更新必须先读出整行再回写。
 */
@Data
@TableName(value = "portal_news", autoResultMap = true)
public class PortalNewsDO {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_REJECTED = "rejected";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String summary;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String content;

    /** 文本分类，与 portal_categories 无外键：动态的分类口径独立于物资/培训 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String category;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverImage;

    /** simple 或模板类型，模板特有字段在 blocks 里 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String templateType;

    /** 模板数据块，前端会读 blocks.dept 等字段 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private Map<String, Object> blocks;

    /** 作者展示名快照 */
    private String author;

    private String authorUsername;

    private String status;

    /** 精选，前端兼作置顶展示 */
    @TableField(value = "is_featured", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean isFeatured;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime publishedAt;

    /** 作者提交的推送意图，审核人审批通过时二次确认并一次性消费（实际推送在 B8 接入） */
    private Boolean pendingNotifyWecom;

    /** 推送部门范围，null = 全员 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> pendingNotifyDeptScope;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
