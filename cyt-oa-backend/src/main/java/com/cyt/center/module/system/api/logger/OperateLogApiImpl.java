package com.cyt.center.module.system.api.logger;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.util.collection.CollectionUtils;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.system.api.logger.dto.OperateLogCreateReqDTO;
import com.cyt.center.module.system.api.logger.dto.OperateLogPageReqDTO;
import com.cyt.center.module.system.api.logger.dto.OperateLogRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import com.cyt.center.module.system.mapper.SysOperateLogMapper;
import com.cyt.center.module.system.model.entity.SysOperateLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志 API 实现类：落到 sys_operate_log。
 *
 * 记日志不该把业务打挂——调用方都是「业务已 commit，再尽力补一条审计」的口径，
 * 所以这里把写入失败降级成 warn，与原 OperateLogApiImpl 落空时的行为保持一致。
 *
 * @author 芋道源码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperateLogApiImpl implements OperateLogApi {

    private final SysOperateLogMapper operateLogs;
    private final AdminUserApi adminUserApi;

    @Override
    public void createOperateLog(OperateLogCreateReqDTO createReqDTO) {
        if (createReqDTO == null) {
            return;
        }
        try {
            operateLogs.insert(BeanUtils.toBean(createReqDTO, SysOperateLog.class));
        } catch (Exception ex) {
            log.warn("[createOperateLog][{} {} 业务({})] 写入失败", createReqDTO.getType(),
                    createReqDTO.getSubType(), createReqDTO.getBizId(), ex);
        }
    }

    @Override
    public PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(pageReqDTO.getPageNo());
        pageParam.setPageSize(pageReqDTO.getPageSize());
        PageResult<SysOperateLog> page = operateLogs.selectPage(pageParam, pageReqDTO.getType(),
                pageReqDTO.getBizId(), pageReqDTO.getUserId());
        if (page.getList().isEmpty()) {
            return PageResult.empty(page.getTotal());
        }
        // 操作人昵称按 userId 现查：日志表只存编号，账号改名后历史仍指向同一个人
        Set<Long> userIds = new LinkedHashSet<>(CollectionUtils.convertSet(page.getList(), SysOperateLog::getUserId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        List<OperateLogRespDTO> list = CollectionUtils.convertList(page.getList(), logDO -> {
            OperateLogRespDTO respDTO = BeanUtils.toBean(logDO, OperateLogRespDTO.class);
            respDTO.setCreateTime(logDO.getCreatedAt());
            AdminUserRespDTO user = logDO.getUserId() == null ? null : userMap.get(logDO.getUserId());
            if (user != null) {
                respDTO.setUserName(user.getNickname());
            }
            return respDTO;
        });
        return new PageResult<>(list, page.getTotal());
    }

}
