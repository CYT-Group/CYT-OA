package com.cyt.center.module.portal.controller.admin.training.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * NAS 目录导入回包（对齐 Portal 的 application/training_import.scan_and_import 返回键）。
 */
@Schema(description = "管理后台 - 培训 NAS 目录导入 Response VO")
@Data
public class PortalTrainingImportRespVO {

    @Schema(description = "本次扫描出现的主题（小类）名")
    private List<String> categories;

    @Schema(description = "新建的小类数")
    private Integer categoriesCreated;

    @Schema(description = "新建的课程数")
    private Integer coursesCreated;

    @Schema(description = "扫描到的课程数（含已存在）")
    private Integer coursesSeen;

    @Schema(description = "新建的课时数")
    private Integer lessonsCreated;

    @Schema(description = "刷新的课时数（派生字段与 bucket 不一致）")
    private Integer lessonsUpdated;

    @Schema(description = "无法归入四类而被跳过的对象键")
    private List<String> skipped;

}
