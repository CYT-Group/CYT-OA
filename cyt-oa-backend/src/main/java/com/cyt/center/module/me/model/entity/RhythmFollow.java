package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我关注的节奏节点。remindBefore 控制开始前几天生成一条待办提醒。
 */
@Data
@TableName("rhythm_node_follow")
public class RhythmFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long nodeId;

    private Integer remindBefore;

    private LocalDateTime createdAt;

}
