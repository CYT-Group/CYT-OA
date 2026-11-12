package com.cyt.center.module.portal.dal.postgresql.signoff;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.signoff.PortalHandbookSignoffDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 制度签收仓储（迁自 Portal 的 repositories/handbook_signoff.py）。
 */
@Mapper
public interface PortalHandbookSignoffMapper extends BaseMapperX<PortalHandbookSignoffDO> {

    /** 某用户的全部签收记录（按签收时间倒序） */
    default List<PortalHandbookSignoffDO> selectByUserId(Long userId) {
        LambdaQueryWrapper<PortalHandbookSignoffDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHandbookSignoffDO::getUserId, userId)
                .orderByDesc(PortalHandbookSignoffDO::getSignedAt);
        return selectList(wrapper);
    }

    /** 幂等查重：按 user_id + version + (document_id 或 document in 集合) 查最早一条 */
    default PortalHandbookSignoffDO selectExisting(Long userId, Long documentId, Collection<String> documents,
                                                   String version) {
        LambdaQueryWrapper<PortalHandbookSignoffDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHandbookSignoffDO::getUserId, userId)
                .eq(PortalHandbookSignoffDO::getVersion, version);
        if (documentId != null && (documents == null || documents.isEmpty())) {
            wrapper.eq(PortalHandbookSignoffDO::getDocumentId, documentId);
        } else if (documents != null && !documents.isEmpty()) {
            wrapper.and(w -> {
                w.in(PortalHandbookSignoffDO::getDocument, documents);
                if (documentId != null) {
                    w.or().eq(PortalHandbookSignoffDO::getDocumentId, documentId);
                }
            });
        }
        wrapper.orderByAsc(PortalHandbookSignoffDO::getSignedAt).last("LIMIT 1");
        return selectOne(wrapper);
    }

    /** 某文档某版本已签收的 user_id 集合（document 或 document_id 命中） */
    default java.util.Set<Long> selectSignedUserIds(Long documentId, Collection<String> documents, String version) {
        if(documentId==null&&(documents==null||documents.isEmpty()))return java.util.Set.of();
        LambdaQueryWrapper<PortalHandbookSignoffDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHandbookSignoffDO::getVersion, version);
        if (documentId != null && (documents == null || documents.isEmpty())) {
            wrapper.eq(PortalHandbookSignoffDO::getDocumentId, documentId);
        } else if(documentId!=null) {
            wrapper.and(w -> {
                w.eq(PortalHandbookSignoffDO::getDocumentId,documentId)
                        .or(legacy->legacy.isNull(PortalHandbookSignoffDO::getDocumentId).in(PortalHandbookSignoffDO::getDocument,documents));
            });
        } else {
            wrapper.in(PortalHandbookSignoffDO::getDocument,documents);
        }
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        for (PortalHandbookSignoffDO row : selectList(wrapper)) {
            ids.add(row.getUserId());
        }
        return ids;
    }

}
