package com.cyt.center.module.portal.service.news;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.exception.ErrorCode;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsApproveReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsPageReqVO;
import com.cyt.center.module.portal.controller.admin.news.vo.PortalNewsSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO;
import com.cyt.center.module.portal.dal.mysql.news.PortalNewsMapper;
import com.cyt.center.module.portal.service.support.PortalAccess;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO.STATUS_PENDING;
import static com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO.STATUS_PUBLISHED;
import static com.cyt.center.module.portal.dal.dataobject.news.PortalNewsDO.STATUS_REJECTED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_FORBIDDEN;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.NEWS_NOT_EXISTS;

/**
 * 公司动态用例实现（逐条对齐 Portal 的 application/news.py）。
 */
@Slf4j
@Service
public class PortalNewsServiceImpl implements PortalNewsService {

    private static final String AUDIT_TABLE = "portal_news";
    private static final String STATUS_ALL = "all";

    @Resource
    private PortalNewsMapper newsMapper;
    @Resource
    private PortalAccess access;
    @Resource
    private PortalOperatorResolver operators;
    @Resource
    private PortalAudit audit;
    @Resource
    private com.cyt.center.module.portal.service.push.PortalContentPushService contentPush;

    @Override
    public PageResult<PortalNewsDO> page(Long userId, PortalNewsPageReqVO pageReqVO) {
        String status = blankToDefault(pageReqVO.getStatus(), STATUS_PUBLISHED);
        PortalOperator operator = operators.resolve(userId);
        boolean reviewer = access.canViewAnyStatus(userId);
        boolean all = STATUS_ALL.equals(status);
        // published 全员可见；指定其它状态时，普通人只看自己写的那几条。
        // all 是给控制台用的「全部」：审阅者真的全看，普通人 = 已发布 + 自己的，不会因为传了 all 就翻出别人的草稿
        String visibleTo = reviewer || STATUS_PUBLISHED.equals(status) ? null : operator.username();
        return newsMapper.selectPage(pageReqVO, all ? null : status, blankToNull(pageReqVO.getCategory()),
                blankToNull(pageReqVO.getQ()), visibleTo);
    }

