package com.cyt.center.module.system.model.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台账号视图：档案字段来自企微，角色来自本地。
 */
public record SysUserVO(
        Long id,
        String wecomUserid,
        String username,
        String name,
        String position,
        Long mainDeptId,
        String mainDeptName,
        String mobile,
        String email,
        String avatar,
        Integer wecomStatus,
        boolean enabled,
        boolean localAccount,
        LocalDateTime lastLoginAt,
        LocalDateTime directorySyncedAt,
        List<String> roles
) {
}
