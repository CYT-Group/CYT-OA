package com.cyt.center.module.portal.dal.dataobject.training;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 培训课程（迁自 Portal 的 portal_training）。
 *
 * 老审批流（pending/rejected）已作废，status 只剩 draft|published（口径 docs/Portal管理面迁移契约.md §一「培训中心」）；
 * 发布要求课程至少有一个课时。分类一步切后，小类的事实源是 category_id → portal_categories(scope='training.course',
 * depth=2)，category 文本列只作过渡期镜像（S3a 停写、S3b 删列）。
 *
 * 身份列保留 username（企微 userid）：Portal 的作者按名认人，账号注销后历史行仍读得出是谁。
 *
 * 下面标了 updateStrategy = ALWAYS 的列就是 PATCH 语义下「能显式清空」的那批：description / cover_image /
 * instructor / dept_scope / sort_order 都要能写回 NULL 或 0（清空简介、换封面为无、清空部门范围、排序归零），
 * 默认的 NOT_NULL 策略会把 NULL 从 UPDATE 语句里省掉，清空等于没改。因此更新必须先读出整行再回写。
 */
@Data
@TableName(value = "portal_training", autoResultMap = true)
public class PortalTrainingDO {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /** 老列：早期按单一部门字符串归属，新口径用 dept_scope，此列不再写（只在 legacy 响应里读） */
    private String department;

    /** required（必修）| elective（选修）；老值 optional/入门等已在迁移时归一到 elective */
    private String level;

    private Integer durationMinutes;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String instructor;

    /** 老列：老课程资料外链，重做后已各迁成一条 link 课时，不再写 */
    private String docUrl;

    /** 老列：同上 */
    private String videoUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverImage;

    private String status;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer sortOrder;

    /** 小类名的过渡期镜像；事实源是 categoryId */
    private String category;

    /** 小类 → portal_categories(scope='training.course', depth=2)；NULL = 员工端「未分类」兜底 */
    private Long categoryId;

    /** 导入幂等键（bucket 课程目录前缀），手建课程为 NULL */
    private String sourcePrefix;

    /** 可见部门名单（部门名），NULL = 全员可见 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> deptScope;

    /** 手建课程的创建人；导入课程为 NULL */
    private String authorUsername;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
