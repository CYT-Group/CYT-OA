package com.cyt.center.module.system.model.cmd;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色绑定全量覆盖：传空数组即清空该账号的全部角色。
 */
@Data
public class AssignRolesCmd {

    @Size(max = 20, message = "最多绑定 20 个角色")
    private List<String> roles;

}
