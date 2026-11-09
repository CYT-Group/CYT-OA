package com.cyt.center.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门。主键直接沿用企微部门 ID，通讯录里消失的部门只置 active=false，保留历史归属。
 */
@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long parentId;

    private String name;

    private String path;

    private Long sortOrder;

    private String leaderUserid;

    private Boolean active;

    private LocalDateTime syncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
