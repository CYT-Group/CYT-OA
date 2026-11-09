package com.cyt.center.module.system.model.cmd;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 指定部门负责人。leaderUserid 存企微 userid，留空即取消指定。
 */
@Data
public class DeptLeaderCmd {

    @NotNull(message = "部门不能为空")
    private Long id;

    @Size(max = 64, message = "企微 userid 过长")
    private String leaderUserid;

}
