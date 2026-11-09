package com.cyt.center.module.portal.dal.dataobject.pageview;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 页面浏览记录（迁自 Portal 的 portal_page_views）。
 *
 * username / displayName / department 是打点那一刻的快照，不建外键：
 * 账号改名或停用后，历史统计的口径不能跟着变形。看板按这三列聚合，
 * 所以 department 只存主部门单值。
 */
@Data
@TableName("portal_page_views")
public class PortalPageViewDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String pageUrl;

    private String pageTitle;

    /** null = 匿名访问；UV 类指标一律排除匿名 */
    private String username;

    private String displayName;

    private String department;

    private String referrer;

    private String userAgent;

    private LocalDateTime viewedAt;

}
