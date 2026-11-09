package com.cyt.center.module.oa.dal.mysql.workreport;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.date.LocalDateTimeUtils;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.oa.controller.admin.workreport.vo.OaWorkReportPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportDO;
import com.cyt.center.module.oa.enums.workreport.OaWorkReportStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

/**
 * OA 工作汇报 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaWorkReportMapper extends BaseMapperX<OaWorkReportDO> {

    default OaWorkReportDO selectByNo(String no) {
        return selectOne(OaWorkReportDO::getNo, no);
    }

    default PageResult<OaWorkReportDO> selectPage(OaWorkReportPageReqVO pageReqVO, Long userId) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<OaWorkReportDO>()
                .eq(OaWorkReportDO::getCreator, userId.toString())
                .likeIfPresent(OaWorkReportDO::getNo, pageReqVO.getNo())
                .eqIfPresent(OaWorkReportDO::getDeptId, pageReqVO.getDeptId())
                .betweenIfPresent(OaWorkReportDO::getStartTime, pageReqVO.getPeriodTime())
                .betweenIfPresent(OaWorkReportDO::getStartTime,
                        LocalDateTimeUtils.getDayBeginTime(pageReqVO.getStartTime()),
                        LocalDateTimeUtils.getDayEndTime(pageReqVO.getStartTime()))
                .betweenIfPresent(OaWorkReportDO::getEndTime,
                        LocalDateTimeUtils.getDayBeginTime(pageReqVO.getEndTime()),
                        LocalDateTimeUtils.getDayEndTime(pageReqVO.getEndTime()))
                .eqIfPresent(OaWorkReportDO::getType, pageReqVO.getType())
                .eqIfPresent(OaWorkReportDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(OaWorkReportDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(OaWorkReportDO::getStartTime)
                .orderByDesc(OaWorkReportDO::getId));
    }

    default List<OaWorkReportDO> selectListByCreatorsAndTypeAndStatusAndStartTimeBetween(
            Collection<String> creators, Integer type, Integer status,
            LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OaWorkReportDO>()
                .in(OaWorkReportDO::getCreator, creators)
                .eq(OaWorkReportDO::getType, type)
                .eq(OaWorkReportDO::getStatus, status)
                .between(OaWorkReportDO::getStartTime, startTime, endTime)
                .orderByAsc(OaWorkReportDO::getCreator)
                .orderByAsc(OaWorkReportDO::getStartTime)
                .orderByDesc(OaWorkReportDO::getId));
    }

    /** 团队周报看板：一批人在一段时间内的全部汇报，不分状态。 */
    default List<OaWorkReportDO> selectListByCreatorsAndStartTimeBetween(
            Collection<String> creators, LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<OaWorkReportDO>()
                .in(OaWorkReportDO::getCreator, creators)
                .between(OaWorkReportDO::getStartTime, startTime, endTime)
                .orderByDesc(OaWorkReportDO::getStartTime)
                .orderByDesc(OaWorkReportDO::getId));
    }

    /** 还没交过的汇报：徽标与「待提交」列表都用它，一次查完不再逐周期问数据库。 */
    default List<OaWorkReportDO> selectDraftsByCreator(String creator) {
        return selectList(new LambdaQueryWrapperX<OaWorkReportDO>()
                .eq(OaWorkReportDO::getCreator, creator)
                .eq(OaWorkReportDO::getStatus, OaWorkReportStatusEnum.DRAFT.getStatus())
                .orderByDesc(OaWorkReportDO::getStartTime));
    }

}