    @Override
    public PortalNewsDO requireVisible(Long userId, Long id, ErrorCode notFound) {
        PortalNewsDO row = id == null ? null : newsMapper.selectById(id);
        if (row == null) {
            throw exception(notFound);
        }
        if (!STATUS_PUBLISHED.equals(row.getStatus()) && !access.canViewAnyStatus(userId)) {
            PortalOperator operator = operators.resolve(userId);
            if (!Objects.equals(row.getAuthorUsername(), operator.username())) {
                throw exception(notFound);
            }
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalNewsDO create(Long userId, PortalNewsSaveReqVO createReqVO) {
        PortalOperator operator = operators.resolve(userId);
        boolean autoPublish = access.canViewAnyStatus(userId);
        boolean wantNotify = Boolean.TRUE.equals(createReqVO.getNotifyWecom());
        PortalNewsDO row = new PortalNewsDO();
        row.setTitle(createReqVO.getTitle());
        row.setSummary(blankToNull(createReqVO.getSummary()));
        // 正文列 NOT NULL：模板化稿件可以先空着，内容在 blocks 里
        row.setContent(createReqVO.getContent() == null ? "" : createReqVO.getContent());
        row.setCategory(blankToNull(createReqVO.getCategory()));
        row.setCoverImage(blankToNull(createReqVO.getCoverImage()));
        row.setTemplateType(blankToDefault(createReqVO.getTemplateType(), "simple"));
        row.setBlocks(createReqVO.getBlocks());
        row.setAuthor(operator.displayNameOrUsername());
        row.setAuthorUsername(operator.username());
        row.setStatus(autoPublish ? STATUS_PUBLISHED : STATUS_PENDING);
        row.setIsFeatured(Boolean.TRUE.equals(createReqVO.getIsFeatured()));
        row.setPublishedAt(autoPublish ? LocalDateTime.now() : null);
        // 有全量编辑权的角色这一趟就发布了，推送在本请求里直接处理，不必留意图；
        // 走审批的稿才把意图存下来，等审核人二次确认时消费
        row.setPendingNotifyWecom(wantNotify && !autoPublish);
        row.setPendingNotifyDeptScope(wantNotify && !autoPublish ? createReqVO.getNotifyDeptScope() : null);
        newsMapper.insert(row);
        if (autoPublish && wantNotify) schedulePush(row, createReqVO.getNotifyDeptScope(), userId);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalNewsDO update(Long userId, Long id, PortalNewsSaveReqVO updateReqVO) {
        PortalNewsDO row = newsMapper.selectById(id);
        if (row == null) {
            throw exception(NEWS_NOT_EXISTS);
        }
        PortalOperator operator = operators.resolve(userId);
        boolean editor = access.canEditAny(userId);
        if (!editor && !Objects.equals(row.getAuthorUsername(), operator.username())) {
            throw exception(NEWS_FORBIDDEN);
        }
        row.setTitle(updateReqVO.getTitle());
        row.setSummary(blankToNull(updateReqVO.getSummary()));
        row.setContent(updateReqVO.getContent() == null ? "" : updateReqVO.getContent());
        row.setCategory(blankToNull(updateReqVO.getCategory()));
        row.setCoverImage(blankToNull(updateReqVO.getCoverImage()));
        row.setTemplateType(blankToDefault(updateReqVO.getTemplateType(), "simple"));
        row.setBlocks(updateReqVO.getBlocks());
        row.setIsFeatured(Boolean.TRUE.equals(updateReqVO.getIsFeatured()));
        // 工作流完整性：没有全量编辑权的人改已发布稿，退回 pending 重走审批，
        // 防的是「提交合规内容→通过→悄悄改成有问题内容」这条绕审批的路
        if (!editor && STATUS_PUBLISHED.equals(row.getStatus())) {
            row.setStatus(STATUS_PENDING);
            row.setPublishedAt(null);
        }
        if (updateReqVO.getNotifyWecom() != null) {
            boolean notify = Boolean.TRUE.equals(updateReqVO.getNotifyWecom());
            row.setPendingNotifyWecom(notify && !STATUS_PUBLISHED.equals(row.getStatus()));
            row.setPendingNotifyDeptScope(notify && !STATUS_PUBLISHED.equals(row.getStatus()) ? updateReqVO.getNotifyDeptScope() : null);
        }
        newsMapper.updateById(row);
        if (STATUS_PUBLISHED.equals(row.getStatus()) && Boolean.TRUE.equals(updateReqVO.getNotifyWecom()))
            schedulePush(row, updateReqVO.getNotifyDeptScope(), userId);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        PortalNewsDO row = newsMapper.selectById(id);
        if (row == null) {
            throw exception(NEWS_NOT_EXISTS);
        }
        newsMapper.deleteById(id);
        // 内容行只有删除留审计，编辑史不进门（Portal 口径）
        audit.record(userId, "delete", AUDIT_TABLE, id, snapshot(row), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long userId, Long id, PortalNewsApproveReqVO approveReqVO) {
        PortalNewsDO row = newsMapper.selectById(id);
        if (row == null) {
            throw exception(NEWS_NOT_EXISTS);
        }
        String before = snapshot(row);
        boolean doPush;
        List<String> deptScope;
        if (approveReqVO != null && approveReqVO.getNotifyWecom() != null) {
            doPush = approveReqVO.getNotifyWecom();
            deptScope = approveReqVO.getNotifyDeptScope();
        } else {
            doPush = Boolean.TRUE.equals(row.getPendingNotifyWecom());
            deptScope = row.getPendingNotifyDeptScope();
        }
        row.setStatus(STATUS_PUBLISHED);
        row.setPublishedAt(LocalDateTime.now());
        // 一次性消费：不清就会在下次 approve 重复推（Portal 第七轮评审留下的口径）
        row.setPendingNotifyWecom(false);
        row.setPendingNotifyDeptScope(null);
        newsMapper.updateById(row);
        if (doPush) {
            schedulePush(row, deptScope, userId);
        }
        audit.record(userId, "approve", AUDIT_TABLE, id, before, snapshot(row),
                approveReqVO == null ? null : approveReqVO.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long userId, Long id, String comment) {
        PortalNewsDO row = newsMapper.selectById(id);
        if (row == null) {
            throw exception(NEWS_NOT_EXISTS);
        }
        String before = snapshot(row);
        row.setStatus(STATUS_REJECTED);
        // 驳回一并清掉推送意图，否则作者改稿重提后被 approve 消费的是旧决定，要推得重新勾
        row.setPendingNotifyWecom(false);
        row.setPendingNotifyDeptScope(null);
        newsMapper.updateById(row);
        audit.record(userId, "reject", AUDIT_TABLE, id, before, snapshot(row), comment);
    }

    // ══════════════ 内部 ══════════════

    /** 同事务保存明确推送意图，提交后才异步发送；失败和24小时冷却结果均可查询。 */
    private void schedulePush(PortalNewsDO row, List<String> deptScope, Long userId) {
        contentPush.news(userId, row, deptScope, false, false);
    }

    /** 行的审计快照：键名沿用 Portal 的 snake_case，与 B1 搬来的历史行对齐；正文按摘要存 */
    private String snapshot(PortalNewsDO row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", row.getTitle());
        snapshot.put("summary", PortalAudit.summarize(row.getSummary()));
        snapshot.put("content", PortalAudit.summarize(row.getContent()));
        snapshot.put("category", row.getCategory());
        snapshot.put("cover_image", row.getCoverImage());
        snapshot.put("template_type", row.getTemplateType());
        snapshot.put("blocks", row.getBlocks() == null ? Map.of() : row.getBlocks());
        snapshot.put("author", row.getAuthor());
        snapshot.put("author_username", row.getAuthorUsername());
        snapshot.put("status", row.getStatus());
        snapshot.put("is_featured", row.getIsFeatured());
        snapshot.put("published_at", row.getPublishedAt() == null ? null : row.getPublishedAt().toString());
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
