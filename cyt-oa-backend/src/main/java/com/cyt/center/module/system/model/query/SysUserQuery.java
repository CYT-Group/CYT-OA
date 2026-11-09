package com.cyt.center.module.system.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 账号列表查询条件。
 */
@Data
public class SysUserQuery {

    /** 模糊匹配登录名 / 姓名 / 企微 userid */
    private String keyword;

    private Long deptId;

    /** true 只看能登录的账号，false 只看被停用的，为空看全部 */
    private Boolean enabled;

    @Min(value = 1, message = "最小为 1")
    private Integer page = 1;

    @Min(value = 1, message = "最小为 1")
    @Max(value = 100, message = "最大为 100")
    private Integer size = 20;

}
