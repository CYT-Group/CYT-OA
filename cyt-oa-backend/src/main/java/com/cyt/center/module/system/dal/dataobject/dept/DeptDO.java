package com.cyt.center.module.system.dal.dataobject.dept;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 移植兼容用的类型占位：芋道 oa / hrm 模块的字段注释里大量 {@code @link DeptDO#getId()}，
 * 包名改掉后需要这里有个同名类型可对上。
 *
 * 它不是映射实体（sys_dept 没有对应的 system_dept 表），读写部门一律走
 * {@link com.cyt.center.module.system.api.dept.DeptApi} 或 SysDeptMapper。
 */
@Data
@Accessors(chain = true)
public class DeptDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 部门ID，即企微部门 ID
     */
    private Long id;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 父部门ID
     */
    private Long parentId;
    /**
     * 显示顺序
     */
    private Integer sort;
    /**
     * 负责人，关联 sys_user.id
     */
    private Long leaderUserId;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 部门状态，芋道 CommonStatusEnum 口径：0 开启、1 停用
     */
    private Integer status;

}
