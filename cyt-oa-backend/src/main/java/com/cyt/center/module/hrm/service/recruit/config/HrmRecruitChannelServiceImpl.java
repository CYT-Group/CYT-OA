package com.cyt.center.module.hrm.service.recruit.config;

import com.cyt.center.module.hrm.service.recruit.candidate.HrmRecruitCandidateService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.cyt.center.framework.common.enums.CommonStatusEnum;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.object.BeanUtils;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.channel.HrmRecruitChannelDeleteReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.channel.HrmRecruitChannelPageReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.channel.HrmRecruitChannelSaveReqVO;
import com.cyt.center.module.hrm.controller.admin.recruit.vo.channel.HrmRecruitChannelStatusReqVO;
import com.cyt.center.module.hrm.dal.dataobject.recruit.config.HrmRecruitChannelDO;
import com.cyt.center.module.hrm.dal.postgresql.recruit.config.HrmRecruitChannelMapper;
import com.cyt.center.module.hrm.service.employee.info.HrmEmployeeService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.RECRUIT_CHANNEL_SYSTEM_NAME_UPDATE_FORBIDDEN;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.RECRUIT_CHANNEL_SYSTEM_DELETE_FORBIDDEN;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.RECRUIT_CHANNEL_NOT_EXISTS;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.RECRUIT_CHANNEL_TRANSFER_DISABLED;
import static com.cyt.center.module.hrm.enums.ErrorCodeConstants.RECRUIT_CHANNEL_TRANSFER_SELF;
import static com.cyt.center.module.hrm.enums.LogRecordConstants.*;

/**
 * 招聘渠道 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class HrmRecruitChannelServiceImpl implements HrmRecruitChannelService {

    @Resource
    private HrmRecruitChannelMapper recruitChannelMapper;
    @Resource
    private HrmEmployeeService employeeService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private HrmRecruitCandidateService recruitCandidateService;

    @Override    public Long createRecruitChannel(HrmRecruitChannelSaveReqVO createReqVO) {
        // 1. 插入招聘渠道
        HrmRecruitChannelDO recruitChannel = BeanUtils.toBean(createReqVO, HrmRecruitChannelDO.class)
                .setSystemFlag(Boolean.FALSE).setStatus(CommonStatusEnum.ENABLE.getStatus());
        recruitChannelMapper.insert(recruitChannel);;
        return recruitChannel.getId();
    }

    @Override    public void updateRecruitChannel(HrmRecruitChannelSaveReqVO updateReqVO) {
        // 1. 校验招聘渠道是否存在。系统内置渠道只允许调整排序和备注
        HrmRecruitChannelDO recruitChannel = validateRecruitChannelExists(updateReqVO.getId());
        if (Boolean.TRUE.equals(recruitChannel.getSystemFlag())
                && ObjUtil.notEqual(recruitChannel.getName(), updateReqVO.getName())) {
            throw exception(RECRUIT_CHANNEL_SYSTEM_NAME_UPDATE_FORBIDDEN);
        }

        // 2. 更新招聘渠道
        recruitChannelMapper.updateById(BeanUtils.toBean(updateReqVO, HrmRecruitChannelDO.class));;
    }

    @Override    public void updateRecruitChannelStatus(HrmRecruitChannelStatusReqVO statusReqVO) {
        // 1. 校验招聘渠道是否存在
        HrmRecruitChannelDO recruitChannel = validateRecruitChannelExists(statusReqVO.getId());

        // 2. 更新招聘渠道状态
        recruitChannelMapper.updateById(new HrmRecruitChannelDO()
                .setId(statusReqVO.getId()).setStatus(statusReqVO.getStatus()));;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)    public void deleteRecruitChannel(HrmRecruitChannelDeleteReqVO deleteReqVO) {
        // 1.1 校验待删除招聘渠道
        HrmRecruitChannelDO recruitChannel = validateRecruitChannelExists(deleteReqVO.getId());
        if (Boolean.TRUE.equals(recruitChannel.getSystemFlag())) {
            throw exception(RECRUIT_CHANNEL_SYSTEM_DELETE_FORBIDDEN);
        }
        if (ObjUtil.equals(deleteReqVO.getId(), deleteReqVO.getTransferChannelId())) {
            throw exception(RECRUIT_CHANNEL_TRANSFER_SELF);
        }
        // 1.2 校验承接招聘渠道
        HrmRecruitChannelDO transferRecruitChannel = validateRecruitChannelExists(deleteReqVO.getTransferChannelId());
        if (!CommonStatusEnum.isEnable(transferRecruitChannel.getStatus())) {
            throw exception(RECRUIT_CHANNEL_TRANSFER_DISABLED);
        }

        // 2. 迁移员工、候选人的招聘渠道后，删除原渠道
        employeeService.updateEmployeeChannelByChannelId(recruitChannel.getId(), transferRecruitChannel.getId());
        recruitCandidateService.updateRecruitCandidateChannelByChannelId(
                recruitChannel.getId(), transferRecruitChannel.getId());
        recruitChannelMapper.deleteById(recruitChannel.getId());;;
    }

    @Override
    public HrmRecruitChannelDO getRecruitChannel(Long id) {
        return recruitChannelMapper.selectById(id);
    }

    @Override
    public HrmRecruitChannelDO validateRecruitChannelExists(Long id) {
        HrmRecruitChannelDO recruitChannel = recruitChannelMapper.selectById(id);
        if (recruitChannel == null) {
            throw exception(RECRUIT_CHANNEL_NOT_EXISTS);
        }
        return recruitChannel;
    }

    @Override
    public List<HrmRecruitChannelDO> getRecruitChannelList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return recruitChannelMapper.selectByIds(ids);
    }

    @Override
    public PageResult<HrmRecruitChannelDO> getRecruitChannelPage(HrmRecruitChannelPageReqVO pageReqVO) {
        return recruitChannelMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HrmRecruitChannelDO> getRecruitChannelSimpleList() {
        return recruitChannelMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

}
