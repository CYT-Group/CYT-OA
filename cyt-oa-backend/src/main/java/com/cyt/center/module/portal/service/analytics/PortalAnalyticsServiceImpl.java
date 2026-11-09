package com.cyt.center.module.portal.service.analytics;

import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalActiveUserRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalAnalyticsRangeReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalAnalyticsSummaryRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalAnalyticsTopReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalDailyTrendRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalPopularPageRespVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalTrackReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalUserHistoryReqVO;
import com.cyt.center.module.portal.controller.admin.analytics.vo.PortalUserHistoryRespVO;
import com.cyt.center.module.portal.dal.dataobject.pageview.PortalPageViewDO;
import com.cyt.center.module.portal.dal.mysql.pageview.PortalPageViewMapper;
import com.cyt.center.module.portal.service.support.PortalOperator;
import com.cyt.center.module.portal.service.support.PortalOperatorResolver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.PAGE_VIEW_URL_REQUIRED;

/**
 * 埋点与看板实现（逐条对齐 Portal 的 application/analytics.py + repositories/analytics.py）。
 *
 * 时间基准用服务器时区（Asia/Shanghai）而不是 UTC：Portal 那边整库是 UTC-naive，
 * B2 的数据迁移已把历史行 +8 小时换算成本地时间，这里的窗口才跟存量对得上。
 */
@Slf4j
@Service
public class PortalAnalyticsServiceImpl implements PortalAnalyticsService {

    private static final int URL_MAX_LEN = 500;
    private static final int TITLE_MAX_LEN = 300;

    @Resource
    private PortalPageViewMapper pageViewMapper;
    @Resource
    private PortalOperatorResolver operators;

    @Override
    public void track(Long userId, PortalTrackReqVO trackReqVO, String userAgent) {
        String pageUrl = trimToLength(trackReqVO.getPageUrl(), URL_MAX_LEN);
        // Portal 的 track 缺 page_url 时回 200 + success=False；OA 的统一信封里没有 success 位，
        // 按入参不合法回错，展示端本来就是 fire-and-forget，不改变它的行为
        if (pageUrl.isEmpty()) {
            throw exception(PAGE_VIEW_URL_REQUIRED);
        }
        PortalOperator operator = operators.resolve(userId);
        PortalPageViewDO row = new PortalPageViewDO();
        row.setPageUrl(pageUrl);
        row.setPageTitle(trimToNull(trackReqVO.getPageTitle(), TITLE_MAX_LEN));
        row.setUsername(operator.username());
        row.setDisplayName(operator.displayName());
        // 快照存单值：一个人挂多个部门时取主部门，Portal 的 (user.department or [None])[0] 同款
        row.setDepartment(operator.department());
        row.setReferrer(trimToNull(trackReqVO.getReferrer(), URL_MAX_LEN));
        row.setUserAgent(trimToNull(userAgent, URL_MAX_LEN));
        row.setViewedAt(LocalDateTime.now());
        pageViewMapper.insert(row);
    }

    @Override
    public PortalAnalyticsSummaryRespVO summary(PortalAnalyticsRangeReqVO rangeReqVO) {
        int days = rangeReqVO.getDays();
        Map<String, Object> window = pageViewMapper.selectCounts(since(days));
        Map<String, Object> today = pageViewMapper.selectCounts(LocalDate.now().atStartOfDay());
        PortalAnalyticsSummaryRespVO vo = new PortalAnalyticsSummaryRespVO();
        vo.setDays(days);
        vo.setTotalPv(number(window.get("pv")));
        vo.setTotalUv(number(window.get("uv")));
        vo.setTodayPv(number(today.get("pv")));
        vo.setTodayUv(number(today.get("uv")));
        vo.setActivePages(number(window.get("pageCount")));
        return vo;
    }

    @Override
    public List<PortalPopularPageRespVO> popularPages(PortalAnalyticsTopReqVO topReqVO) {
        List<Map<String, Object>> rows = pageViewMapper.selectPopularPages(since(topReqVO.getDays()), topReqVO.getLimit());
        List<PortalPopularPageRespVO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            PortalPopularPageRespVO vo = new PortalPopularPageRespVO();
            vo.setPageUrl(text(row.get("pageUrl")));
            vo.setPageTitle(text(row.get("pageTitle")));
            vo.setPv(number(row.get("pv")));
            vo.setUv(number(row.get("uv")));
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<PortalDailyTrendRespVO> dailyTrend(PortalAnalyticsRangeReqVO rangeReqVO) {
        List<Map<String, Object>> rows = pageViewMapper.selectDailyTrend(since(rangeReqVO.getDays()));
        List<PortalDailyTrendRespVO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            PortalDailyTrendRespVO vo = new PortalDailyTrendRespVO();
            vo.setDate(text(row.get("day")));
            vo.setPv(number(row.get("pv")));
            vo.setUv(number(row.get("uv")));
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<PortalActiveUserRespVO> activeUsers(PortalAnalyticsTopReqVO topReqVO) {
        List<Map<String, Object>> rows = pageViewMapper.selectActiveUsers(since(topReqVO.getDays()), topReqVO.getLimit());
        List<PortalActiveUserRespVO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            PortalActiveUserRespVO vo = new PortalActiveUserRespVO();
            vo.setUsername(text(row.get("username")));
            vo.setDisplayName(text(row.get("displayName")));
            vo.setDepartment(text(row.get("department")));
            vo.setPageViews(number(row.get("pageViews")));
            vo.setUniquePages(number(row.get("uniquePages")));
            vo.setLastActive(text(row.get("lastActive")));
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<PortalUserHistoryRespVO> userHistory(PortalUserHistoryReqVO historyReqVO) {
        List<PortalPageViewDO> rows = pageViewMapper.selectUserHistory(
                historyReqVO.getUsername().trim(), since(historyReqVO.getDays()), historyReqVO.getLimit());
        List<PortalUserHistoryRespVO> list = new ArrayList<>(rows.size());
        for (PortalPageViewDO row : rows) {
            PortalUserHistoryRespVO vo = new PortalUserHistoryRespVO();
            vo.setPageUrl(row.getPageUrl());
            vo.setPageTitle(row.getPageTitle());
            vo.setViewedAt(row.getViewedAt());
            list.add(vo);
        }
        return list;
    }

    // ══════════════ 内部 ══════════════

    /** 窗口起点：此刻往前推 days 天（不是 days 个自然日，第一天是不完整的一天——Portal 口径） */
    private static LocalDateTime since(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    /** 截断到列宽：前端传来的超长 URL 不该把整条埋点写挂，Portal 那边同样是截断而不是报错 */
    private static String trimToLength(String value, int maxLen) {
        String text = value == null ? "" : value.trim();
        return text.length() <= maxLen ? text : text.substring(0, maxLen);
    }

    /** 空串按 null 存（Portal 的 (x or "")[:n] or None 同款：库里不留一堆空串） */
    private static String trimToNull(String value, int maxLen) {
        String text = trimToLength(value, maxLen);
        return text.isEmpty() ? null : text;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long number(Object value) {
        return value instanceof Number count ? count.longValue() : 0L;
    }

}
