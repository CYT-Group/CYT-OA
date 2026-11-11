package com.cyt.center.module.portal.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工×课时的学习进度（迁自 Portal 的 portal_training_lesson_progress）。
 *
 * percent 只增不减（传小值忽略）由应用层保证；完成阈值 video ≥90、其余 ≥100 → completed。
 * 一门课全部课时 completed 时，应用层同步把老课程级表 portal_training_progress 写成 completed。
 * 幂等靠 UNIQUE(username, lesson_id)，不靠先查后插（并发下先查后插会撞唯一索引）。
 */
@Data
@TableName("portal_training_lesson_progress")
public class PortalTrainingLessonProgressDO {

    public static final String STATUS_STARTED = "started";
    public static final String STATUS_COMPLETED = "completed";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private Long lessonId;

    private String status;

    /** 断点：视频秒 / PDF 页 / 卡片索引，允许回退（≥0） */
    private Integer position;

    private Integer percent;

    /** 播放器回报的媒体总时长（秒），同时回填课时的 duration_sec */
    private Integer durationSec;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

}
