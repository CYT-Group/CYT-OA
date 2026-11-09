package com.cyt.center.module.approval.dal.mysql;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.approval.dal.dataobject.WecomApprovalLinkDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务单据与企微审批单的链接表。唯一键（apply_type, biz_id）保证一张单据只认一张企微单，
 * 并发重复提交时靠 ON DUPLICATE KEY UPDATE 幂等回填单号。
 */
@Mapper
public interface WecomApprovalLinkMapper extends BaseMapperX<WecomApprovalLinkDO> {

    default WecomApprovalLinkDO selectByTypeAndBizId(String applyType, Long bizId) {
        return selectOne(new LambdaQueryWrapperX<WecomApprovalLinkDO>()
                .eq(WecomApprovalLinkDO::getApplyType, applyType)
                .eq(WecomApprovalLinkDO::getBizId, bizId));
    }

    @Insert("INSERT INTO oa_wecom_approval_link (apply_type,biz_id,request_id,approval_no,creator,updater,create_time,update_time,deleted) "
            + "VALUES (#{applyType},#{bizId},#{requestId},#{approvalNo},#{creator},#{updater},now(),now(),0) "
            + "ON DUPLICATE KEY UPDATE approval_no=VALUES(approval_no),update_time=now()")
    int upsert(WecomApprovalLinkDO link);

}
