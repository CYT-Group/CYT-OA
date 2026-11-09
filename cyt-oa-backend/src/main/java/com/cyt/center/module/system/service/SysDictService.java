package com.cyt.center.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.module.system.mapper.SysDictDataMapper;
import com.cyt.center.module.system.mapper.SysDictTypeMapper;
import com.cyt.center.module.system.model.entity.SysDictData;
import com.cyt.center.module.system.model.entity.SysDictType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据字典。移植芋道页面时沿用其 hrm_* / oa_* 类型名，前端下拉与状态文案只认这一份来源。
 */
@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictTypeMapper dictTypes;
    private final SysDictDataMapper dictData;

    public List<SysDictType> types() {
        return dictTypes.selectList(new LambdaQueryWrapper<SysDictType>().orderByAsc(SysDictType::getId));
    }

    public List<SysDictData> data(String type) {
        return dictData.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, type)
                .eq(SysDictData::getStatus, true)
                .orderByAsc(SysDictData::getSortOrder)
                .orderByAsc(SysDictData::getId));
    }

}
