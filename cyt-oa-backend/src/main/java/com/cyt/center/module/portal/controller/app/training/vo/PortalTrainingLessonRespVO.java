package com.cyt.center.module.portal.controller.app.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课时行（迁自 Portal 的 routers/training.py _lesson_row）：刻意不带对象键（object_key/pdf_key/pages），
 * 媒体地址只走 media 端点签发。my 是本人进度，从未打开过为 null。
 */
@Schema(description = "门户 - 培训课时 Response VO")
@Data
public class PortalTrainingLessonRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "形态")
    private String kind;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "视频时长（秒）")
    private Integer durationSec;

    @Schema(description = "字节数")
    private Long sizeBytes;

    @Schema(description = "是否带 PDF")
    private Boolean hasPdf;

    @Schema(description = "页数（cards / 图片序列型 article），无则为 null")
    private Integer pageCount;

    @Schema(description = "外链地址（仅 link）")
    private String url;

    @Schema(description = "本人进度，从未打开过为 null")
    private MyProgress my;

    @Schema(description = "本人课时进度")
    @Data
    public static class MyProgress {

        @Schema(description = "状态：started / completed")
        private String status;

        @Schema(description = "断点位置")
        private Integer position;

        @Schema(description = "进度百分比")
        private Integer percent;

        @Schema(description = "已回报时长（秒）")
        private Integer durationSec;

        @Schema(description = "完成时间")
        private LocalDateTime completedAt;

    }

}
