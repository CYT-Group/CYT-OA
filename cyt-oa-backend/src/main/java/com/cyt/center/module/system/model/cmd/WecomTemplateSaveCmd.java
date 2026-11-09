package com.cyt.center.module.system.model.cmd;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新建/更新企微审批模板。create 不传 template；update 传 template=别名定位原模板。
 * 字段列表会整体重建模板控件，所以更新时前端要带上原控件的 id。
 */
@Data
public class WecomTemplateSaveCmd {

    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** update 时定位原模板的别名 */
    private String template;

    /** 非空时自动绑定（create 通常必填；update 用 template 里的别名） */
    private String alias;

    @Size(max = 255, message = "描述过长")
    private String description;

    @NotEmpty(message = "模板字段不能为空")
    @Valid
    private List<WecomTemplateFieldCmd> fields;

}
