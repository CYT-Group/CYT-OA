package com.cyt.center.module.system.api.dept.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 部门 Response DTO
 *
 * 字段名与芋道保持一致，移植过来的模块只需改包名即可编译。
 *
 * @author 芋道源码
 */
@Data
@Accessors(chain = true)
public class DeptRespDTO {

    /**
     * 部门编号：新库里就是企微部门 ID（sys_dept.id）
     */
    private Long id;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 父部门编号
     */
    private Long parentId;
    /**
     * 负责人的用户编号
     *
     * 取自 sys_dept.leader_userid（存的是企微 userid），落库前已解析成 sys_user.id；解析不到为 null。
     */
    private Long leaderUserId;
    /**
     * 部门状态
     *
     * 芋道 CommonStatusEnum 口径：0 开启、1 停用，由 sys_dept.active 推导。
     */
    private Integer status;

}
