package com.cyt.center.framework.dict.core;

import cn.hutool.core.collection.CollUtil;
import com.cyt.center.framework.common.util.cache.CacheUtils;
import com.cyt.center.module.system.api.dict.DictDataApi;
import com.cyt.center.module.system.api.dict.dto.DictDataRespDTO;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.cyt.center.framework.common.util.collection.CollectionUtils.convertList;

/**
 * 字典工具类
 *
 * 与芋道原版的差异：
 * 1. 依赖的接口从 framework.common.biz.system.dict.DictDataCommonApi 换成本项目已有的
 *    {@link DictDataApi}（由 system 模块实现），避免再移植一层 common-biz；
 * 2. dictDataApi 未注入时不抛异常，返回空字典，保证未启用字典能力的部署也能启动。
 */
@Slf4j
public class DictFrameworkUtils {

    private static DictDataApi dictDataApi;

    /**
     * 针对 dictType 的字段数据缓存
     */
    private static final LoadingCache<String, List<DictDataRespDTO>> GET_DICT_DATA_CACHE = CacheUtils.buildAsyncReloadingCache(
            Duration.ofMinutes(1L), // 过期时间 1 分钟
            new CacheLoader<String, List<DictDataRespDTO>>() {

                @Override
                public List<DictDataRespDTO> load(String dictType) {
                    // 防御：字典 API 未注入（例如 system 模块未装配）时，按“无字典数据”处理
                    DictDataApi api = dictDataApi;
                    if (api == null) {
                        return Collections.emptyList();
                    }
                    List<DictDataRespDTO> dictDatas = api.getDictDataList(dictType);
                    return dictDatas != null ? dictDatas : Collections.emptyList();
                }

            });

    public static void init(DictDataApi dictDataApi) {
        DictFrameworkUtils.dictDataApi = dictDataApi;
        log.info("[init][初始化 DictFrameworkUtils 成功]");
    }

    public static void clearCache() {
        GET_DICT_DATA_CACHE.invalidateAll();
    }

    @SneakyThrows
    public static String parseDictDataLabel(String dictType, Integer value) {
        if (value == null) {
            return null;
        }
        return parseDictDataLabel(dictType, String.valueOf(value));
    }

    @SneakyThrows
    public static String parseDictDataLabel(String dictType, String value) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(dictType);
        DictDataRespDTO dictData = CollUtil.findOne(dictDatas, data -> Objects.equals(data.getValue(), value));
        return dictData != null ? dictData.getLabel() : null;
    }

    @SneakyThrows
    public static List<String> getDictDataLabelList(String dictType) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(dictType);
        return convertList(dictDatas, DictDataRespDTO::getLabel);
    }

    @SneakyThrows
    public static String parseDictDataValue(String dictType, String label) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(dictType);
        DictDataRespDTO dictData = CollUtil.findOne(dictDatas, data -> Objects.equals(data.getLabel(), label));
        return dictData != null ? dictData.getValue() : null;
    }

    @SneakyThrows
    public static List<String> getDictDataValueList(String dictType) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(dictType);
        return convertList(dictDatas, DictDataRespDTO::getValue);
    }

}
