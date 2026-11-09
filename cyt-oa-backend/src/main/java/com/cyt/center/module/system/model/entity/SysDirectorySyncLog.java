package com.cyt.center.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 每次通讯录同步的运行记录，运维按时间倒序看历史结果。 */
@Data
@TableName("sys_directory_sync_log")
public class SysDirectorySyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerType;

    private String actor;

    /** NULL 表示这一趟还在跑。 */
    private Boolean ok;

    private Integer userCreated;

    private Integer userUpdated;

    private Integer departments;

    private Integer conflicts;

    private Integer missing;

    private String message;

    private Long durationMs;

    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

}
