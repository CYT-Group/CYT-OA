package com.cyt.center.module.portal.dal.dataobject.category;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 全门户共用的分类字典行（迁自 Portal 的 portal_categories）。
 *
 * 一张表服务多个业务，靠 scope 认归属；形状、名长、attrs 白名单与 code 规则
 * 都在 {@link com.cyt.center.module.portal.domain.PortalCategoryScope} 注册表里，
 * 所以 scope 这一列刻意没有 DB CHECK。
 *
 * activeName 是只读生成列，等价于 Postgres 的「在役行才参与唯一」的部分唯一索引，
 * 应用层既不写也不读它。
 *
 * 下面几列的 updateStrategy = ALWAYS 是给「整行回写」用的：摘父、清 code、恢复归档
 * 都要能显式写 NULL，默认的 NOT_NULL 策略会把 null 字段从 UPDATE 语句里省掉、改动凭空丢失。
 * 代价是 updateById 必须传从库里读出来的完整行——拿半份 DO 调它会把没填的列洗成 NULL。
 */
@Data
@TableName(value = "portal_categories", autoResultMap = true)
public class PortalCategoryDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务归属，点号分层：material.sample / material.office / training.course */
    private String scope;

    /** 展示名；物资侧另由注册表限 32 */
    private String name;

    /** 程序锚点（路由 slug / seed 幂等键），普通分类留空 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String code;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;

    /** 1 = 顶层（物资品类、培训大类），2 = 子级（培训小类） */
    private Integer depth;

    /** 同层展示序，再按 name */
    private Integer sortOrder;

    /** 各业务自己的开关与展示字段，键名按 scope 白名单校验 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> attrs;

    /** null = 在役；时间戳本身即审计信息，不用 boolean */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime archivedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String archivedBy;

    /** 创建人 username；迁移 seed 行写 migration */
    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
