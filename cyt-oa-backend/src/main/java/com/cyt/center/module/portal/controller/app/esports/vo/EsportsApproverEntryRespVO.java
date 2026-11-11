package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 电竞室开放规则审批环节内的审批人条目（迁自 Portal 的 _settings_row 的 approvers）。
 *
 * 与 approvers/participants 端点的 EsportsUserRespVO 不同：这里只下发 id / display_name / active。
 */
@Schema(description = "门户 - 电竞室设置审批人条目 Response VO")
@Data
public class EsportsApproverEntryRespVO {

    private Long id;

    private String displayName;

    private Boolean active;

}
