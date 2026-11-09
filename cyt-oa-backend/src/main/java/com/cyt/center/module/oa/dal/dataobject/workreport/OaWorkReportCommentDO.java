package com.cyt.center.module.oa.dal.dataobject.workreport;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyt.center.framework.mybatis.core.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作汇报点评：上级写 reviewText，员工用 reply 回复，形成闭环。
 */
@TableName(value = "oa_work_report_comment")
@KeySequence("oa_work_report_comment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class OaWorkReportCommentDO extends BaseDO {

    @TableId
    private Long id;

    /** 汇报编号 {@link OaWorkReportDO#getId()} */
    private Long reportId;

    /** 点评人用户编号 */
    private Long reviewerId;

    /** 评级：A / B / C */
    private String grade;

    private String reviewText;

    /** 员工回复 */
    private String reply;

    private LocalDateTime replyTime;

}
