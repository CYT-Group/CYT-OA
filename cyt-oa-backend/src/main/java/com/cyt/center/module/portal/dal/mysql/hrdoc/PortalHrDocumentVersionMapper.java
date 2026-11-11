package com.cyt.center.module.portal.dal.mysql.hrdoc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * HR 文档发布版本快照仓储（迁自 Portal 的 repositories/hr_documents.py 的 version 段）。
 *
 * 发布版本只增不可删（仲裁举证面）；按 document_id + version 查单条、按 document_id 列全部（新的在前）。
 */
@Mapper
public interface PortalHrDocumentVersionMapper extends BaseMapperX<PortalHrDocumentVersionDO> {

    /** 按文档 ID + 版本号查单条发布快照；不存在返回 null */
    default PortalHrDocumentVersionDO selectByDocumentIdAndVersion(Long documentId, String version) {
        return selectOne(PortalHrDocumentVersionDO::getDocumentId, documentId,
                PortalHrDocumentVersionDO::getVersion, version);
    }

    /** 某文档的全部发布快照，新的在前 */
    default List<PortalHrDocumentVersionDO> selectVersionsByDocumentId(Long documentId) {
        LambdaQueryWrapper<PortalHrDocumentVersionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHrDocumentVersionDO::getDocumentId, documentId)
                .orderByDesc(PortalHrDocumentVersionDO::getPublishedAt);
        return selectList(wrapper);
    }

    /** 某文档已发布过的全部版本号（publication_version_taken 用，忽略首尾空白对齐旧数据） */
    default List<String> selectVersionNamesByDocumentId(Long documentId) {
        LambdaQueryWrapper<PortalHrDocumentVersionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(PortalHrDocumentVersionDO::getVersion)
                .eq(PortalHrDocumentVersionDO::getDocumentId, documentId);
        return selectList(wrapper).stream().map(PortalHrDocumentVersionDO::getVersion).toList();
    }

}
