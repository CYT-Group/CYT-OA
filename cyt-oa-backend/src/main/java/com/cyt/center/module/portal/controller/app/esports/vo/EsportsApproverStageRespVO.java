package com.cyt.center.module.portal.controller.app.esports.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电竞室开放规则里的一个审批环节（含已解析的审批人），迁自 Portal 的 _settings_row 的 approval_stages。
 */
@Schema(description = "门户 - 电竞室审批环节 Response VO")
@Data
public class EsportsApproverStageRespVO {

    private List<Long> approverUserIds;

    private List<EsportsApproverEntryRespVO> approvers;

}
