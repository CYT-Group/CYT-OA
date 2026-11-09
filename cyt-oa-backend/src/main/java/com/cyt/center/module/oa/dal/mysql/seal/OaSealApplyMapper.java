package com.cyt.center.module.oa.dal.mysql.seal;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.oa.controller.admin.seal.vo.apply.OaSealApplyPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.seal.OaSealApplyDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.cyt.center.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.cyt.center.module.oa.enums.seal.OaSealUseModeEnum;

/**
 * OA 用印申请 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaSealApplyMapper extends BaseMapperX<OaSealApplyDO> {

    default Long selectCountBySealId(Long id) {
        return selectCount(OaSealApplyDO::getSealId, id);
    }

    default OaSealApplyDO selectByNo(String no) {
        return selectOne(OaSealApplyDO::getNo, no);
    }

    default PageResult<OaSealApplyDO> selectPage(Long userId, OaSealApplyPageReqVO reqVO) {
        LambdaQueryWrapperX<OaSealApplyDO> query = new LambdaQueryWrapperX<OaSealApplyDO>()
                .eq(OaSealApplyDO::getUserId, userId)
                .likeIfPresent(OaSealApplyDO::getNo, reqVO.getNo())
                .eqIfPresent(OaSealApplyDO::getSealId, reqVO.getSealId())
                .likeIfPresent(OaSealApplyDO::getSealName, reqVO.getSealName())
                .eqIfPresent(OaSealApplyDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(OaSealApplyDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(OaSealApplyDO::getExpectedUseTime, reqVO.getExpectedUseTime())
                .eqIfPresent(OaSealApplyDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaSealApplyDO::getType, reqVO.getType())
                .eqIfPresent(OaSealApplyDO::getMode, reqVO.getMode())
                .eqIfPresent(OaSealApplyDO::getUrgent, reqVO.getUrgent())
                .orderByDesc(OaSealApplyDO::getId);
        query.eqIfPresent(OaSealApplyDO::getUseStatus, reqVO.getUseStatus());
        return selectPage(reqVO, query);
    }

    default List<OaSealApplyDO> selectListBySealIdAndBorrowTimeConflict(
            Long sealId, Long excludeId, LocalDateTime beginTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OaSealApplyDO>().eq(OaSealApplyDO::getSealId, sealId)
                .ne(OaSealApplyDO::getId, excludeId)
                .eq(OaSealApplyDO::getMode, OaSealUseModeEnum.BORROW.getMode())
                .le(OaSealApplyDO::getExpectedUseTime, endTime)
                .ge(OaSealApplyDO::getExpectedReturnTime, beginTime)
                .in(OaSealApplyDO::getStatus, BpmProcessInstanceStatusEnum.RUNNING.getStatus(),
                        BpmProcessInstanceStatusEnum.APPROVE.getStatus())
                .orderByAsc(OaSealApplyDO::getId));
    }
}
