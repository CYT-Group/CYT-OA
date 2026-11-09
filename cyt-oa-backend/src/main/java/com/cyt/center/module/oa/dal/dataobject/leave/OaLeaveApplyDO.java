package com.cyt.center.module.oa.dal.dataobject.leave;

import com.cyt.center.framework.mybatis.core.dataobject.BaseDO;

import com.cyt.center.framework.mybatis.core.dataobject.BaseDO;
import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.enums.DictTypeConstants;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OA 请假申请 DO
 *
 * 申请人使用 {@link BaseDO#getCreator()}，申请时间使用 {@link BaseDO#getCreateTime()}
 *
 * @author 芋道源码
 */
@TableName(value = "oa_leave_apply", autoResultMap = true)
@KeySequence("oa_leave_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaLeaveApplyDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 申请标题
     */
    private String title;
    /**
     * 紧急程度
     *
     * 字典 {@link DictTypeConstants#APPLY_URGENCY}
     */
    private Integer urgency;
    /**
     * 请假类型
     *
     * 字典 {@link DictTypeConstants#LEAVE_TYPE}
     */
    private Integer type;
    /**
     * 请假开始时间
     */
    private LocalDateTime startTime;
    /**
     * 请假结束时间
     */
    private LocalDateTime endTime;
    /**
     * 请假天数
     */
    private Integer days;
    /**
     * 申请原因
     */
    private String reason;
    /**
     * 自选审批人的员工编号，按顺序依次审批；提交时换成企微成员 ID 发起审批
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> approvers;
    /**
     * 审批状态，未开始表示尚未提交的草稿
     *
     * 枚举 {@link BpmProcessInstanceStatusEnum}
     */
    private Integer status;
    /**
     * BPM 流程实例编号
     *
     * 关联 HistoricProcessInstance#getId()
     */
    private String processInstanceId;
    /**
     * 企微审批单号（sp_no），由 common-api 网关返回
     */
    private String wecomApprovalNo;
    /**
     * 提交到企微时用的幂等请求号，一个申请固定一个，重复提交与回读都靠它
     */
    private String wecomRequestId;
    /**
     * 附件地址列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> fileUrls;

}
