package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端课时行（新建 / 更新回包共用）：刻意不带对象键，媒体地址只走签发端点。
 */
@Schema(description = "管理后台 - 培训课时 Response VO")
@Data
public class PortalTrainingLessonRespVO {

    @Schema(description = "编号", example = "1")
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

    @Schema(description = "页数（cards / 图片序列型 article）")
    private Integer pageCount;

    @Schema(description = "外链地址（仅 link）")
    private String url;

}
