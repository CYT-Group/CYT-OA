package com.cyt.center.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录账号。姓名、部门等档案字段由企微通讯录覆盖写入，本地只有角色绑定和口令两件事是自己管的。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String wecomUserid;

    private String username;

    private String name;

    private String position;

    private Long mainDeptId;

    private String mobile;

    private String email;

    private String avatar;

    private Integer wecomStatus;

    private Boolean enabled;

    private String passwordHash;

    private LocalDateTime lastLoginAt;

    private LocalDateTime directorySyncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
