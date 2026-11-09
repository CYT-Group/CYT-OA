package com.cyt.center.module.system.mapper;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.system.model.entity.SysOperateLog;

public interface SysOperateLogMapper extends BaseMapperX<SysOperateLog> {

    default PageResult<SysOperateLog> selectPage(PageParam pageParam, String type, Long bizId, Long userId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<SysOperateLog>()
                .eqIfPresent(SysOperateLog::getType, type)
                .eqIfPresent(SysOperateLog::getBizId, bizId)
                .eqIfPresent(SysOperateLog::getUserId, userId)
                .orderByDesc(SysOperateLog::getId));
    }

}
