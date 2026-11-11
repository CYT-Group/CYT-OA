package com.cyt.center.module.portal.dal.dataobject.products;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 新品动态（迁自 Portal 的 portal_products）。
 *
 * 无审批流：创建即 published，status 只有 published 一个取值。is_featured 精选位
 * 供前端置顶展示，管理端可勾选。没有作者列（Portal 的 products 不记创建人），
 * 写操作由 @SaCheckPermission(can_manage_products) 整端点闸。
 *
 * ALWAYS 的列是 PUT 全量覆盖里能显式清空的那批：brand / category / description /
 * image_url / link_url / launch_date 都能清成 NULL。
 */
@Data
@TableName(value = "portal_products", autoResultMap = true)
public class PortalProductDO {

    public static final String STATUS_PUBLISHED = "published";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String brand;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String category;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String imageUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String linkUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate launchDate;

    private String status;

    /** 精选，前端兼作置顶展示 */
    @TableField(value = "is_featured")
    private Boolean isFeatured;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
