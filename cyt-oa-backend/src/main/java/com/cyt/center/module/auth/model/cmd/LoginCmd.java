package com.cyt.center.module.auth.model.cmd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 口令登录入参，只在本机联调开启口令登录时有效。
 */
@Data
public class LoginCmd {

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String password;

    /** front 个人中心 / admin 管理控制台 / auto 按角色自动落到对应入口 */
    private String device = "auto";

}
