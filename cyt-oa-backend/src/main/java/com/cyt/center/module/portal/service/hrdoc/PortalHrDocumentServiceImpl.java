package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentSaveReqVO;
import com.cyt.center.module.portal.controller.admin.hrdoc.vo.PortalHrDocumentUpdateReqVO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentAliasDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDraftVersionDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentEntryBindingDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentVersionDO;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentAliasMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentDraftVersionMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentEntryBindingMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentVersionMapper;
import com.cyt.center.module.portal.domain.hrdoc.HrDocumentBlockValidator;
import com.cyt.center.module.portal.domain.hrdoc.HrDocumentValidator;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalHtmlSanitizer;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.permission.PermissionApi;
import com.cyt.center.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.domain.PortalCapability.VIEW_HR_SOP;
import static com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO.CATEGORY_HR_SOP;
import static com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO.STATUS_PUBLISHED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_ALREADY_PUBLISHED_NOT_OFFLINE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_BODY_CHANGED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_DRAFT_VERSION_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_FIELDS_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_FORBIDDEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_NEVER_PUBLISHED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_NO_DRAFT;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_NOT_PUBLISHED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_OFFLINE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_REVISION_CONFLICT;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_SLUG_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_SLUG_TAKEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_VERSION_NOT_EXISTS;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_VERSION_TAKEN;

/**
 * HR 文档 CMS 用例实现（逐条对齐 Portal 的 application/hr_documents.py + domain/hr_documents.py）。
 *
 * 草稿/发布双列：admin 改稿只动 draft_*，publish 把 draft_* 晋升为正式列并写版本快照；员工读已发布快照，
 * 逐篇按部门可见范围过滤。下架 / 逻辑删除 / 重新上架 / 恢复都是独立动作，不升版本不写快照。
 */
@Slf4j
@Service
public class PortalHrDocumentServiceImpl implements PortalHrDocumentService {

    private static final String AUDIT_TABLE = "portal_hr_documents";
    private static final int DRAFT_VERSIONS_KEEP = 30;
    private static final String ROLE_ADMIN = "admin";

    @Resource
    private PortalHrDocumentMapper documentMapper;
    @Resource
    private PortalHrDocumentAliasMapper aliasMapper;
    @Resource
    private PortalHrDocumentEntryBindingMapper entryMapper;
    @Resource
    private PortalHrDocumentVersionMapper versionMapper;
    @Resource
    private PortalHrDocumentDraftVersionMapper draftVersionMapper;
    @Resource
    private PortalAccess access;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PortalAudit audit;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;

    // ══════════════ 管理端 ══════════════

    @Override
    public PageResult<PortalHrDocumentDO> page(PageParam pageParam, String category, String status,
                                               boolean deleted, String q) {
        return documentMapper.selectPage(pageParam, blankToNull(category), blankToNull(status), deleted,
                blankToNull(q));
    }

    @Override
    public Detail getDetail(Long id) {
        PortalHrDocumentDO doc = getOr404(id, false);
        return describe(doc);
    }

    @Override
    public Detail describe(PortalHrDocumentDO doc) {
        Map<String, Object> state = publicationState(doc);
        return new Detail(doc, (String) state.get("revision"), state);
    }

