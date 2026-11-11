package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量发布 / 批量下架回包：成功数键名随动作（发布 = published、下架 = unpublished），
 * 发不了的进 skipped 带原因（no_lessons / already_published / already_draft / not_found）。
 */
@Schema(description = "管理后台 - 培训课程批量操作 Response VO")
@Data
public class PortalTrainingBatchResultVO {

    @Schema(description = "实发门数（批量发布）")
    private Integer published;

    @Schema(description = "实下门数（批量下架）")
    private Integer unpublished;

    @Schema(description = "跳过的课程")
    private List<Skipped> skipped;

    @Schema(description = "跳过明细")
    @Data
    public static class Skipped {

        @Schema(description = "课程编号")
        private Long id;

        @Schema(description = "课程标题；not_found 时为 null")
        private String title;

        @Schema(description = "跳过原因")
        private String reason;

    }

}
