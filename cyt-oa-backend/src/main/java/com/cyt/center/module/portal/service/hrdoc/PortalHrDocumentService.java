package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentSaveReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentUpdateReqVO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDraftVersionDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentVersionDO;

import java.util.List;
import java.util.Map;

/**
 * HR 文档 CMS 用例（迁自 Portal 的 application/hr_documents.py）。
 *
 * 草稿/发布双列：admin 改稿只动 draft_*，publish 把 draft_* 晋升为正式列并写版本快照；
 * 员工读的是已发布快照（content_*），逐篇按部门可见范围过滤。下架 / 逻辑删除 / 重新上架 / 恢复
 * 都是独立动作，不升版本不写快照。
 */
public interface PortalHrDocumentService {

    // ══════════════ 管理端 ══════════════

    PageResult<PortalHrDocumentDO> page(PageParam pageParam, String category, String status, boolean deleted, String q);

    /** 管理端详情：DO + 发布确认凭据 revision + 发布状态（publishedBefore / bodyChanged / currentVersion） */
    Detail getDetail(Long id);

    /** 对本次写入返回的同一份文档快照生成发布凭据，避免另查一次夹入他人并发改稿。 */
    Detail describe(PortalHrDocumentDO doc);

    /** 管理员旧地址解析，不应用员工发布状态/部门过滤。 */
    Long resolveAdminSlug(String slug);

    PortalHrDocumentDO create(Long userId, PortalHrDocumentSaveReqVO reqVO);

    PortalHrDocumentDO update(Long userId, Long id, PortalHrDocumentUpdateReqVO reqVO);

    PortalHrDocumentDO publish(Long userId, Long id, String expectedRevision, String version);

    PortalHrDocumentDO offline(Long userId, Long id, String reason);

    PortalHrDocumentDO restore(Long userId, Long id);

    PortalHrDocumentDO delete(Long userId, Long id);

    PortalHrDocumentDO undelete(Long userId, Long id);

    List<PortalHrDocumentVersionDO> listVersions(Long id);

    PortalHrDocumentVersionDO getVersion(Long id, String version);

    List<PortalHrDocumentDraftVersionDO> listDraftVersions(Long id);

    PortalHrDocumentDraftVersionDO getDraftVersion(Long id, Long versionId);

    void deleteDraftVersion(Long userId, Long id, Long versionId);

    SlugCheck checkSlug(String slug, String exclude);

    // ══════════════ 员工端（读） ══════════════

    List<PortalHrDocumentDO> listPublished(Long userId, String category);

    PortalHrDocumentDO getPublished(Long userId, Long id);

    List<EntryItem> listEntries(Long userId);

    PortalHrDocumentDO getEntry(Long userId, String entryKey);

    PortalHrDocumentDO getPublishedBySlug(Long userId, String slug);

    // ══════════════ 返回结构 ══════════════

    record Detail(PortalHrDocumentDO doc, String revision, Map<String, Object> publicationState) {
    }

    record SlugCheck(boolean available, String reason, String message, Map<String, Object> owner) {
    }

    record EntryItem(String entryKey, PortalHrDocumentDO doc) {
    }

}
