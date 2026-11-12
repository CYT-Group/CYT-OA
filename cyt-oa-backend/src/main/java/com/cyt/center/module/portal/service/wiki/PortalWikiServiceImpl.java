package com.cyt.center.module.portal.service.wiki;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiPageReqVO;
import com.cyt.center.module.portal.controller.admin.wiki.vo.PortalWikiSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO;
import com.cyt.center.module.portal.dal.postgresql.wiki.PortalWikiMapper;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO.STATUS_DRAFT;
import static com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO.STATUS_PENDING;
import static com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO.STATUS_PUBLISHED;
import static com.cyt.center.module.portal.dal.dataobject.wiki.PortalWikiDO.STATUS_REJECTED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.WIKI_FORBIDDEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.WIKI_NOT_EXISTS;

/**
 * 知识库用例实现（逐条对齐 Portal 的 application/wiki.py）。
 */
@Slf4j
@Service
public class PortalWikiServiceImpl implements PortalWikiService {

    private static final String AUDIT_TABLE = "portal_wiki";
    private static final String STATUS_ALL = "all";

    @Resource
    private PortalWikiMapper wikiMapper;
    @Resource
    private PortalAccess access;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PortalAudit audit;

    @Override
    public PageResult<PortalWikiDO> page(Long userId, PortalWikiPageReqVO pageReqVO) {
        String status = blankToDefault(pageReqVO.getStatus(), STATUS_PUBLISHED);
        PortalOperator operator = operators.resolve(userId);
        boolean reviewer = access.canViewAnyStatus(userId);
        boolean all = STATUS_ALL.equals(status);
        // 口径与动态一致：all 只给控制台用，普通人拿到的「全部」= 已发布 + 自己写的
        String visibleTo = reviewer || STATUS_PUBLISHED.equals(status) ? null : operator.username();
        return wikiMapper.selectPage(pageReqVO, all ? null : status, blankToNull(pageReqVO.getCategory()),
                blankToNull(pageReqVO.getDepartment()), blankToNull(pageReqVO.getQ()), visibleTo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalWikiDO getForRead(Long userId, Long id) {
        PortalWikiDO row = requireVisible(userId, id);
        // 浏览即计数：setSql 原子自增后读回，返回的是库里最终的数，不是本地加一
        wikiMapper.increaseViewCount(id);
        return wikiMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalWikiDO create(Long userId, PortalWikiSaveReqVO createReqVO) {
        PortalOperator operator = operators.resolve(userId);
        // 状态派发只有这一条路：显式草稿 / 审阅者免审 / 其余走审批。
        // 入参的 published、rejected 一律不信，否则任何人 POST 就能造出一篇 published。
        String status;
        if (STATUS_DRAFT.equals(blankToNull(createReqVO.getStatus()))) {
            status = STATUS_DRAFT;
        } else if (access.canViewAnyStatus(userId)) {
            status = STATUS_PUBLISHED;
        } else {
            status = STATUS_PENDING;
        }
        PortalWikiDO row = new PortalWikiDO();
        row.setTitle(createReqVO.getTitle());
        row.setContent(createReqVO.getContent());
        row.setCategory(blankToNull(createReqVO.getCategory()));
        row.setDepartment(blankToNull(createReqVO.getDepartment()));
        row.setDocUrl(blankToNull(createReqVO.getDocUrl()));
        row.setTags(createReqVO.getTags());
        row.setAuthorUsername(operator.username());
        row.setStatus(status);
        row.setViewCount(0);
        wikiMapper.insert(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalWikiDO update(Long userId, Long id, PortalWikiSaveReqVO updateReqVO) {
        PortalWikiDO row = wikiMapper.selectById(id);
        if (row == null) {
            throw exception(WIKI_NOT_EXISTS);
        }
        PortalOperator operator = operators.resolve(userId);
        boolean editor = access.canEditAny(userId);
        if (!editor && !Objects.equals(row.getAuthorUsername(), operator.username())) {
            throw exception(WIKI_FORBIDDEN);
        }
        row.setTitle(updateReqVO.getTitle());
        row.setContent(updateReqVO.getContent());
        row.setCategory(blankToNull(updateReqVO.getCategory()));
        row.setDepartment(blankToNull(updateReqVO.getDepartment()));
        row.setDocUrl(blankToNull(updateReqVO.getDocUrl()));
        row.setTags(updateReqVO.getTags());
        // 工作流完整性同 news：没全量编辑权的人改已发布文档，退回 pending 重走审批
        if (!editor && STATUS_PUBLISHED.equals(row.getStatus())) {
            row.setStatus(STATUS_PENDING);
        }
        wikiMapper.updateById(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long userId, Long id, boolean approve, String comment) {
        PortalWikiDO row = wikiMapper.selectById(id);
        if (row == null) {
            throw exception(WIKI_NOT_EXISTS);
        }
        String before = snapshot(row);
        row.setStatus(approve ? STATUS_PUBLISHED : STATUS_REJECTED);
        wikiMapper.updateById(row);
        // Portal 那边审批流水落 portal_approvals，那张表按契约不再迁（docs/Portal管理面迁移契约.md 表清单 D）；
        // 动作键沿用内容行的裸键，与 news 的 approve/reject 同一套
        audit.record(userId, approve ? "approve" : "reject", AUDIT_TABLE, id, before, snapshot(row), comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        PortalWikiDO row = wikiMapper.selectById(id);
        if (row == null) {
            throw exception(WIKI_NOT_EXISTS);
        }
        wikiMapper.deleteById(id);
        audit.record(userId, "delete", AUDIT_TABLE, id, snapshot(row), null, null);
    }

    // ══════════════ 内部 ══════════════

    private PortalWikiDO requireVisible(Long userId, Long id) {
        PortalWikiDO row = id == null ? null : wikiMapper.selectById(id);
        if (row == null) {
            throw exception(WIKI_NOT_EXISTS);
        }
        if (!STATUS_PUBLISHED.equals(row.getStatus()) && !access.canViewAnyStatus(userId)) {
            PortalOperator operator = operators.resolve(userId);
            if (!Objects.equals(row.getAuthorUsername(), operator.username())) {
                throw exception(WIKI_NOT_EXISTS);
            }
        }
        return row;
    }

    /** 行的审计快照：键名沿用 Portal 的 snake_case；正文按摘要存，避免整篇文档写进日志 */
    private String snapshot(PortalWikiDO row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", row.getTitle());
        snapshot.put("content", PortalAudit.summarize(row.getContent()));
        snapshot.put("category", row.getCategory());
        snapshot.put("department", row.getDepartment());
        snapshot.put("doc_url", row.getDocUrl());
        snapshot.put("tags", row.getTags() == null ? List.of() : row.getTags());
        snapshot.put("author_username", row.getAuthorUsername());
        snapshot.put("status", row.getStatus());
        snapshot.put("view_count", row.getViewCount());
        return JsonUtils.toJsonString(snapshot);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        String cleaned = blankToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

}
