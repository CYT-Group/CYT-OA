package com.cyt.center.module.portal.dal.dataobject.feedback;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工反馈（迁自 Portal 的 bot.employee_feedback 的员工提交部分）。
 *
 * 员工提交只写 raw 字段（username/reporter_name/department/category/sub_category/brand/product_name/
 * raw_message/image_urls）；sentiment/ai_summary/status 流转/assigned_to/resolved_note 由 bot（AI）富化，
 * 随 bot 模块落地后写入，这里只保留列占位。
 */
@Data
@TableName(value = "portal_feedback", autoResultMap = true)
public class PortalFeedbackDO {

    public static final String STATUS_PENDING = "pending";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交人（企微 userid = Portal 的 wecom_userid） */
    private String username;

    private String reporterName;

    private String department;

    private String category;

    private String subCategory;

    private String brand;

    private String productName;

    private String sentiment;

    private String priority;

    private String rawMessage;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    private String aiSummary;

    private String status;

    private String assignedTo;

    private String resolvedNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
