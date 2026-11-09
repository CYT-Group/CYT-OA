package com.cyt.center.module.hrm.controller.admin.salary.vo.config;

import com.cyt.center.framework.common.validation.InEnum;
import com.cyt.center.module.hrm.enums.salary.config.HrmSalarySocialSecurityMonthTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HRM 计薪配置更新 Request VO")
@Data
public class HrmSalaryConfigUpdateReqVO {

    @Schema(description = "社保对应月份类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "社保对应月份类型不能为空")
    @InEnum(value = HrmSalarySocialSecurityMonthTypeEnum.class, message = "社保对应月份类型必须是 {value}")    private Integer socialSecurityMonthType;

}
