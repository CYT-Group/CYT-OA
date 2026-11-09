package com.cyt.center.module.system.api.user.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * Admin 用户 Response DTO
 *
 * 字段名保持芋道口径，取值来自本项目的 sys_user。
 *
 * @author 芋道源码
 */
@Data
@Accessors(chain = true)
public class AdminUserRespDTO {

    /**
     * 用户ID
     */
    private Long id;
    /**
     * 登录账号：企微 userid，与 sys_user.username 同义
     *
     * portal_* 业务表用 username（而不是 user_id）当身份锚点——账号被删后历史行仍读得出是谁，
     * 这是从 Portal 迁过来的口径，所以 API 必须把它暴露出来。
     */
    private String username;
    /**
     * 用户昵称：新库里就是 sys_user.name（企微通讯录姓名）
     */
    private String nickname;
    /**
     * 帐号状态
     *
     * 芋道 CommonStatusEnum 口径：0 开启、1 停用，由 sys_user.enabled 推导。
     */
    private Integer status;

    /**
     * 部门ID：主部门 sys_user.main_dept_id
     */
    private Long deptId;
    /**
     * 岗位编号数组
     *
     * 新库岗位是企微下发的文本（sys_user.position），没有岗位表，恒为空集合。
     */
    private Set<Long> postIds;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 用户性别
     *
     * 枚举 {@link com.cyt.center.module.system.enums.common.SexEnum}
     *
     * 企微通讯录不下发性别、sys_user 也无该列，恒为 null，由业务侧自行补录。
     */
    private Integer sex;
    /**
     * 用户头像
     */
    private String avatar;

}
