package com.cyt.center.module.portal.dal.mysql.hrdoc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDraftVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 文档草稿版本快照仓储（迁自 Portal 的 repositories/hr_documents.py 的 draft version 段）。
 *
 * 保存草稿自动留快照、连续相同内容去重、每文档只留最新 30 条；可回看/删除。
 */
@Mapper
public interface PortalHrDocumentDraftVersionMapper extends BaseMapperX<PortalHrDocumentDraftVersionDO> {

    /** 某文档最新一条草稿快照（保存时比对去重；created_at + id 双键保证「最新」确定） */
    default PortalHrDocumentDraftVersionDO selectLatestByDocumentId(Long documentId) {
        LambdaQueryWrapper<PortalHrDocumentDraftVersionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHrDocumentDraftVersionDO::getDocumentId, documentId)
                .orderByDesc(PortalHrDocumentDraftVersionDO::getCreatedAt)
                .orderByDesc(PortalHrDocumentDraftVersionDO::getId)
                .last("LIMIT 1");
        return selectOne(wrapper);
    }

    /** 某文档的全部草稿快照，新的在前（编辑器历史面板列表序） */
    default List<PortalHrDocumentDraftVersionDO> selectByDocumentId(Long documentId) {
        LambdaQueryWrapper<PortalHrDocumentDraftVersionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHrDocumentDraftVersionDO::getDocumentId, documentId)
                .orderByDesc(PortalHrDocumentDraftVersionDO::getCreatedAt)
                .orderByDesc(PortalHrDocumentDraftVersionDO::getId);
        return selectList(wrapper);
    }

    /** 按文档 ID + 快照 ID 查单条（防跨文档猜号）；不存在返回 null */
    default PortalHrDocumentDraftVersionDO selectByDocumentIdAndId(Long documentId, Long versionId) {
        return selectOne(PortalHrDocumentDraftVersionDO::getDocumentId, documentId,
                PortalHrDocumentDraftVersionDO::getId, versionId);
    }

    /** 删掉某文档超出保留条数的最旧草稿快照，返回删除行数（best-effort，SAVEPOINT 语义由服务层保证） */
    default int deleteBeyondKeep(Long documentId, int keep) {
        List<PortalHrDocumentDraftVersionDO> all = selectByDocumentId(documentId);
        if (all.size() <= keep) {
            return 0;
        }
        int deleted = 0;
        for (int i = keep; i < all.size(); i++) {
            deleteById(all.get(i).getId());
            deleted++;
        }
        return deleted;
    }

}
