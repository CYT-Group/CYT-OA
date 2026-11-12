package com.cyt.center.module.oa.dal.postgresql.reimbursement;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.oa.controller.admin.reimbursement.vo.OaReimbursementPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.reimbursement.OaReimbursementDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 费用报销 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaReimbursementMapper extends BaseMapperX<OaReimbursementDO> {

    default PageResult<OaReimbursementDO> selectPage(Long userId, OaReimbursementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaReimbursementDO>()
                .eq(OaReimbursementDO::getCreator, userId.toString())
                .likeIfPresent(OaReimbursementDO::getTitle, reqVO.getTitle())
                .eqIfPresent(OaReimbursementDO::getStatus, reqVO.getStatus())
                .orderByDesc(OaReimbursementDO::getCreateTime, OaReimbursementDO::getId));
    }

}
