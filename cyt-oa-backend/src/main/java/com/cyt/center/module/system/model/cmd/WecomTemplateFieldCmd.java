package com.cyt.center.module.system.model.cmd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模板字段设计器里的一个控件。id 只在更新已有模板时传（保住原控件 ID）；
 * options 是 Selector 的候选项；multiple 决定单选还是多选。
 */
@Data
public class WecomTemplateFieldCmd {

    @NotBlank(message = "字段键不能为空")
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}", message = "字段键须以字母开头，仅含字母数字下划线连字符")
    private String key;

    @NotBlank(message = "字段名不能为空")
    @Size(max = 40, message = "字段名过长")
    private String label;

    @NotBlank(message = "控件类型不能为空")
    private String control;

    private Boolean required;

    private Boolean multiple;

    /** 更新已有模板时带原控件 ID，避免企微侧生成新控件 */
    private String id;

    private List<String> options;

    private Map<String, Object> config;

}
