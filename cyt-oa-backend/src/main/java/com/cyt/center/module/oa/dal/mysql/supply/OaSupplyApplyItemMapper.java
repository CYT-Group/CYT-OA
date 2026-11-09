package com.cyt.center.module.oa.dal.mysql.supply;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.framework.mybatis.core.query.MPJLambdaWrapperX;
import com.cyt.center.module.oa.dal.dataobject.supply.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.apply.*;
import com.cyt.center.module.oa.controller.admin.supply.vo.issue.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * OA 用品申请明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface OaSupplyApplyItemMapper extends BaseMapperX<OaSupplyApplyItemDO> {

    default Long selectCountByItemIdAndUnreturned(Long itemId, Integer manageType) {
        return selectCount(new LambdaQueryWrapperX<OaSupplyApplyItemDO>()
                .eq(OaSupplyApplyItemDO::getItemId, itemId)
                .eq(OaSupplyApplyItemDO::getManageType, manageType)
                .apply("issued_quantity > returned_quantity"));
    }

    default Long selectCountByItemIdAndStatus(Long itemId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<OaSupplyApplyItemDO>()
                .eq(OaSupplyApplyItemDO::getItemId, itemId)
                .eq(OaSupplyApplyItemDO::getStatus, status));
    }

    default List<OaSupplyApplyItemDO> selectListByApplyId(Long applyId) {
        return selectList(new LambdaQueryWrapperX<OaSupplyApplyItemDO>()
                .eq(OaSupplyApplyItemDO::getApplyId, applyId).orderByAsc(OaSupplyApplyItemDO::getId));
    }

    default PageResult<OaSupplyApplyItemDO> selectPage(OaSupplyIssuePageReqVO reqVO,
                                                       Collection<String> creators) {
        MPJLambdaWrapperX<OaSupplyApplyItemDO> query = new MPJLambdaWrapperX<OaSupplyApplyItemDO>()
                .selectAll(OaSupplyApplyItemDO.class)
                .innerJoin(OaSupplyApplyDO.class, OaSupplyApplyDO::getId, OaSupplyApplyItemDO::getApplyId)
                .likeIfPresent(OaSupplyApplyItemDO::getItemName, reqVO.getItemName())
                .eqIfPresent(OaSupplyApplyItemDO::getManageType, reqVO.getManageType())
                .eqIfPresent(OaSupplyApplyItemDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaSupplyApplyDO::getUseType, reqVO.getUseType())
                .betweenIfPresent(OaSupplyApplyDO::getCreateTime, reqVO.getCreateTime());
        if (creators != null) {
            query.in(OaSupplyApplyDO::getCreator, creators);
        }
        query.orderByDesc(OaSupplyApplyItemDO::getId);
        return selectJoinPage(reqVO, OaSupplyApplyItemDO.class, query);
    }

    default void deleteByApplyId(Long applyId) {
        delete(OaSupplyApplyItemDO::getApplyId, applyId);
    }

    default void updateStatusByApplyId(Long applyId, Integer status) {
        update(new OaSupplyApplyItemDO().setStatus(status),
                new LambdaUpdateWrapper<OaSupplyApplyItemDO>().eq(OaSupplyApplyItemDO::getApplyId, applyId));
    }

    default int updateByIdAndStatus(OaSupplyApplyItemDO item, Integer expectedStatus) {
        return update(item, new LambdaUpdateWrapper<OaSupplyApplyItemDO>()
                .eq(OaSupplyApplyItemDO::getId, item.getId())
                .eq(OaSupplyApplyItemDO::getStatus, expectedStatus));
    }

    default int updateByIdAndReturnedQuantity(OaSupplyApplyItemDO item, Integer expectedReturnedQuantity) {
        return update(item, new LambdaUpdateWrapper<OaSupplyApplyItemDO>()
                .eq(OaSupplyApplyItemDO::getId, item.getId())
                .eq(OaSupplyApplyItemDO::getReturnedQuantity, expectedReturnedQuantity));
    }

}
