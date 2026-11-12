package com.cyt.center.module.oa.dal.postgresql.supply;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.oa.dal.dataobject.supply.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.item.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * OA 办公用品 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaSupplyItemMapper extends BaseMapperX<OaSupplyItemDO> {

    default PageResult<OaSupplyItemDO> selectPage(OaSupplyItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaSupplyItemDO>()
                .likeIfPresent(OaSupplyItemDO::getName, reqVO.getName())
                .likeIfPresent(OaSupplyItemDO::getNo, reqVO.getNo())
                .eqIfPresent(OaSupplyItemDO::getCategory, reqVO.getCategory())
                .eqIfPresent(OaSupplyItemDO::getManageType, reqVO.getManageType())
                .eqIfPresent(OaSupplyItemDO::getStatus, reqVO.getStatus())
                .orderByAsc(OaSupplyItemDO::getSort).orderByDesc(OaSupplyItemDO::getId));
    }

    default OaSupplyItemDO selectByNo(String no) {
        return selectOne(OaSupplyItemDO::getNo, no);
    }

    default int updateStockQuantity(Long id, Integer quantity) {
        LambdaUpdateWrapper<OaSupplyItemDO> update = new LambdaUpdateWrapper<OaSupplyItemDO>()
                .eq(OaSupplyItemDO::getId, id)
                .setIncrBy(OaSupplyItemDO::getStockQuantity, quantity);
        if (quantity < 0) {
            update.ge(OaSupplyItemDO::getStockQuantity, -quantity);
        }
        return update(update);
    }

}
