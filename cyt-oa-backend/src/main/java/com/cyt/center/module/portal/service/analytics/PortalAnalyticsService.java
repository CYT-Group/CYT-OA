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

import java.util.List;

/**
 * 页面浏览埋点与看板用例（口径 Portal 的 application/analytics.py）。
 *
 * 本接口是 Portal 模块里唯一「服务层直接回 VO」的地方：聚合行没有对应的表、也就没有 DO，
 * 再造一层 record 只是给同一份字段起两个名字。查询方法的权限交给端点注解
 * （Portal 的 require(can_view_dashboards) 就是整端点闸，没有按人分叉的规则）。
 */
public interface PortalAnalyticsService {

    /** 记一次页面浏览；姓名 / 部门按当前登录人在写入时快照，userAgent 取自请求头 */
    void track(Long userId, PortalTrackReqVO trackReqVO, String userAgent);

    /** 窗口总览 + 今日总览 */
    PortalAnalyticsSummaryRespVO summary(PortalAnalyticsRangeReqVO rangeReqVO);

    /** 热门页面 Top N，按 PV 倒序 */
    List<PortalPopularPageRespVO> popularPages(PortalAnalyticsTopReqVO topReqVO);

    /** 每日 PV/UV 趋势，按日期升序的稀疏序列 */
    List<PortalDailyTrendRespVO> dailyTrend(PortalAnalyticsRangeReqVO rangeReqVO);

    /** 活跃用户 Top N（匿名访问不计） */
    List<PortalActiveUserRespVO> activeUsers(PortalAnalyticsTopReqVO topReqVO);

    /** 某个人的浏览轨迹 */
    List<PortalUserHistoryRespVO> userHistory(PortalUserHistoryReqVO historyReqVO);

}
