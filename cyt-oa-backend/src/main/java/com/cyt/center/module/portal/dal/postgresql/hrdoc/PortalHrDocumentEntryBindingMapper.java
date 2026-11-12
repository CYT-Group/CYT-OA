package com.cyt.center.module.portal.dal.postgresql.hrdoc;

import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentEntryBindingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * HR 文档固定入口绑定仓储（迁自 Portal 的 repositories/hr_documents.py 的 entry 段）。
 */
@Mapper
public interface PortalHrDocumentEntryBindingMapper extends BaseMapperX<PortalHrDocumentEntryBindingDO> {

    /** 按入口 key 精确查绑定；不存在返回 null */
    default PortalHrDocumentEntryBindingDO selectByEntryKey(String entryKey) {
        return selectOne(PortalHrDocumentEntryBindingDO::getEntryKey, entryKey);
    }

}
