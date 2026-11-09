package com.cyt.center.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyt.center.module.system.model.entity.SysUserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT r.code FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} ORDER BY r.id")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("SELECT ur.user_id FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE r.code = #{roleCode}")
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);

}
