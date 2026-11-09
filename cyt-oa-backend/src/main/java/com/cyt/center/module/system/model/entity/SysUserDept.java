package com.cyt.center.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 成员与部门多对多，企微允许一人挂在多个部门下。 */
@Data
@TableName("sys_user_dept")
public class SysUserDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deptId;

}