    @Override
    public Long resolveAdminSlug(String slug) {
        if(slug==null||slug.isBlank()||slug.length()>255)throw exception(HRDOC_SLUG_INVALID);
        String reference=slug.trim();
        Long id=aliasMapper.selectDocumentIdByAlias(reference);
        if(id==null&&!reference.startsWith("/"))id=aliasMapper.selectDocumentIdByAlias("/hr-doc/"+reference);
        PortalHrDocumentDO doc=id==null?documentMapper.selectBySlug(reference):documentMapper.selectById(id);
        if(doc==null)throw exception(HRDOC_NOT_EXISTS);
        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO create(Long userId, PortalHrDocumentSaveReqVO reqVO) {
        PortalOperator operator = operators.resolve(userId);
        validateCreateFields(reqVO);

        PortalHrDocumentDO doc = new PortalHrDocumentDO();
        doc.setTitle(reqVO.getTitle().trim());
        doc.setCategory(reqVO.getCategory());
        doc.setTopic(blankToNull(reqVO.getTopic()));
        doc.setContentHtml("");
        doc.setDraftContent(PortalHtmlSanitizer.sanitize(reqVO.getContentHtml()));
        doc.setDraftTitle(reqVO.getTitle().trim());
        doc.setDraftRequiresSignoff(reqVO.getRequiresSignoff());
        doc.setDraftDeptScope(reqVO.getDeptScope());
        doc.setDraftIntro(reqVO.getIntro());
        doc.setDraftCoverImage(blankToNull(reqVO.getCoverImage()));
        doc.setDraftTheme(blankToNull(reqVO.getTheme()));
        doc.setDraftTags(reqVO.getTags());
        doc.setDraftTocEnabled(reqVO.getTocEnabled());
        doc.setDraftTocPosition(reqVO.getTocPosition());
        doc.setDraftCover(HrDocumentValidator.normalizeCover(reqVO.getCover()));
        doc.setDraftDownloadPdfEnabled(reqVO.getDownloadPdfEnabled());
        doc.setDraftSignoff(HrDocumentValidator.normalizeSignoff(reqVO.getSignoff()));
        doc.setVersion(reqVO.getVersion());
        doc.setDraftVersion(reqVO.getVersion());
        doc.setStatus(PortalHrDocumentDO.STATUS_DRAFT);
        doc.setRequiresSignoff(false);
        doc.setDeptScope(null);
        // 正式展示列先落默认值，publish 时由 draft_* 晋升覆盖
        doc.setIntro("");
        doc.setCoverImage("");
        doc.setTheme("");
        doc.setTocEnabled(true);
        doc.setTocPosition("left");
        doc.setCover(HrDocumentValidator.normalizeCover(null));
        doc.setDownloadPdfEnabled(false);
        doc.setSignoff(HrDocumentValidator.normalizeSignoff(null));
        doc.setUpdatedBy(operator.username());
        documentMapper.insert(doc);
        audit.record(userId, "hr_doc_create", AUDIT_TABLE, doc.getId(), null,
                "{\"category\":\"" + doc.getCategory() + "\",\"version\":\"" + doc.getVersion() + "\"}", null);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO update(Long userId, Long id, PortalHrDocumentUpdateReqVO reqVO) {
        PortalOperator operator = operators.resolve(userId);
        PortalHrDocumentDO doc = getOr404(id, false);

        if (reqVO.getBlocks() != null && reqVO.getContentHtml() != null) {
            throw exception(HRDOC_FIELDS_INVALID, "blocks 与 contentHtml 不能同时提交，一次只能保存一种草稿格式");
        }

        String before = "{\"version\":\"" + doc.getVersion() + "\",\"status\":\"" + doc.getStatus() + "\"}";

        if (reqVO.getTitle() != null) {
            doc.setDraftTitle(reqVO.getTitle());
        }
        if (reqVO.getTopic() != null) {
            HrDocumentValidator.requireValidTopic(reqVO.getTopic());
            doc.setTopic(blankToNull(reqVO.getTopic()));
        }
        if (reqVO.getCategory() != null) {
            HrDocumentValidator.requireValidCategory(reqVO.getCategory());
            doc.setCategory(reqVO.getCategory());
        }
        if (reqVO.getBlocks() != null) {
            HrDocumentBlockValidator.validate(reqVO.getBlocks());
            doc.setDraftBlocks(HrDocumentBlockValidator.sanitize(reqVO.getBlocks(), PortalHtmlSanitizer::sanitize));
            doc.setDraftContent(null);
        }
        if (reqVO.getContentHtml() != null) {
            doc.setDraftContent(PortalHtmlSanitizer.sanitize(reqVO.getContentHtml()));
            doc.setDraftBlocks(null);
        }
        if (reqVO.getDraftVersion() != null) {
            doc.setDraftVersion(reqVO.getDraftVersion());
        }
        if (reqVO.getRequiresSignoff() != null) {
            doc.setDraftRequiresSignoff(reqVO.getRequiresSignoff());
        }
        if (reqVO.getDeptScope() != null) {
            doc.setDraftDeptScope(reqVO.getDeptScope());
        }
        if (reqVO.getIntro() != null) {
            HrDocumentValidator.requireValidIntro(reqVO.getIntro());
            doc.setDraftIntro(reqVO.getIntro());
        }
        if (reqVO.getCoverImage() != null) {
            HrDocumentValidator.requireValidCoverImage(reqVO.getCoverImage());
            doc.setDraftCoverImage(reqVO.getCoverImage());
        }
        if (reqVO.getTheme() != null) {
            HrDocumentValidator.requireValidTheme(reqVO.getTheme());
            doc.setDraftTheme(reqVO.getTheme());
        }
        if (reqVO.getTags() != null) {
            HrDocumentValidator.requireValidTags(reqVO.getTags());
            doc.setDraftTags(reqVO.getTags());
        }
        if (reqVO.getTocEnabled() != null) {
            doc.setDraftTocEnabled(reqVO.getTocEnabled());
        }
        if (reqVO.getTocPosition() != null) {
            HrDocumentValidator.requireValidTocPosition(reqVO.getTocPosition());
            doc.setDraftTocPosition(reqVO.getTocPosition());
        }
        if (reqVO.getCover() != null) {
            HrDocumentValidator.requireValidCover(reqVO.getCover());
            doc.setDraftCover(HrDocumentValidator.normalizeCover(reqVO.getCover()));
        }
        if (reqVO.getDownloadPdfEnabled() != null) {
            doc.setDraftDownloadPdfEnabled(reqVO.getDownloadPdfEnabled());
        }
        if (reqVO.getSignoff() != null) {
            HrDocumentValidator.requireValidSignoff(reqVO.getSignoff());
            doc.setDraftSignoff(HrDocumentValidator.normalizeSignoff(reqVO.getSignoff()));
        }

        doc.setStatus(PortalHrDocumentDO.STATUS_DRAFT);
        doc.setUpdatedBy(operator.username());
        snapshotDraftVersion(doc, operator.username());
        documentMapper.updateById(doc);

        audit.record(userId, "hr_doc_update", AUDIT_TABLE, id, before,
                "{\"status\":\"draft\",\"draft_version\":\"" + doc.getDraftVersion() + "\"}", null);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO publish(Long userId, Long id, String expectedRevision, String version) {
        PortalOperator operator = operators.resolve(userId);
        PortalHrDocumentDO doc = getOr404(id, false);

        Map<String, Object> state = publicationState(doc);
        String revision = (String) state.get("revision");
        if (expectedRevision != null && !expectedRevision.equals(revision)) {
            throw exception(HRDOC_REVISION_CONFLICT);
        }
        if (!hasDraft(doc)) {
            throw exception(HRDOC_NO_DRAFT);
        }
        boolean publishedBefore = doc.publishedBefore();
        String targetVersion = HrDocumentValidator.validatePublicationVersion(
                version != null ? version : (doc.getDraftVersion() != null ? doc.getDraftVersion() : doc.getVersion()),
                doc.getVersion(), publishedBefore);
        boolean sameVersion = targetVersion.equals(trimToEmpty(doc.getVersion()));
        boolean bodyChanged = publishedBefore && bodyChanged(doc);
        boolean settingsOnly = publishedBefore && sameVersion && !bodyChanged;

        if (publishedBefore && sameVersion && bodyChanged) {
            throw exception(HRDOC_BODY_CHANGED);
        }
        if (!settingsOnly && versionTaken(doc.getId(), targetVersion)) {
            throw exception(HRDOC_VERSION_TAKEN, targetVersion);
        }

        // 草稿 → 已发布快照（draft_* 晋升为正式列）
        List<Object> publishedBlocks;
        String publishedContent;
        if (doc.getDraftBlocks() != null) {
            publishedBlocks = doc.getDraftBlocks();
            publishedContent = "";
        } else if (doc.getDraftContent() != null) {
            publishedBlocks = null;
            publishedContent = doc.getDraftContent();
        } else {
            publishedBlocks = doc.getContentBlocks();
            publishedContent = doc.getContentHtml();
        }
        String publishedTitle = HrDocumentValidator.promoted(doc.getDraftTitle(), doc.getTitle());
        boolean publishedRequiresSignoff = HrDocumentValidator.promoted(
                doc.getDraftRequiresSignoff(), doc.getRequiresSignoff() == null ? false : doc.getRequiresSignoff());
        List<String> publishedDeptScope = HrDocumentValidator.promotedDeptScope(doc.getDraftDeptScope(), doc.getDeptScope());

        if (!settingsOnly) {
            PortalHrDocumentVersionDO versionDO = new PortalHrDocumentVersionDO();
            versionDO.setDocumentId(doc.getId());
            versionDO.setSlug(doc.getSlug());
            versionDO.setVersion(targetVersion);
            versionDO.setTitle(publishedTitle);
            versionDO.setContentHtml(publishedContent == null ? "" : publishedContent);
            versionDO.setContentBlocks(publishedBlocks);
            versionDO.setPublishedBy(operator.username());
            versionDO.setApprovedBy(operator.username());
            versionDO.setSubmittedBy(doc.getSubmittedBy());
            versionMapper.insert(versionDO);

            doc.setVersion(targetVersion);
            doc.setContentHtml(publishedContent == null ? "" : publishedContent);
            doc.setContentBlocks(publishedBlocks);
            doc.setTitle(publishedTitle);
            doc.setIntro(HrDocumentValidator.promoted(doc.getDraftIntro(), doc.getIntro()));
            doc.setPublishedBy(operator.username());
            doc.setPublishedAt(LocalDateTime.now().withNano(0));
        }

        doc.setStatus(STATUS_PUBLISHED);
        doc.setRequiresSignoff(publishedRequiresSignoff);
        doc.setDeptScope(publishedDeptScope);
        doc.setCoverImage(HrDocumentValidator.promoted(doc.getDraftCoverImage(), doc.getCoverImage()));
        doc.setTheme(HrDocumentValidator.promoted(doc.getDraftTheme(), doc.getTheme()));
        doc.setTags(HrDocumentValidator.promoted(doc.getDraftTags(), doc.getTags()));
        doc.setTocEnabled(HrDocumentValidator.promoted(doc.getDraftTocEnabled(), doc.getTocEnabled()));
        doc.setTocPosition(HrDocumentValidator.promoted(doc.getDraftTocPosition(), doc.getTocPosition()));
        doc.setCover(HrDocumentValidator.promoted(doc.getDraftCover(), doc.getCover()));
        doc.setDownloadPdfEnabled(HrDocumentValidator.promoted(doc.getDraftDownloadPdfEnabled(), doc.getDownloadPdfEnabled()));
        doc.setSignoff(HrDocumentValidator.promoted(doc.getDraftSignoff(), doc.getSignoff()));
        // 清空所有 draft_*
        clearDrafts(doc);
        // 重新发布 = 顺带重新上架
        doc.setOfflineAt(null);
        doc.setOfflineBy(null);
        doc.setOfflineReason(null);

        documentMapper.updateById(doc);
        audit.record(userId, settingsOnly ? "hr_doc_settings_publish" : "hr_doc_publish", AUDIT_TABLE, id,
                null, "{\"version\":\"" + doc.getVersion() + "\"}", null);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO offline(Long userId, Long id, String reason) {
        PortalOperator operator = operators.resolve(userId);
        PortalHrDocumentDO doc = getOr404(id, false);
        if (!doc.publishedBefore()) {
            throw exception(HRDOC_NEVER_PUBLISHED);
        }
        if (doc.isOffline()) {
            return doc;
        }
        HrDocumentValidator.requireValidOfflineReason(reason);
        doc.setOfflineAt(LocalDateTime.now().withNano(0));
        doc.setOfflineBy(operator.username());
        doc.setOfflineReason(reason == null || reason.isBlank() ? null : reason.trim());
        doc.setUpdatedBy(operator.username());
        documentMapper.updateById(doc);
        audit.record(userId, "hr_doc_offline", AUDIT_TABLE, id, null,
                "{\"version\":\"" + doc.getVersion() + "\",\"reason\":\"" + (doc.getOfflineReason() == null ? "" : doc.getOfflineReason()) + "\"}", null);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO restore(Long userId, Long id) {
        PortalOperator operator = operators.resolve(userId);
        PortalHrDocumentDO doc = getOr404(id, false);
        if (!doc.isOffline()) {
            return doc;
        }
        if (!doc.publishedBefore()) {
            throw exception(HRDOC_NEVER_PUBLISHED);
        }
        doc.setOfflineAt(null);
        doc.setOfflineBy(null);
        doc.setOfflineReason(null);
        doc.setUpdatedBy(operator.username());
        documentMapper.updateById(doc);
        audit.record(userId, "hr_doc_restore", AUDIT_TABLE, id, null,
                "{\"version\":\"" + doc.getVersion() + "\"}", null);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO delete(Long userId, Long id) {
        PortalOperator operator = operators.resolve(userId);
        PortalHrDocumentDO doc = getOr404(id, true);
        if (doc.isDeleted()) {
            return doc;
        }
        if (doc.publishedBefore() && !doc.isOffline()) {
            throw exception(HRDOC_ALREADY_PUBLISHED_NOT_OFFLINE);
        }
        doc.setDeletedAt(LocalDateTime.now().withNano(0));
        doc.setDeletedBy(operator.username());
        doc.setUpdatedBy(operator.username());
        documentMapper.updateById(doc);
        audit.record(userId, "hr_doc_delete", AUDIT_TABLE, id, null,
                "{\"version\":\"" + doc.getVersion() + "\"}", null);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHrDocumentDO undelete(Long userId, Long id) {
        PortalOperator operator = operators.resolve(userId);
        PortalHrDocumentDO doc = getOr404(id, true);
        if (!doc.isDeleted()) {
            return doc;
        }
        doc.setDeletedAt(null);
        doc.setDeletedBy(null);
        doc.setUpdatedBy(operator.username());
        documentMapper.updateById(doc);
        audit.record(userId, "hr_doc_undelete", AUDIT_TABLE, id, null,
                "{\"version\":\"" + doc.getVersion() + "\"}", null);
        return doc;
    }

    @Override
    public List<PortalHrDocumentVersionDO> listVersions(Long id) {
        getOr404(id, false);
        return versionMapper.selectVersionsByDocumentId(id);
    }

    @Override
    public PortalHrDocumentVersionDO getVersion(Long id, String version) {
        getOr404(id, false);
        PortalHrDocumentVersionDO v = versionMapper.selectByDocumentIdAndVersion(id, version);
        if (v == null) {
            throw exception(HRDOC_VERSION_NOT_EXISTS);
        }
        return v;
    }

    @Override
    public List<PortalHrDocumentDraftVersionDO> listDraftVersions(Long id) {
        getOr404(id, false);
        return draftVersionMapper.selectByDocumentId(id);
    }

    @Override
    public PortalHrDocumentDraftVersionDO getDraftVersion(Long id, Long versionId) {
        getOr404(id, false);
        PortalHrDocumentDraftVersionDO v = draftVersionMapper.selectByDocumentIdAndId(id, versionId);
        if (v == null) {
            throw exception(HRDOC_DRAFT_VERSION_NOT_EXISTS);
        }
        return v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraftVersion(Long userId, Long id, Long versionId) {
        getOr404(id, false);
        PortalHrDocumentDraftVersionDO v = draftVersionMapper.selectByDocumentIdAndId(id, versionId);
        if (v == null) {
            throw exception(HRDOC_DRAFT_VERSION_NOT_EXISTS);
        }
        draftVersionMapper.deleteById(versionId);
        try {
            audit.record(userId, "hr_doc_draft_version_delete", "portal_hr_document_draft_versions", versionId,
                    v.getTitle(), null, null);
        } catch (Exception e) {
            log.warn("HR 文档 {} 草稿版本 {} 删除审计写入失败", id, versionId, e);
        }
    }

    @Override
    public SlugCheck checkSlug(String slug, String exclude) {
        try {
            HrDocumentValidator.requireValidSlug(slug);
        } catch (Exception e) {
            return new SlugCheck(false, "invalid", "slug 只能由小写字母、数字与连字符组成（2-64 位）", null);
        }
        if (exclude != null && slug.equals(exclude)) {
            return new SlugCheck(true, "self", null, null);
        }
        PortalHrDocumentDO owner = documentMapper.selectBySlug(slug);
        if (owner == null) {
            return new SlugCheck(true, null, null, null);
        }
        Map<String, Object> ownerInfo = new LinkedHashMap<>();
        ownerInfo.put("title", owner.getDraftTitle() != null ? owner.getDraftTitle() : owner.getTitle());
        ownerInfo.put("status", owner.getStatus());
        ownerInfo.put("published_at", owner.getPublishedAt());
        ownerInfo.put("offline_at", owner.getOfflineAt());
        ownerInfo.put("deleted_at", owner.getDeletedAt());
        return new SlugCheck(false, "taken", null, ownerInfo);
    }

    // ══════════════ 员工端（读） ══════════════

    @Override
    public List<PortalHrDocumentDO> listPublished(Long userId, String category) {
        operators.resolve(userId);
        List<PortalHrDocumentDO> docs = documentMapper.selectPublished(category);
        List<PortalHrDocumentDO> visible = new ArrayList<>();
        for (PortalHrDocumentDO doc : docs) {
            if (checkReadAccess(doc, userId)) {
                visible.add(doc);
            }
        }
        return visible;
    }

    @Override
    public PortalHrDocumentDO getPublished(Long userId, Long id) {
        operators.resolve(userId);
        PortalHrDocumentDO doc = documentMapper.selectById(id);
        if (doc == null || !doc.publishedBefore() || doc.isDeleted()) {
            throw exception(HRDOC_NOT_PUBLISHED);
        }
        if (doc.isOffline()) {
            throw exception(HRDOC_OFFLINE);
        }
        if (!checkReadAccess(doc, userId)) {
            throw exception(HRDOC_FORBIDDEN);
        }
        return doc;
    }

    @Override
    public List<EntryItem> listEntries(Long userId) {
        operators.resolve(userId);
        List<PortalHrDocumentEntryBindingDO> bindings = entryMapper.selectList();
        List<EntryItem> out = new ArrayList<>();
        for (PortalHrDocumentEntryBindingDO binding : bindings) {
            PortalHrDocumentDO doc = documentMapper.selectById(binding.getDocumentId());
            if (doc == null || !doc.publishedBefore() || doc.isOffline() || doc.isDeleted()) {
                continue;
            }
            if (checkReadAccess(doc, userId)) {
                out.add(new EntryItem(binding.getEntryKey(), doc));
            }
        }
        return out;
    }

    @Override
    public PortalHrDocumentDO getEntry(Long userId, String entryKey) {
        operators.resolve(userId);
        PortalHrDocumentEntryBindingDO binding = entryMapper.selectByEntryKey(entryKey);
        if (binding == null) {
            throw exception(HRDOC_NOT_PUBLISHED);
        }
        PortalHrDocumentDO doc = documentMapper.selectById(binding.getDocumentId());
        if (doc == null || !doc.publishedBefore() || doc.isDeleted()) {
            throw exception(HRDOC_NOT_PUBLISHED);
        }
        if (doc.isOffline()) {
            throw exception(HRDOC_OFFLINE);
        }
        if (!checkReadAccess(doc, userId)) {
            throw exception(HRDOC_FORBIDDEN);
        }
        return doc;
    }

    @Override
    public PortalHrDocumentDO getPublishedBySlug(Long userId, String slug) {
        operators.resolve(userId);
        Long documentId = aliasMapper.selectDocumentIdByAlias(slug);
        if (documentId == null) {
            documentId = aliasMapper.selectDocumentIdByAlias("/hr-doc/" + slug);
        }
        PortalHrDocumentDO doc = documentId != null ? documentMapper.selectById(documentId)
                : documentMapper.selectBySlug(slug);
        if (doc == null || !doc.publishedBefore() || doc.isDeleted()) {
            throw exception(HRDOC_NOT_PUBLISHED);
        }
        if (doc.isOffline()) {
            throw exception(HRDOC_OFFLINE);
        }
        if (!checkReadAccess(doc, userId)) {
            throw exception(HRDOC_FORBIDDEN);
        }
        return doc;
    }

    // ══════════════ 读权限 / 可见性 ══════════════

    /** 员工读权限：SOP 类限 HR+admin；设了 dept_scope 的还要求部门命中（admin 豁免）。返回 false 表示无权。 */
    private boolean checkReadAccess(PortalHrDocumentDO doc, Long userId) {
        if (CATEGORY_HR_SOP.equals(doc.getCategory()) && !access.has(userId, VIEW_HR_SOP)) {
            return false;
        }
        if (doc.getDeptScope() != null && !doc.getDeptScope().isEmpty()) {
            boolean admin = permissionApi.hasAnyRoles(userId, ROLE_ADMIN);
            if (!admin && !deptScopeHit(doc.getDeptScope(), userDeptNames(userId))) {
                return false;
            }
        }
        return true;
    }

    private List<String> userDeptNames(Long userId) {
        DeptDataPermissionRespDTO perm = permissionApi.getDeptDataPermission(userId);
        Set<Long> deptIds = perm == null ? Set.of() : perm.getDeptIds();
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (DeptRespDTO dept : deptApi.getDeptList(deptIds)) {
            if (dept.getName() != null) {
                names.add(dept.getName());
            }
        }
        return names;
    }

    private static boolean deptScopeHit(List<String> scope, List<String> userDepts) {
        if (scope == null || scope.isEmpty()) {
            return true;
        }
        Set<String> set = new HashSet<>(userDepts == null ? List.of() : userDepts);
        for (String d : scope) {
            if (set.contains(d)) {
                return true;
            }
        }
        return false;
    }

    // ══════════════ 发布判定 ══════════════

    private Map<String, Object> publicationState(PortalHrDocumentDO doc) {
        boolean publishedBefore = doc.publishedBefore();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("published_before", publishedBefore);
        state.put("body_changed", publishedBefore && bodyChanged(doc));
        state.put("current_version", doc.getVersion());
        state.put("revision", computeRevision(doc));
        return state;
    }

    private boolean bodyChanged(PortalHrDocumentDO doc) {
        boolean titleChanged = doc.getDraftTitle() != null && !Objects.equals(doc.getDraftTitle(), doc.getTitle());
        boolean introChanged = doc.getDraftIntro() != null && !Objects.equals(doc.getDraftIntro(), doc.getIntro());
        boolean contentChanged;
        if (doc.getDraftBlocks() != null) {
            contentChanged = !Objects.equals(doc.getDraftBlocks(), doc.getContentBlocks());
        } else if (doc.getDraftContent() != null) {
            contentChanged = !Objects.equals(doc.getDraftContent(), doc.getContentHtml());
        } else {
            contentChanged = false;
        }
        return titleChanged || introChanged || contentChanged;
    }

    private boolean hasDraft(PortalHrDocumentDO doc) {
        return doc.getDraftContent() != null || doc.getDraftBlocks() != null
                || doc.getDraftTitle() != null || doc.getDraftIntro() != null
                || doc.getDraftVersion() != null || doc.getDraftRequiresSignoff() != null
                || doc.getDraftDeptScope() != null || doc.getDraftCoverImage() != null
                || doc.getDraftTheme() != null || doc.getDraftTags() != null
                || doc.getDraftTocEnabled() != null || doc.getDraftTocPosition() != null
                || doc.getDraftCover() != null || doc.getDraftDownloadPdfEnabled() != null
                || doc.getDraftSignoff() != null;
    }

    private void clearDrafts(PortalHrDocumentDO doc) {
        doc.setDraftContent(null);
        doc.setDraftBlocks(null);
        doc.setDraftTitle(null);
        doc.setDraftRequiresSignoff(null);
        doc.setDraftDeptScope(null);
        doc.setDraftIntro(null);
        doc.setDraftCoverImage(null);
        doc.setDraftTheme(null);
        doc.setDraftTags(null);
        doc.setDraftTocEnabled(null);
        doc.setDraftTocPosition(null);
        doc.setDraftCover(null);
        doc.setDraftDownloadPdfEnabled(null);
        doc.setDraftSignoff(null);
        doc.setDraftVersion(null);
    }

    private boolean versionTaken(Long documentId, String version) {
        return versionMapper.selectVersionNamesByDocumentId(documentId).stream()
                .anyMatch(v -> v != null && v.trim().equals(version));
    }

    /** 发布确认凭据：对发布相关字段做规范化 sha256（与 OA 前端共用，只要求 OA 内部一致） */
    private String computeRevision(PortalHrDocumentDO doc) {
        Map<String, Object> values = new TreeMap<>();
        values.put("document_id", doc.getId());
        values.put("current_version", doc.getVersion());
        values.put("published_at", revisionTime(doc.getPublishedAt()));
        values.put("offline_at", revisionTime(doc.getOfflineAt()));
        values.put("deleted_at", revisionTime(doc.getDeletedAt()));
        values.put("category", doc.getCategory());
        values.put("topic", doc.getTopic());
        values.put("title", HrDocumentValidator.promoted(doc.getDraftTitle(), doc.getTitle()));
        values.put("intro", HrDocumentValidator.promoted(doc.getDraftIntro(), doc.getIntro()));
        values.put("requires_signoff", HrDocumentValidator.promoted(doc.getDraftRequiresSignoff(), doc.getRequiresSignoff()));
        values.put("dept_scope", HrDocumentValidator.promoted(doc.getDraftDeptScope(), doc.getDeptScope()));
        values.put("cover_image", HrDocumentValidator.promoted(doc.getDraftCoverImage(), doc.getCoverImage()));
        values.put("theme", HrDocumentValidator.promoted(doc.getDraftTheme(), doc.getTheme()));
        values.put("tags", HrDocumentValidator.promoted(doc.getDraftTags(), doc.getTags()));
        values.put("toc_enabled", HrDocumentValidator.promoted(doc.getDraftTocEnabled(), doc.getTocEnabled()));
        values.put("toc_position", HrDocumentValidator.promoted(doc.getDraftTocPosition(), doc.getTocPosition()));
        values.put("cover", HrDocumentValidator.promoted(doc.getDraftCover(), doc.getCover()));
        values.put("download_pdf_enabled", HrDocumentValidator.promoted(doc.getDraftDownloadPdfEnabled(), doc.getDownloadPdfEnabled()));
        values.put("signoff", HrDocumentValidator.promoted(doc.getDraftSignoff(), doc.getSignoff()));
        if (doc.getDraftBlocks() != null) {
            values.put("format", "blocks");
            values.put("body", doc.getDraftBlocks());
        } else if (doc.getDraftContent() != null) {
            values.put("format", "html");
            values.put("body", doc.getDraftContent());
        } else if (doc.getContentBlocks() != null) {
            values.put("format", "blocks");
            values.put("body", doc.getContentBlocks());
        } else {
            values.put("format", "html");
            values.put("body", doc.getContentHtml());
        }
        return sha256Hex(JsonUtils.toJsonString(canonicalJson(values)));
    }

    /** JSON 对象的键顺序不影响内容；凭据与 Python sort_keys=True 一样递归规范化。 */
    private Object canonicalJson(Object value) {
        if(value instanceof Map<?,?> input){Map<String,Object> sorted=new TreeMap<>();input.forEach((key,item)->sorted.put(key.toString(),canonicalJson(item)));return sorted;}
        if(value instanceof List<?> input)return input.stream().map(this::canonicalJson).toList();
        return value;
    }
    private String revisionTime(LocalDateTime value){return value==null?null:value.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();}

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    // ══════════════ 草稿版本快照 ══════════════

    private void snapshotDraftVersion(PortalHrDocumentDO doc, String savedBy) {
        if (doc.getDraftContent() == null && doc.getDraftBlocks() == null) {
            return;
        }
        String snapshotTitle = doc.getDraftTitle() != null ? doc.getDraftTitle() : doc.getTitle();
        PortalHrDocumentDraftVersionDO latest = draftVersionMapper.selectLatestByDocumentId(doc.getId());
        if (latest != null && Objects.equals(latest.getTitle(), snapshotTitle)
                && Objects.equals(latest.getContentHtml(), doc.getDraftContent())
                && Objects.equals(latest.getContentBlocks(), doc.getDraftBlocks())) {
            return;
        }
        PortalHrDocumentDraftVersionDO version = new PortalHrDocumentDraftVersionDO();
        version.setDocumentId(doc.getId());
        version.setSlug(doc.getSlug());
        version.setTitle(snapshotTitle);
        version.setContentHtml(doc.getDraftContent());
        version.setContentBlocks(doc.getDraftBlocks());
        version.setSavedBy(savedBy);
        draftVersionMapper.insert(version);
        // 超限清理 best-effort：清最旧，失败不拖垮保存主流程
        try {
            draftVersionMapper.deleteBeyondKeep(doc.getId(), DRAFT_VERSIONS_KEEP);
        } catch (Exception e) {
            log.warn("HR 文档 {} 草稿版本超限清理失败（快照已存）", doc.getId(), e);
        }
    }

    // ══════════════ 内部 ══════════════

    private void validateCreateFields(PortalHrDocumentSaveReqVO reqVO) {
        HrDocumentValidator.requireValidCategory(reqVO.getCategory());
        HrDocumentValidator.requireValidTopic(reqVO.getTopic());
        HrDocumentValidator.requireValidIntro(reqVO.getIntro());
        HrDocumentValidator.requireValidCoverImage(reqVO.getCoverImage());
        HrDocumentValidator.requireValidTheme(reqVO.getTheme());
        HrDocumentValidator.requireValidTocPosition(reqVO.getTocPosition());
        HrDocumentValidator.requireValidCover(reqVO.getCover());
        HrDocumentValidator.requireValidSignoff(reqVO.getSignoff());
        HrDocumentValidator.requireValidTags(reqVO.getTags());
    }

    private PortalHrDocumentDO getOr404(Long id, boolean allowDeleted) {
        PortalHrDocumentDO doc = id == null ? null : documentMapper.selectById(id);
        if (doc == null || (doc.isDeleted() && !allowDeleted)) {
            throw exception(HRDOC_NOT_EXISTS);
        }
        return doc;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
