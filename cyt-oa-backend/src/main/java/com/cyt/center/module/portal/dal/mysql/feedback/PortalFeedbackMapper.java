package com.cyt.center.module.portal.dal.mysql.feedback;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.feedback.PortalFeedbackDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工反馈仓储（迁自 Portal 的 repositories/feedback.py）。
 *
 * 只承载 SQL：分页按创建时间倒序。可见性/权限由 service 决定。
 */
@Mapper
public interface PortalFeedbackMapper extends BaseMapperX<PortalFeedbackDO> {
}
