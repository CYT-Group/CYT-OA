package com.cyt.center.module.system.api.logger;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.module.system.api.logger.dto.OperateLogCreateReqDTO;
import com.cyt.center.module.system.api.logger.dto.OperateLogPageReqDTO;
import com.cyt.center.module.system.api.logger.dto.OperateLogRespDTO;
import jakarta.validation.Valid;

/**
 * 操作日志 API 接口
 *
 * @author 芋道源码
 */
public interface OperateLogApi {

    /**
     * 创建操作日志
     *
     * @param createReqDTO 请求
     */
    void createOperateLog(@Valid OperateLogCreateReqDTO createReqDTO);

    /**
     * 【异步】创建操作日志
     *
     * @param createReqDTO 请求
     */
    default void createOperateLogAsync(OperateLogCreateReqDTO createReqDTO) {
        createOperateLog(createReqDTO);
    }

    /**
     * 获取指定模块的指定数据的操作日志分页
     *
     * @param pageReqDTO 请求
     * @return 操作日志分页
     */
    PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO);

}
