package com.cyt.center.module.oa.dal.mysql.workreport;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.oa.dal.dataobject.workreport.OaWorkReportCommentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 工作汇报点评 Mapper
 */
@Mapper
public interface OaWorkReportCommentMapper extends BaseMapperX<OaWorkReportCommentDO> {

    default List<OaWorkReportCommentDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<OaWorkReportCommentDO>()
                .eq(OaWorkReportCommentDO::getReportId, reportId)
                .orderByDesc(OaWorkReportCommentDO::getId));
    }

    default List<OaWorkReportCommentDO> selectListByReportIds(Collection<Long> reportIds) {
        return selectList(new LambdaQueryWrapperX<OaWorkReportCommentDO>()
                .in(OaWorkReportCommentDO::getReportId, reportIds)
                .orderByDesc(OaWorkReportCommentDO::getId));
    }

    default OaWorkReportCommentDO selectByReportAndReviewer(Long reportId, Long reviewerId) {
        return selectOne(OaWorkReportCommentDO::getReportId, reportId,
                OaWorkReportCommentDO::getReviewerId, reviewerId);
    }

    /** 某位上级在给定报告里已点评过的报告号，用于「等我的点评」去重。 */
    default List<OaWorkReportCommentDO> selectListByReviewer(Long reviewerId, Collection<Long> reportIds) {
        return selectList(new LambdaQueryWrapperX<OaWorkReportCommentDO>()
                .eq(OaWorkReportCommentDO::getReviewerId, reviewerId)
                .in(OaWorkReportCommentDO::getReportId, reportIds));
    }

}
