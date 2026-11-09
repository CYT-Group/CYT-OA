package com.cyt.center.module.system.dal.dataobject.user;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * 移植兼容用的类型占位：芋道 oa / hrm 模块的字段注释里大量 {@code @link AdminUserDO#getId()}，
 * 包名改掉后需要这里有个同名类型可对上。
 *
 * 它不是映射实体（sys_user 没有对应的 system_users 表），读写账号一律走
 * {@link com.cyt.center.module.system.api.user.AdminUserApi} 或 SysUserMapper。
 */
@Data
@Accessors(chain = true)
public class AdminUserDO {

    /**
     * 用户ID
     */
    private Long id;
    /**
     * 用户账号
     */
    private String username;
    /**
     * 用户昵称，对应 sys_user.name
     */
    private String nickname;
    /**
     * 备注
     */
    private String remark;
    /**
     * 部门ID，对应 sys_user.main_dept_id
     */
    private Long deptId;
    /**
     * 岗位编号数组，新库无岗位主数据
     */
    private Set<Long> postIds;
    /**
     * 用户邮箱
     */
    private String email;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 用户性别，枚举 {@link com.cyt.center.module.system.enums.common.SexEnum}
     */
    private Integer sex;
    /**
     * 用户头像
     */
    private String avatar;
    /**
     * 帐号状态，芋道 CommonStatusEnum 口径：0 开启、1 停用
     */
    private Integer status;

}
