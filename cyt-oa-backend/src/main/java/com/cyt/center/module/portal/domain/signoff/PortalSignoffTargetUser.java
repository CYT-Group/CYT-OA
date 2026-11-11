package com.cyt.center.module.portal.domain.signoff;

/** 本次签收统计目标人群；department 展示 OA 主部门，纳入范围按全部部门归属判断。 */
public record PortalSignoffTargetUser(Long userId,String username,String displayName,String department){}
