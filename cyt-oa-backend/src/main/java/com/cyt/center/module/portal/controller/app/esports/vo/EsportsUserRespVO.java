package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电竞室审批人 / 参加人员的最小公开字段（迁自 Portal 的 approvers / participants 返回）。
 *
 * department 对齐 Portal 的 portal_users.department（部门名数组，主部门第 0 位）；
 * OA 侧按主部门名落单元素列表（多部门展示收敛到主部门）。
 */
@Schema(description = "门户 - 电竞室用户最小字段 Response VO")
@Data
public class EsportsUserRespVO {

    private Long id;

    private String username;

    private String displayName;

    private List<String> department;

}
