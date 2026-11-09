package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的提醒。分类固定四类：todo 待我办、milestone 里程碑、compliance 规则提醒、corp 公司动态。
 *
 * dedupKey 是重复同步的幂等键（如 report-2026-W45），同一用户同一键只保留一条；
 * 提醒不是一次性写死的消息，生成逻辑见 MeReminderService。
 */
@Data
@TableName("notifications")
public class Notification {

    public static final String CATEGORY_TODO = "todo";
    public static final String CATEGORY_MILESTONE = "milestone";
    public static final String CATEGORY_COMPLIANCE = "compliance";
    public static final String CATEGORY_CORP = "corp";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 sys_user.id */
    private Long userId;

    private String category;

    /** 具体类型，如 report_due、task_pending、contract_expired */
    private String type;

    private String title;

    /** 一键跳转到对应模块的前端路径 */
    private String link;

    private Boolean readFlag;

    /** site 站内、wecom 企业微信 */
    private String channel;

    private String dedupKey;

    private LocalDateTime createdAt;

}
