package com.cyt.center.module.me.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收到的认可：点赞、表扬、之星提名。发起人和接收人都是 sys_user.id。
 */
@Data
@TableName("recognitions")
public class Recognition {

    public static final String TYPE_LIKE = "like";
    public static final String TYPE_PRAISE = "praise";
    public static final String TYPE_STAR_NOMINATION = "star_nomination";
    /** 里程碑祝福：scope 存里程碑类型，period 存那一天 */
    public static final String TYPE_BLESSING = "blessing";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromId;

    private Long toId;

    private String type;

    /** 认可范围：team、corp、company 等 */
    private String scope;

    private String note;

    /** 归属周期，如 2026-W43、2026-10 */
    private String period;

    private LocalDateTime createdAt;

}
