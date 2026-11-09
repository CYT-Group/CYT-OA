package com.cyt.center.module.oa.dal.mysql.seal;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.oa.controller.admin.seal.vo.OaSealPageReqVO;
import com.cyt.center.module.oa.dal.dataobject.seal.OaSealDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 印章 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaSealMapper extends BaseMapperX<OaSealDO> {

    default PageResult<OaSealDO> selectPage(OaSealPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaSealDO>()
                .likeIfPresent(OaSealDO::getNo, reqVO.getNo())
                .likeIfPresent(OaSealDO::getName, reqVO.getName())
                .eqIfPresent(OaSealDO::getCategory, reqVO.getCategory())
                .eqIfPresent(OaSealDO::getType, reqVO.getType())
                .eqIfPresent(OaSealDO::getKeeperUserId, reqVO.getKeeperUserId())
                .eqIfPresent(OaSealDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(OaSealDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(OaSealDO::getPurchaseTime, reqVO.getPurchaseTime())
                .betweenIfPresent(OaSealDO::getEnableTime, reqVO.getEnableTime())
                .betweenIfPresent(OaSealDO::getDisableTime, reqVO.getDisableTime())
                .orderByAsc(OaSealDO::getSort)
                .orderByDesc(OaSealDO::getId));
    }

    default OaSealDO selectByNo(String no) {
        return selectOne(OaSealDO::getNo, no);
    }

}
