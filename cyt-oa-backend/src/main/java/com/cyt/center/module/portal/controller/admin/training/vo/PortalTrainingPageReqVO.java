package com.cyt.center.module.portal.controller.admin.training.vo;

import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 培训课程分页查询条件（迁自 Portal 的 application/training.py admin_list_courses）。
 *
 * status 默认「不限」（全部状态含 draft）：管理端要看草稿才能发；传了值就只认 draft|published。
 */
@Schema(description = "管理后台 - 培训课程分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalTrainingPageReqVO extends PageParam {

    @Schema(description = "状态：draft / published，不传 = 全部（含草稿）", example = "draft")
    @Size(max = 20, message = "状态不能超过 20 字")
    private String status;

    @Schema(description = "小类名精确匹配（文本列镜像）", example = "亚马逊培训")
    @Size(max = 100, message = "分类不能超过 100 字")
    private String category;

    @Schema(description = "关键词，对标题 / 简介做模糊匹配")
    @Size(max = 100, message = "关键词不能超过 100 字")
    private String q;

}
