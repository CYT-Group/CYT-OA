package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.dal.mysql.rollout.PortalEntryRolloutRepository;
import com.cyt.center.module.portal.service.support.PortalAudit;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 上线开关只控制管理页面入口，不改角色授权、已发布内容或业务调度。 */
@Service
@RequiredArgsConstructor
public class PortalEntryRolloutService {
    private final PortalEntryRolloutRepository repository;
    private final CurrentUser currentUser;
    private final PortalAudit audit;

    // 管理中心与上线设置入口固定保留，不能通过配置隐藏自己。
    public static final List<String> ENTRY_KEYS =
            List.of(
                    "portal-roles",
                    "portal-ai",
                    "portal-analytics",
                    "portal-news",
                    "portal-wiki",
                    "portal-brand-docs",
                    "portal-samples",
                    "portal-supplies",
                    "portal-training",
                    "portal-import",
                    "portal-events",
                    "portal-products",
                    "portal-categories",
                    "portal-api-costs",
                    "portal-hr-documents",
                    "portal-signoff",
                    "portal-contracts",
                    "portal-celebrations",
                    "portal-employee-info",
                    "portal-resumes");

    public record Settings(
            Map<String, Boolean> entries,
            long revision,
            String updatedBy,
            LocalDateTime updatedAt) {}

    public record Entries(Map<String, Boolean> entries, long revision) {}

    public Entries entries() {
        if (!currentUser.require().canAccessConsole())
            throw AuthException.forbidden("仅管理控制台用户可读取入口设置");
        var row = repository.read(false);
        return new Entries(effective(row.entries()), row.revision());
    }

    public Settings settings() {
        requireAdmin();
        return view(repository.read(false));
    }

    private void requireAdmin() {
        if (!currentUser.require().isAdmin()) throw AuthException.forbidden("仅系统管理员可维护入口上线设置");
    }

    private Map<String, Boolean> effective(Map<String, Object> stored) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String key : ENTRY_KEYS) {
            Object value = stored.get(key);
            if (value != null && !(value instanceof Boolean))
                throw new BizException("入口上线设置格式无效，请联系系统管理员");
            result.put(key, value == null || Boolean.TRUE.equals(value));
        }
        return result;
    }

    private Settings view(PortalEntryRolloutRepository.Settings row) {
        return new Settings(
                effective(row.entries()), row.revision(), row.updatedBy(), row.updatedAt());
    }

    @Transactional
    public Settings save(Long revision, Map<String, Object> requested) {
        requireAdmin();
        if (revision == null
                || revision < 0
                || requested == null
                || requested.isEmpty()
                || requested.size() > ENTRY_KEYS.size()) throw new BizException("缺少有效的版本号或入口开关");
        for (var entry : requested.entrySet()) {
            if (!ENTRY_KEYS.contains(entry.getKey())) throw new BizException("包含不可配置的入口");
            if (!(entry.getValue() instanceof Boolean)) throw new BizException("入口状态必须为开启或关闭");
        }
        repository.initialize();
        var before = repository.read(true);
        if (before.revision() != revision) throw new BizException("上线设置已被其他管理员更新，请重新加载后再保存");
        var entries = effective(before.entries());
        Map<String, Boolean> changedBefore = new LinkedHashMap<>(),
                changedAfter = new LinkedHashMap<>();
        requested.forEach(
                (key, value) -> {
                    boolean enabled = (Boolean) value;
                    if (entries.get(key) != enabled) {
                        changedBefore.put(key, entries.get(key));
                        changedAfter.put(key, enabled);
                        entries.put(key, enabled);
                    }
                });
        if (changedAfter.isEmpty()) return view(before);
        var actor = currentUser.require();
        if (!repository.update(entries, revision, actor.username()))
            throw new BizException("上线设置已变化，请重新加载后再保存");
        audit.record(
                actor.userId(),
                "portal_entry_rollout",
                "portal_entry_rollout",
                1,
                JsonUtils.toJsonString(changedBefore),
                JsonUtils.toJsonString(changedAfter),
                "调整 " + changedAfter.size() + " 个管理入口");
        return view(repository.read(false));
    }
}
