package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 大促 · 新品 · 公司活动的节奏节点。
 *
 * deptActions 存 JSON 数组 [{"dept":"运营部","items":["会场搭建"]}]，由内容管理侧维护；
 * linkedRefs 存逗号分隔的关联单据或文档编号。
 */
@Data
@TableName("rhythm_nodes")
public class RhythmNode {

    public static final String KIND_SALE = "sale";
    public static final String KIND_NEW = "new";
    public static final String KIND_EVENT = "event";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** sale 大促、new 新品、event 公司活动 */
    private String kind;

    private LocalDate startDate;

    private LocalDate endDate;

    private String deptActions;

    private String note;

    private String linkedRefs;

    private LocalDateTime createdAt;

}
