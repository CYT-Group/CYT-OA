package com.cyt.center.module.system.api.dict;

import com.cyt.center.module.system.api.dict.dto.DictDataRespDTO;

import java.util.Collection;
import java.util.List;

/**
 * 字典数据 API 接口
 *
 * @author 芋道源码
 */
public interface DictDataApi {

    /**
     * 获得指定字典类型的字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<DictDataRespDTO> getDictDataList(String dictType);

    /**
     * 校验字典数据们是否有效。如下情况，视为无效：
     * 1. 字典数据不存在
     * 2. 字典数据被禁用
     *
     * @param dictType 字典类型
     * @param values 字典数据值的数组
     */
    void validateDictDataList(String dictType, Collection<String> values);

}
