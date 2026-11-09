package com.cyt.center.module.portal.dal.dataobject.news;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态点赞：每人每动态最多一次。
 *
 * 幂等靠 portal_news_like 的 UNIQUE(news_id, username)，不靠先查后插——
 * 并发重复点赞由数据库挡下，接口仍然返回 201 与正确计数。
 */
@Data
@TableName("portal_news_like")
public class PortalNewsLikeDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long newsId;

    private String username;

    private String displayName;

    private LocalDateTime createdAt;

}
