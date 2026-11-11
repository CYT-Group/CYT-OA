package com.cyt.center.module.portal.service.events;

import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.util.json.JsonUtils;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventPageReqVO;
import com.cyt.center.module.portal.controller.admin.events.vo.PortalEventSaveReqVO;
import com.cyt.center.module.portal.dal.dataobject.events.PortalEventDO;
import com.cyt.center.module.portal.dal.mysql.events.PortalEventMapper;
import com.cyt.center.module.portal.service.support.PortalAudit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.EVENT_NOT_EXISTS;

/**
 * 展会/活动日历用例实现（逐条对齐 Portal 的 application/events.py）。
 */
@Slf4j
@Service
public class PortalEventServiceImpl implements PortalEventService {

    private static final String AUDIT_TABLE = "portal_events";
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 100;

    @Resource
    private PortalEventMapper eventMapper;
    @Resource
    private PortalAudit audit;

    @Override
    public PageResult<PortalEventDO> page(PortalEventPageReqVO pageReqVO) {
        return eventMapper.selectPage(pageReqVO, blankToNull(pageReqVO.getQ()));
    }

    @Override
    public PortalEventDO get(Long id) {
        PortalEventDO row = id == null ? null : eventMapper.selectById(id);
        if (row == null) {
            throw exception(EVENT_NOT_EXISTS);
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalEventDO create(PortalEventSaveReqVO createReqVO) {
        PortalEventDO row = new PortalEventDO();
        row.setTitle(createReqVO.getTitle().trim());
        row.setDescription(blankToNull(createReqVO.getDescription()));
        row.setLocation(blankToNull(createReqVO.getLocation()));
        row.setStartDate(createReqVO.getStartDate() == null ? LocalDate.now() : createReqVO.getStartDate());
        row.setEndDate(createReqVO.getEndDate());
        row.setEventType(blankToDefault(createReqVO.getEventType(), "exhibition"));
        row.setStatus(PortalEventDO.STATUS_PUBLISHED);
        eventMapper.insert(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalEventDO update(Long id, PortalEventSaveReqVO updateReqVO) {
        PortalEventDO row = id == null ? null : eventMapper.selectById(id);
        if (row == null) {
            throw exception(EVENT_NOT_EXISTS);
        }
        row.setTitle(updateReqVO.getTitle().trim());
        row.setDescription(blankToNull(updateReqVO.getDescription()));
        row.setLocation(blankToNull(updateReqVO.getLocation()));
        // Portal：start_date 解析为空保留原值，end_date 允许清空
        if (updateReqVO.getStartDate() != null) {
            row.setStartDate(updateReqVO.getStartDate());
        }
        row.setEndDate(updateReqVO.getEndDate());
        row.setEventType(blankToDefault(updateReqVO.getEventType(), "exhibition"));
        eventMapper.updateById(row);
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        PortalEventDO row = id == null ? null : eventMapper.selectById(id);
        if (row == null) {
            throw exception(EVENT_NOT_EXISTS);
        }
        eventMapper.deleteById(id);
        audit.record(userId, "delete", AUDIT_TABLE, id, snapshot(row), null, null);
    }

    @Override
    public List<PortalEventDO> listPublished(Integer limit) {
        return eventMapper.selectPublished(clampLimit(limit, DEFAULT_LIMIT));
    }

    @Override
    public PortalEventDO getPublished(Long id) {
        PortalEventDO row = id == null ? null : eventMapper.selectById(id);
        // 不存在 / 未发布统一 404（Portal 的 NotFound 不暴露存在性）
        if (row == null || !PortalEventDO.STATUS_PUBLISHED.equals(row.getStatus())) {
            throw exception(EVENT_NOT_EXISTS);
        }
        return row;
    }

    // ══════════════ 内部 ══════════════

    private static int clampLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /** 行的审计快照：键名沿用 Portal 的 snake_case；介绍按摘要存 */
    private String snapshot(PortalEventDO row) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", row.getTitle());
        snapshot.put("description", PortalAudit.summarize(row.getDescription()));
        snapshot.put("location", row.getLocation());
        snapshot.put("start_date", row.getStartDate() == null ? null : row.getStartDate().toString());
        snapshot.put("end_date", row.getEndDate() == null ? null : row.getEndDate().toString());
        snapshot.put("event_type", row.getEventType());
        snapshot.put("status", row.getStatus());
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
