package com.cyt.center.module.portal.controller.admin.news.vo;

import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 动态分页查询条件。
 *
 * status 默认 published 而不是「全部」：与 Portal 一致，非 published 的可见范围还要按能力收敛，
 * 不给一个「不带 status 就把别人草稿全捞出来」的默认口子。
 */
@Schema(description = "管理后台 - 公司动态分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalNewsPageReqVO extends PageParam {

    @Schema(description = "状态：draft / pending / published / rejected，all = 不限（控制台用）", example = "published")
    @Size(max = 20, message = "状态不能超过 20 字")
    private String status = "published";

    @Schema(description = "文本分类精确匹配", example = "company")
    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    @Schema(description = "关键词，对标题 / 摘要 / 正文做模糊匹配")
    @Size(max = 100, message = "关键词不能超过 100 字")
    private String q;

}
