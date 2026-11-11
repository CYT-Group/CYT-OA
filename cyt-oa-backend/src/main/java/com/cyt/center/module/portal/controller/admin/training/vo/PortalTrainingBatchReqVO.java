package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量发布 / 批量下架课程共用的入参（迁自 Portal 的 schemas.TrainingBatchPublishIn）：
 * ids 1~200 个，空数组与超限直接 422。
 */
@Schema(description = "管理后台 - 培训课程批量发布/下架 Request VO")
@Data
public class PortalTrainingBatchReqVO {

    @Schema(description = "课程编号数组（1~200）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "ids 不能为空")
    @Size(max = 200, message = "一次最多处理 200 门课程")
    private List<Long> ids;

}
