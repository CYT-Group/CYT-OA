package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工里程碑：生日、入职周年、转正、晋升。日期从人事档案算出来，不靠人工登记。
 */
@Data
@TableName("milestones")
public class Milestone {

    public static final String TYPE_BIRTHDAY = "birthday";
    public static final String TYPE_ANNIVERSARY = "anniversary";
    public static final String TYPE_PROBATION = "probation";
    public static final String TYPE_PROMOTION = "promotion";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    /** 里程碑发生的日期（生日/周年按今年重新计算） */
    private LocalDate milestoneDate;

    private String title;

    private LocalDateTime createdAt;

}
