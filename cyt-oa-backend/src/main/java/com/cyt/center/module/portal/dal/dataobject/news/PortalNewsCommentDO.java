package com.cyt.center.module.portal.dal.dataobject.news;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态评论，楼中楼用 parentId 表达（服务端返回平铺数组，前端自己组装）。
 *
 * 删除是软删：isDeleted 置真、content 换成占位文案，列表不返回软删行，
 * 所以占位文案只存在于库里，对外表现为评论数变少。
 */
@Data
@TableName("portal_news_comment")
public class PortalNewsCommentDO {

    public static final String DELETED_PLACEHOLDER = "[该评论已删除]";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long newsId;

    private String username;

    private String displayName;

    private String avatarUrl;

    private String content;

    private Long parentId;

    @TableField("is_deleted")
    private Boolean isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
