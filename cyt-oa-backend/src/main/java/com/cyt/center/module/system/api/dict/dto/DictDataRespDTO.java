package com.cyt.center.module.system.api.dict.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 字典数据 Response DTO
 *
 * @author 芋道源码
 */
@Data
@Accessors(chain = true)
public class DictDataRespDTO {

    /**
     * 字典标签
     */
    private String label;
    /**
     * 字典值
     */
    private String value;
    /**
     * 字典类型
     */
    private String dictType;
    /**
     * 状态
     *
     * 芋道 CommonStatusEnum 口径：0 开启、1 停用，由 sys_dict_data.status 推导。
     */
    private Integer status;

}
