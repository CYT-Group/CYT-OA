package com.cyt.center.module.system.api.logger.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 操作日志分页 Request DTO
 *
 * @author HUIHUI
 */
@Data
@Accessors(chain = true)
public class OperateLogPageReqDTO {

    /**
     * 每页条数 - 不分页
     */
    public static final Integer PAGE_SIZE_NONE = -1;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNo = 1;
    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 模块类型
     */
    private String type;
    /**
     * 模块数据编号
     */
    private Long bizId;

    /**
     * 用户编号
     */
    private Long userId;

}
