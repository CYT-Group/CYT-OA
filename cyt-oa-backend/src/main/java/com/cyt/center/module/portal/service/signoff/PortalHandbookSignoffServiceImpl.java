package com.cyt.center.module.portal.service.signoff;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyt.center.module.portal.controller.admin.signoff.vo.PortalSignoffSignReqVO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.dataobject.signoff.PortalHandbookSignoffDO;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentAliasMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentMapper;
import com.cyt.center.module.portal.dal.postgresql.signoff.PortalHandbookSignoffMapper;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.permission.PermissionApi;
import com.cyt.center.module.system.api.permission.dto.DeptDataPermissionRespDTO;
import com.cyt.center.module.portal.domain.signoff.PortalSignoffIdentity;
import com.cyt.center.module.portal.domain.signoff.PortalSignoffTargetUser;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_DEPT_FORBIDDEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_NOT_SIGNABLE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_REF_REQUIRED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_UNKNOWN_DOC;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_VERSION_MISMATCH;

/**
 * 制度签收用例实现（对齐 Portal 的 application/handbook_signoff.py + domain/handbook.py）。
 *
 * 催签（notify-unsigned）依赖企微推送，随推送切片落地。
 */
@Service
public class PortalHandbookSignoffServiceImpl implements PortalHandbookSignoffService {

    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "handbook", "employee-handbook", "incentive", "hr-incentive");
    private static final Map<String, String> LEGACY_BY_SLUG = Map.of(
            "employee-handbook", "handbook", "hr-incentive", "incentive");
    private static final Map<String, String> LEGACY_FALLBACK_VERSIONS = Map.of(
            "incentive", "v2.0", "handbook", "v1.0");

    @Resource
    private PortalHandbookSignoffMapper signoffMapper;
    @Resource
    private com.cyt.center.module.portal.dal.postgresql.signoff.PortalSignoffDocumentRepository signoffDocuments;
    @Resource
    private PortalHrDocumentMapper documentMapper;
    @Resource
    private PortalHrDocumentAliasMapper aliasMapper;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private com.cyt.center.module.portal.dal.postgresql.signoff.PortalSignoffTargetRepository signoffTargets;
    @Resource
    private com.cyt.center.core.integration.WecomGatewayClient notificationGateway;
    @Resource
    private com.cyt.center.module.portal.dal.postgresql.signoff.PortalSignoffNotificationRepository notificationRepository;
    @Resource
    private com.cyt.center.module.portal.service.support.PortalAudit notificationAudit;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalHandbookSignoffDO sign(Long userId, PortalSignoffSignReqVO reqVO, String ip, String userAgent) {
        PortalOperator operator = operators.resolve(userId);
        String document = reqVO.getDocument();
        Long documentId = reqVO.getDocumentId();
        requireSingleReference(document, documentId);

        ResolvedDoc resolved = resolveSignableDoc(document, documentId, true);
        if (resolved.version() == null) {
            throw exception(SIGNOFF_NOT_SIGNABLE);
        }
        checkDeptAccess(resolved.doc(), userId);
        if (!resolved.version().equals(reqVO.getVersion())) {
            throw exception(SIGNOFF_VERSION_MISMATCH, resolved.version());
        }

        String storedDocument = document != null ? normalizeDocument(document) : ("article:" + resolved.doc().getId());
        Long resolvedDocumentId = resolved.doc() != null ? resolved.doc().getId() : documentId;
        List<String> legacyCandidates = new ArrayList<>();
        legacyCandidates.add(storedDocument);
        if (document != null) {
            legacyCandidates.add(document);
        }

        PortalHandbookSignoffDO existing = signoffMapper.selectExisting(userId, resolvedDocumentId,
                legacyCandidates, reqVO.getVersion());
        if (existing != null) {
            return existing;
        }
        PortalHandbookSignoffDO record = new PortalHandbookSignoffDO();
        record.setUserId(userId);
        record.setUsername(operator.username());
        record.setDocumentId(resolvedDocumentId);
        record.setDocument(storedDocument);
        record.setVersion(reqVO.getVersion());
        record.setIp(ip);
        record.setUserAgent(truncate(userAgent, 255));
        try {
            signoffMapper.insert(record);
        } catch (DuplicateKeyException e) {
            record = signoffMapper.selectExisting(userId, resolvedDocumentId, legacyCandidates, reqVO.getVersion());
        }
        return record;
    }

    @Override
    public MySignoffs mySignoffs(Long userId) {
        operators.resolve(userId);
        List<PortalHandbookSignoffDO> items = signoffMapper.selectByUserId(userId);
        Map<String, String> currentVersions = new LinkedHashMap<>(publishedSignableVersions());
        for (Map.Entry<String, String> e : LEGACY_ALIASES.entrySet()) {
            if (currentVersions.containsKey(e.getValue())) {
                currentVersions.putIfAbsent(e.getKey(), currentVersions.get(e.getValue()));
            }
        }
        for (Map.Entry<String, String> e : LEGACY_FALLBACK_VERSIONS.entrySet()) {
            currentVersions.putIfAbsent(e.getKey(), e.getValue());
        }
        return new MySignoffs(items, currentVersions);
    }

    @Override
    public Map<String, Object> stats(String document, Long documentId, String version) {
        requireSingleReference(document, documentId);
        ResolvedDoc resolved = resolveSignableDoc(document, documentId, false);
        String targetVersion = version != null && !version.isBlank() ? version : resolved.version();
        if (targetVersion == null) {
            throw exception(SIGNOFF_UNKNOWN_DOC);
        }
        if(documentId!=null&&resolved.doc()==null)throw exception(SIGNOFF_UNKNOWN_DOC);
        List<String> aliases=resolved.doc()==null?List.of():aliasMapper.selectByDocumentId(resolved.doc().getId()).stream().map(row->row.getAlias()).toList();
        List<String> documents=PortalSignoffIdentity.candidates(resolved.doc()==null?null:resolved.doc().getId(),resolved.doc()==null?null:resolved.doc().getSlug(),aliases,document);

        Set<Long> signedIds = signoffMapper.selectSignedUserIds(resolved.doc() != null ? resolved.doc().getId() : documentId,
                documents, targetVersion);
        List<PortalSignoffTargetUser> allUsers = targetUsers(resolved.doc() != null ? resolved.doc().getDeptScope() : null);
        Set<Long> targetIds=new HashSet<>();allUsers.forEach(user->targetIds.add(user.userId()));
        Set<Long> signedTargets=new HashSet<>(signedIds);signedTargets.retainAll(targetIds);
        List<Map<String, String>> notSigned = new ArrayList<>();
        for (PortalSignoffTargetUser u : allUsers) {
            if (!signedTargets.contains(u.userId())) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("username", u.username());
                row.put("display_name", u.displayName());
                row.put("department", u.department());
                notSigned.add(row);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("document", document);
        out.put("document_id", resolved.doc() != null ? resolved.doc().getId() : documentId);
        out.put("version", targetVersion);
        out.put("total_active", allUsers.size());
        out.put("signed_count", signedTargets.size());
        out.put("not_signed_count", notSigned.size());
        out.put("coverage_pct", allUsers.isEmpty() ? 0.0
                : Math.round(signedTargets.size() * 1000.0 / allUsers.size()) / 10.0);
        out.put("not_signed", notSigned);
        return out;
    }

    @Override
    public List<com.cyt.center.module.portal.domain.signoff.PortalSignoffDocumentOption> documentOptions() {
        return signoffDocuments.options();
    }

    @Override
    public Map<String, Object> notifyUnsigned(Long actor, String document, Long documentId, String version, String customMessage) {
        requireSingleReference(document, documentId);
        ResolvedDoc active = resolveSignableDoc(document, documentId, true);
        if (active.version() == null) throw exception(SIGNOFF_NOT_SIGNABLE);
        if (version != null && !version.equals(active.version())) throw exception(SIGNOFF_VERSION_MISMATCH, active.version());
        Map<String, Object> stats = stats(document, documentId, active.version());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> unsigned = (List<Map<String, String>>) stats.get("not_signed");
        Map<String, String> wecomIds = notificationRepository.recipients();
        String title = active.doc() == null ? (document == null ? "制度文档" : document) : active.doc().getTitle();
        String message = "请尽快阅读并签收《" + title + "》" + active.version() + "。请进入员工门户完成签收。";
        if (customMessage != null && !customMessage.isBlank()) {
            if (customMessage.length() > 500) throw new com.cyt.center.core.exception.BizException("催签文案不能超过500字");
            message += "\n" + customMessage.trim();
        }
        List<String> recipients = new ArrayList<>();
        int failed = 0, sent = 0;
        List<String> errors = new ArrayList<>();
        for (Map<String, String> row : unsigned) {
            String userid = wecomIds.get(row.get("username"));
            if (userid == null) failed++;
            else recipients.add(userid);
        }
        if (failed > 0) errors.add(failed + "名未签收人员未绑定企业微信");
        for (int start = 0; start < recipients.size(); start += 100) {
            List<String> batch = recipients.subList(start, Math.min(start + 100, recipients.size()));
            try {
                var result = notificationGateway.call("wecom/messages/send-text", Map.of("content", message, "to_users", batch), java.time.Duration.ofSeconds(30));
                java.util.Set<String> invalid = new java.util.HashSet<>();
                String raw = result.path("invaliduser").asText("");
                if (!raw.isBlank()) invalid.addAll(java.util.Arrays.asList(raw.split("\\|")));
                int rejected = (int) batch.stream().filter(invalid::contains).count();
                sent += batch.size() - rejected; failed += rejected;
                if (rejected > 0) errors.add("一批收件人中有" + rejected + "人不在企微应用可见范围");
            } catch (RuntimeException ex) {
                failed += batch.size(); errors.add("一批催签消息发送失败，请检查企微网关和应用授权");
            }
        }
        Long targetId = active.doc() == null ? documentId : active.doc().getId();
        notificationRepository.log(actor, targetId, unsigned.size(), sent, failed);
        notificationAudit.record(actor, "signoff_notify_unsigned", "portal_signoff_notify_log", targetId, null, "targets=" + unsigned.size() + ",sent=" + sent + ",failed=" + failed, null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("document", document); out.put("documentId", targetId); out.put("version", active.version());
        out.put("targetCount", unsigned.size()); out.put("sent", sent); out.put("failed", failed); out.put("errors", errors);
        return out;
    }

    // ══════════════ 解析 / 权限 ══════════════

    private record ResolvedDoc(PortalHrDocumentDO doc, String version) {
    }

    private ResolvedDoc resolveSignableDoc(String document, Long documentId, boolean activeOnly) {
        if (documentId != null) {
            PortalHrDocumentDO doc = documentMapper.selectById(documentId);
            if (hasPublishedSignoffVersion(doc)) {
                boolean active = doc.publishedBefore() && !doc.isOffline() && !doc.isDeleted();
                if (!activeOnly || active) {
                    return new ResolvedDoc(doc, doc.getVersion());
                }
            }
            return new ResolvedDoc(null, null);
        }
        if (document == null || document.isBlank()) {
            return new ResolvedDoc(null, null);
        }
        String slug = normalizeDocument(document);
        Long managedId = resolveManagedDocumentId(slug);
        if (managedId != null) {
            PortalHrDocumentDO doc = documentMapper.selectById(managedId);
            if (hasPublishedSignoffVersion(doc)) {
                boolean active = doc.publishedBefore() && !doc.isOffline() && !doc.isDeleted();
                if (!activeOnly || active) {
                    return new ResolvedDoc(doc, doc.getVersion());
                }
            }
            // 不可用 CMS 别名不能阻断两篇旧静态制度的历史统计。
            // 执行签收/催签仍 fail closed；统计才继续检查固定 legacy 回退。
            if(activeOnly)return new ResolvedDoc(null, null);
        }
        if (LEGACY_FALLBACK_VERSIONS.containsKey(document)) {
            return new ResolvedDoc(null, LEGACY_FALLBACK_VERSIONS.get(document));
        }
        String legacyKey = LEGACY_BY_SLUG.get(slug);
        if (legacyKey != null && LEGACY_FALLBACK_VERSIONS.containsKey(legacyKey)) {
            return new ResolvedDoc(null, LEGACY_FALLBACK_VERSIONS.get(legacyKey));
        }
        return new ResolvedDoc(null, null);
    }

    private boolean hasPublishedSignoffVersion(PortalHrDocumentDO doc){
        return doc!=null&&doc.publishedBefore()&&Boolean.TRUE.equals(doc.getRequiresSignoff())
                &&doc.getVersion()!=null&&!doc.getVersion().isBlank();
    }

    private Long resolveManagedDocumentId(String slug) {
        Long id = aliasMapper.selectDocumentIdByAlias(slug);
        if (id == null) {
            id = aliasMapper.selectDocumentIdByAlias("/hr-doc/" + slug);
        }
        if (id == null) {
            PortalHrDocumentDO doc = documentMapper.selectBySlug(slug);
            id = doc == null ? null : doc.getId();
        }
        return id;
    }

    private void checkDeptAccess(PortalHrDocumentDO doc, Long userId) {
        if (doc == null || doc.getDeptScope() == null || doc.getDeptScope().isEmpty()) {
            return;
        }
        if (permissionApi.hasAnyRoles(userId, "admin")) {
            return;
        }
        if (!deptScopeHit(doc.getDeptScope(), userDeptNames(userId))) {
            throw exception(SIGNOFF_DEPT_FORBIDDEN);
        }
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
        Set<String> set = new HashSet<>(userDepts == null ? List.of() : userDepts);
        for (String d : scope) {
            if (set.contains(d)) {
                return true;
            }
        }
        return false;
    }

    // ══════════════ 统计目标人群 ══════════════

    private List<PortalSignoffTargetUser> targetUsers(List<String> deptScope) {
        Map<Long,PortalSignoffTargetUser> unique=new LinkedHashMap<>();
        for(var user:signoffTargets.targets(deptScope))unique.putIfAbsent(user.userId(),user);
        return new ArrayList<>(unique.values());
    }

    private Map<String, String> publishedSignableVersions() {
        LambdaQueryWrapper<PortalHrDocumentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalHrDocumentDO::getRequiresSignoff, true)
                .isNotNull(PortalHrDocumentDO::getPublishedAt);
        Map<String, String> versions = new LinkedHashMap<>();
        for (PortalHrDocumentDO doc : documentMapper.selectList(wrapper)) {
            versions.put("article:" + doc.getId(), doc.getVersion());
            if (doc.getSlug() != null) {
                versions.put(doc.getSlug(), doc.getVersion());
            }
        }
        return versions;
    }

    private static String normalizeDocument(String raw) {
        return LEGACY_ALIASES.getOrDefault(raw, raw);
    }

    private static void requireSingleReference(String document, Long documentId) {
        boolean missingBoth = documentId == null && (document == null || document.isBlank());
        boolean suppliedBoth = documentId != null && document != null && !document.isBlank();
        if (missingBoth || suppliedBoth) {
            throw exception(SIGNOFF_REF_REQUIRED);
        }
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

}
