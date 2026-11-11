package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 培训课程新建 / 更新请求体（同一个 VO，PUT 是 PATCH 语义）。
 *
 * 与 Portal 的 TrainingAdminCourseCreate/Update 同构：新建 title/category 必填；更新时
 * title/category 传了空就 422（其它字段 null = 不动）。title 走 @NotBlank（无培训专属错误码），
 * category 的「必填」由服务层按 TRAINING_CATEGORY_REQUIRED 判（对齐 Portal 的手工检查）。
 */
@Schema(description = "管理后台 - 培训课程新建/更新 Request VO")
@Data
public class PortalTrainingSaveReqVO {

    @Schema(description = "课程编号；只在更新时带，新建忽略", example = "18")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "亚马逊卖家入门")
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 字")
    private String title;

    @Schema(description = "小类名（事实源是 category_id）；不存在会自动建小类", requiredMode = Schema.RequiredMode.REQUIRED, example = "亚马逊培训")
    @Size(max = 100, message = "分类不能超过 100 字")
    private String category;

    @Schema(description = "级别：required（必修）/ elective（选修）；空 = elective", example = "elective")
    @Size(max = 20, message = "级别不能超过 20 字")
    private String level;

    @Schema(description = "可见部门名 JSON 数组，留空 = 全员", example = "[\"销售部\",\"市场部\"]")
    private List<String> deptScope;

    @Schema(description = "纯文本简介")
    private String description;

    @Schema(description = "封面图地址")
    @Size(max = 255, message = "封面地址不能超过 255 字")
    private String coverImage;

    @Schema(description = "讲师")
    @Size(max = 100, message = "讲师不能超过 100 字")
    private String instructor;

    @Schema(description = "排序", example = "0")
    private Integer sortOrder;

}
