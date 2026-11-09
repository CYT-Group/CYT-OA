package com.cyt.center.module.hrm.controller.admin.employee.vo.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工字段配置保存 Request VO")
@Data
public class HrmEmployeeFieldConfigSaveReqVO {

    @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "name")
    @NotBlank(message = "字段名称不能为空")
    @Size(max = 40, message = "字段名称不能超过 40 个字符")    private String name;

    @Schema(description = "是否显示", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否显示不能为空")    private Boolean visible;

    @Schema(description = "是否允许员工编辑", example = "true")    private Boolean editable;

}
