package com.cyt.center.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志：谁在什么时候把哪条记录改成了什么样。
 *
 * targetTable / beforeValue / afterValue / comment 这四列是给 Portal 迁来的管理面留的：
 * 那边的审计口径是「表名 + 改动前后快照 + 说明」，比芋道的 type/subType/bizId 更贴内容管理，
 * 两套列并存才能让迁移过来的写接口不丢历史。
 */
@Data
@TableName("sys_operate_log")
public class SysOperateLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long userId;

    private Integer userType;

    /** 操作模块类型，如 公司动态 / HR 文档 */
    private String type;

    /** 操作名，如 发布 / 归档 */
    private String subType;

    private Long bizId;

    private String action;

    private String extra;

    private String targetTable;

    private String beforeValue;

    private String afterValue;

    private String comment;

    private String requestMethod;

    private String requestUrl;

    private String userIp;

    private String userAgent;

    private LocalDateTime createdAt;

}
