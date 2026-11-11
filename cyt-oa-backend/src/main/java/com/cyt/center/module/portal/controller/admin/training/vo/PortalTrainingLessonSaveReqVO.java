package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 手建课时请求体（新建 / 更新共用；迁自 Portal 的 schemas.TrainingAdminLessonCreate/Update）。
 *
 * 新建时 kind 决定哪些对象字段必填（domain/training.validate_lesson_fields）；更新时只动 title / sortOrder
 * （对象键不许改——换文件请删了重建）。
 */
@Schema(description = "管理后台 - 培训课时新建/更新 Request VO")
@Data
public class PortalTrainingLessonSaveReqVO {

    @Schema(description = "课时编号；只在更新时带，新建忽略", example = "18")
    private Long id;

    @Schema(description = "课程编号（新建必填）", example = "7")
    private Long courseId;

    @Schema(description = "标题", example = "第 1 节 公司介绍")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "形态：video / pdf / article / cards / link", example = "video")
    @Size(max = 20, message = "形态不能超过 20 字")
    private String kind;

    @Schema(description = "MinIO 文件键（video / pdf / html 型 article）")
    @Size(max = 500, message = "对象键不能超过 500 字")
    private String objectKey;

    @Schema(description = "附带的 PDF 键（仅 html 型 article）")
    @Size(max = 500, message = "PDF 键不能超过 500 字")
    private String pdfKey;

    @Schema(description = "图片键数组（cards / 图片序列型 article）")
    private List<String> pages;

    @Schema(description = "外链地址（仅 link）")
    @Size(max = 500, message = "地址不能超过 500 字")
    private String url;

    @Schema(description = "排序；不传追加到末尾", example = "1")
    private Integer sortOrder;

}
