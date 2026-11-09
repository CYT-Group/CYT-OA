package com.cyt.center.module.system.api.dict;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.system.api.dict.dto.DictDataRespDTO;
import com.cyt.center.module.system.mapper.SysDictDataMapper;
import com.cyt.center.module.system.model.entity.SysDictData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典数据 API 实现类：数据源是 sys_dict_data，字典类型名沿用芋道的 hrm_* / oa_* 命名。
 *
 * @author 芋道源码
 */
@Service
@RequiredArgsConstructor
public class DictDataApiImpl implements DictDataApi {

    /** 芋道 CommonStatusEnum 口径：0 开启、1 停用 */
    private static final Integer STATUS_ENABLE = 0;
    private static final Integer STATUS_DISABLE = 1;

    private final SysDictDataMapper dictData;

    @Override
    public List<DictDataRespDTO> getDictDataList(String dictType) {
        List<DictDataRespDTO> result = new ArrayList<>();
        for (SysDictData data : selectByType(dictType)) {
            if (Boolean.TRUE.equals(data.getStatus())) {
                result.add(convert(data));
            }
        }
        return result;
    }

    @Override
    public void validateDictDataList(String dictType, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Map<String, SysDictData> dataMap = new LinkedHashMap<>();
        for (SysDictData data : selectByType(dictType)) {
            dataMap.putIfAbsent(data.getValue(), data);
        }
        for (String value : values) {
            SysDictData data = value == null ? null : dataMap.get(value);
            if (data == null) {
                throw new BizException("字典数据(" + dictType + ":" + value + ")不存在");
            }
            if (!Boolean.TRUE.equals(data.getStatus())) {
                throw new BizException("字典数据(" + data.getLabel() + ")不处于开启状态，不允许选择");
            }
        }
    }

    private List<SysDictData> selectByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return List.of();
        }
        return dictData.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .orderByAsc(SysDictData::getSortOrder)
                .orderByAsc(SysDictData::getId));
    }

    /** sys_dict_data.status 是 boolean（1 启用），芋道侧要的是 CommonStatusEnum 的 0/1，这里统一翻译。 */
    private DictDataRespDTO convert(SysDictData data) {
        DictDataRespDTO dto = new DictDataRespDTO();
        dto.setLabel(data.getLabel());
        dto.setValue(data.getValue());
        dto.setDictType(data.getDictType());
        dto.setStatus(Boolean.TRUE.equals(data.getStatus()) ? STATUS_ENABLE : STATUS_DISABLE);
        return dto;
    }

}
