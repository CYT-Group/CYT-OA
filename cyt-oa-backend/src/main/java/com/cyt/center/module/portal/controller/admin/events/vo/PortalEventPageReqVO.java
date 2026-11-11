package com.cyt.center.module.portal.controller.admin.events.vo;

import com.cyt.center.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 展会日历分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PortalEventPageReqVO extends PageParam {

    @Schema(description = "关键词，对标题 / 地点做模糊匹配")
    @Size(max = 100, message = "关键词不能超过 100 字")
    private String q;

}
