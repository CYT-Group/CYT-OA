package com.cyt.center.module.portal.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工×课程级进度汇总（迁自 Portal 的 portal_training_progress）。
 *
 * 培训中心重做后，真实进度落在课时级表 PortalTrainingLessonProgress，这张表退为汇总：
 * 任一课时开始 → started；全部课时 completed → 应用层写/更新为 completed。
 * 保留是因为 stats 面板与 legacy 的 start/complete 端点还在读写它。
 *
 * 刻意无 FK（课程删除后进度留存）、无唯一约束（Portal 原表如此，数据可能有重复）。
 */
@Data
@TableName("portal_training_progress")
public class PortalTrainingProgressDO {

    public static final String STATUS_STARTED = "started";
    public static final String STATUS_COMPLETED = "completed";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 无 FK：课程删除后进度留存 */
    private Long trainingId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

}
