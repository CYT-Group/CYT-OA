package com.cyt.center.module.portal.controller.admin.feedback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 员工反馈提交请求体（对齐 Portal 的 FeedbackCreate）。
 */
@Schema(description = "门户 - 员工反馈提交 Request VO")
@Data
public class PortalFeedbackSubmitReqVO {

    @Schema(description = "反馈分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品")
    @NotBlank(message = "分类不能为空")
    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    @Schema(description = "子分类")
    @Size(max = 100, message = "子分类不能超过 100 字")
    private String subCategory;

    @Schema(description = "品牌")
    @Size(max = 50, message = "品牌不能超过 50 字")
    private String brand;

    @Schema(description = "产品名称")
    @Size(max = 200, message = "产品名称不能超过 200 字")
    private String productName;

    @Schema(description = "反馈内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "希望增加导出功能")
    @NotBlank(message = "反馈内容不能为空")
    private String description;

    @Schema(description = "图片地址数组")
    private List<String> imageUrls;

}
