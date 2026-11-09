package com.cyt.center.module.system.model.cmd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 绑定企微审批模板。templateId 是企微侧的裸模板 ID（绑定只认裸 ID，别名定位用 get）；
 * fieldAliases 是 OA 提交时用的业务键 → 企微控件 ID，网关会先拉模板详情校验。
 */
@Data
public class WecomTemplateBindCmd {

    @NotBlank(message = "企微模板 ID 不能为空")
    private String templateId;

    @NotBlank(message = "业务别名不能为空")
    private String alias;

    private Map<String, String> fieldAliases;

    @Size(max = 255, message = "描述过长")
    private String description;

}
