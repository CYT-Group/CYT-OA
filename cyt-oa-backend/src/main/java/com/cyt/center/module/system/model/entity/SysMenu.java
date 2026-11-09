package com.cyt.center.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 菜单与按钮。权限串沿用芋道 oa:leave:create 风格，移植过来的代码可以直接对上。 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private String type;

    private String path;

    private String permission;

    private Integer sortOrder;

    private Boolean visible;

}
