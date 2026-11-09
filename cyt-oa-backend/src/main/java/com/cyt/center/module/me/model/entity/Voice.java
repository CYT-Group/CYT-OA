package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我要发声。anonymous=1 时只对读者隐去姓名，作者编号仍然留存，本人才能在「我的发声」里找回自己提的那条。
 */
@Data
@TableName("voices")
public class Voice {

    public static final String CHANNEL_SUGGESTION = "suggestion";
    public static final String CHANNEL_CEO = "ceo";
    public static final String CHANNEL_SURVEY = "survey";

    public static final String STATUS_OPEN = "open";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String channel;

    private Boolean anonymous;

    private String content;

    private String status;

    private LocalDateTime createdAt;

}
