package com.cyt.center.module.portal.dal.dataobject.esports;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电竞预约审批快照（迁自 Portal 的 portal_esports_approval_steps）。
 *
 * 每单每环节审批人；同环节任一通过即推进。acted_by_* 记录实际代审人（同环节他人可代审），
 * 与指定审批人 approver_user_id 分开。
 */
@Data
@TableName("portal_esports_approval_steps")
public class PortalEsportsApprovalStepDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private Integer stageIndex;

    private Long approverUserId;

    private String approverName;

    private String status;

    private Long actedByUserId;

    private String actedByName;

    private LocalDateTime actedAt;

    private String remark;

}
