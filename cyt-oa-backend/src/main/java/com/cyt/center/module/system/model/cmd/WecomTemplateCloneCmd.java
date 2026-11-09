package com.cyt.center.module.system.model.cmd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 克隆已有模板：企微上按源模板结构新建一张，别名非空时自动绑定新别名。
 */
@Data
public class WecomTemplateCloneCmd {

    @NotBlank(message = "源模板不能为空")
    private String template;

    @NotBlank(message = "新模板名称不能为空")
    private String name;

    private String alias;

    @Size(max = 255, message = "描述过长")
    private String description;

}
