package com.cyt.center.module.system.api.logger.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统操作日志 Create Request DTO
 *
 * @author HUIHUI
 */
@Data
@Accessors(chain = true)
public class OperateLogCreateReqDTO {

    /**
     * 链路追踪编号
     */
    private String traceId;
    /**
     * 用户编号
     */
    @NotNull(message = "用户编号不能为空")
    private Long userId;
    /**
     * 用户类型
     */
    @NotNull(message = "用户类型不能为空")
    private Integer userType;
    /**
     * 操作模块类型
     */
    @NotEmpty(message = "操作模块类型不能为空")
    private String type;
    /**
     * 操作名
     */
    @NotEmpty(message = "操作名不能为空")
    private String subType;
    /**
     * 操作模块业务编号
     *
     * 可以为空：分类拖拽排序这类「整层」操作没有单一业务编号，硬塞一个 0 反而让日志查不准。
     */
    private Long bizId;
    /**
     * 操作内容
     */
    @NotEmpty(message = "操作内容不能为空")
    private String action;
    /**
     * 拓展字段
     */
    private String extra;

    /**
     * 被改动的表名（Portal 管理面口径，与芋道的 type/subType 并存）
     */
    private String targetTable;
    /**
     * 改动前快照；超长值由调用方压成摘要
     */
    private String beforeValue;
    /**
     * 改动后快照
     */
    private String afterValue;
    /**
     * 操作说明，如审批意见
     */
    private String comment;

    /**
     * 请求方法名
     */
    @NotEmpty(message = "请求方法名不能为空")
    private String requestMethod;
    /**
     * 请求地址
     */
    @NotEmpty(message = "请求地址不能为空")
    private String requestUrl;
    /**
     * 用户 IP
     */
    @NotEmpty(message = "用户 IP 不能为空")
    private String userIp;
    /**
     * 浏览器 UA
     */
    @NotEmpty(message = "浏览器 UA 不能为空")
    private String userAgent;

}
