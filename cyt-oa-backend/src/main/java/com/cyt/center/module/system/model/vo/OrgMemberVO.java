package com.cyt.center.module.system.model.vo;

import java.util.List;

/**
 * 组织架构里的成员行。deptNames 是全部归属部门（企微允许一人多部门），mainDeptName 是主部门路径。
 */
public record OrgMemberVO(
        Long userId,
        String wecomUserid,
        String name,
        String position,
        String mobile,
        String email,
        String mainDeptName,
        List<String> deptNames,
        Boolean enabled,
        String jobNumber) {
}
