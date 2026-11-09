package com.cyt.center.module.portal.controller.admin.analytics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 活跃用户排行行。
 *
 * 姓名与部门取的是埋点行里存的历史快照（MAX 聚合），不 join 通讯录：
 * 一个人改了名或换了部门，历史统计仍按当时的事实呈现。
 * lastActive 是 SQL 里 DATE_FORMAT 出来的串，不是时间戳——聚合列的类型走 Map 结果，
 * 只有格式化成字符串才不会撞上 JDBC 到底还回 Timestamp 的口径问题。
 */
@Schema(description = "管理后台 - 门户活跃用户 Response VO")
@Data
public class PortalActiveUserRespVO {

    @Schema(description = "账号（企微 userid）", example = "zhangsan")
    private String username;

    @Schema(description = "姓名快照")
    private String displayName;

    @Schema(description = "主部门名快照")
    private String department;

    @Schema(description = "浏览次数")
    private Long pageViews;

    @Schema(description = "访问过的页面数（去重）")
    private Long uniquePages;

    @Schema(description = "最近一次浏览时间", example = "2026-11-05 18:30:12")
    private String lastActive;

}
