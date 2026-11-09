package com.cyt.center.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyt.center.module.system.model.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("""
            SELECT DISTINCT m.permission FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.permission <> ''
            """)
    List<String> selectPermissions(@Param("userId") Long userId);

}
