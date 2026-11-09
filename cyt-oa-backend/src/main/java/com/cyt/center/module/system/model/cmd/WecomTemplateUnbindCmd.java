package com.cyt.center.module.system.model.cmd;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解绑模板别名。只删本地别名，不动企微模板。
 */
@Data
public class WecomTemplateUnbindCmd {

    @NotBlank(message = "业务别名不能为空")
    private String alias;

}
