package com.cyt.center.module.portal.dal.mysql.hrdoc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentAliasDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * HR 文档旧地址别名仓储（迁自 Portal 的 repositories/hr_documents.py 的 alias 段）。
 */
@Mapper
public interface PortalHrDocumentAliasMapper extends BaseMapperX<PortalHrDocumentAliasDO> {

    /** 按 alias 精确解析到文档 ID；不存在返回 null */
    default Long selectDocumentIdByAlias(String alias) {
        PortalHrDocumentAliasDO row = selectOne(PortalHrDocumentAliasDO::getAlias, alias);
        return row == null ? null : row.getDocumentId();
    }

    /** 一批 alias 归属的文档 ID 集合（slug_taken 用：主表 + 别名表都要查） */
    default Set<Long> selectDocumentIdsByAliases(Collection<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<PortalHrDocumentAliasDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PortalHrDocumentAliasDO::getAlias, aliases);
        Set<Long> ids = new LinkedHashSet<>();
        for (PortalHrDocumentAliasDO row : selectList(wrapper)) {
            ids.add(row.getDocumentId());
        }
        return ids;
    }

    /** 某文档的全部别名（幂等登记 / 审计用） */
    default List<PortalHrDocumentAliasDO> selectByDocumentId(Long documentId) {
        return selectList(PortalHrDocumentAliasDO::getDocumentId, documentId);
    }

}
