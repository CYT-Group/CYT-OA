package com.cyt.center.module.me.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "个人中心 - 同步审批状态 Request VO")
@Data
public class MeApplySyncReqVO {

    @Schema(description = "单据编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单据编号不能为空")
    private Long id;

}
