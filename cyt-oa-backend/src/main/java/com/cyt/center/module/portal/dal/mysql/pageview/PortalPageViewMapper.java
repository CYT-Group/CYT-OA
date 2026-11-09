package com.cyt.center.module.portal.dal.mysql.pageview;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyt.center.framework.mybatis.core.mapper.BaseMapperX;
import com.cyt.center.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.cyt.center.module.portal.dal.dataobject.pageview.PortalPageViewDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 页面埋点仓储：写一行 + 看板用的几个聚合。
 *
 * 口径与 Portal 对齐，两处容易写错的地方留在这里说明：
 * - UV 一律是 COUNT(DISTINCT username)，匿名行（username IS NULL）天然不计入，PV 与页面数照算；
 * - 看板按行内快照列（display_name / department）聚合，不 join sys_user——
 *   改名或换部门之后，历史统计仍按当时的事实呈现。
 *
 * 别名刻意不含下划线：Map 结果的键名在不同 MyBatis 配置下有大小写口径分歧，单字别名两头都对。
 */
@Mapper
public interface PortalPageViewMapper extends BaseMapperX<PortalPageViewDO> {

    /** 窗口内的 PV / UV / 被访问过的页面数；today 口径就是把 since 换成当天零点再调一次 */
    default Map<String, Object> selectCounts(LocalDateTime since) {
        return selectMaps(new QueryWrapper<PortalPageViewDO>()
                .select("COUNT(*) AS pv", "COUNT(DISTINCT username) AS uv",
                        "COUNT(DISTINCT page_url) AS pageCount")
                .ge("viewed_at", since)).get(0);
    }

    /** 热门页面：按 URL 分组，标题取组内 max()（Portal 口径如此，不是「最新标题」） */
    default List<Map<String, Object>> selectPopularPages(LocalDateTime since, int limit) {
        return selectMaps(new QueryWrapper<PortalPageViewDO>()
                .select("page_url AS pageUrl", "MAX(page_title) AS pageTitle",
                        "COUNT(*) AS pv", "COUNT(DISTINCT username) AS uv")
                .ge("viewed_at", since)
                .groupBy("page_url")
                .orderByDesc("pv")
                .last("LIMIT " + limit));
    }

    /**
     * 按天趋势：稀疏序列，没有访问的日期不补零；首日是不完整的一天。
     * 格式化在 SQL 里做，返回的就是前端能直接画的 "YYYY-MM-DD"。
     */
    default List<Map<String, Object>> selectDailyTrend(LocalDateTime since) {
        return selectMaps(new QueryWrapper<PortalPageViewDO>()
                .select("DATE_FORMAT(viewed_at, '%Y-%m-%d') AS day",
                        "COUNT(*) AS pv", "COUNT(DISTINCT username) AS uv")
                .ge("viewed_at", since)
                .groupBy("day")
                .orderByAsc("day"));
    }

    /** 活跃用户排行：只数有登录的访问，姓名与部门取快照列的 max() */
    default List<Map<String, Object>> selectActiveUsers(LocalDateTime since, int limit) {
        return selectMaps(new QueryWrapper<PortalPageViewDO>()
                .select("username AS username", "MAX(display_name) AS displayName",
                        "MAX(department) AS department", "COUNT(*) AS pageViews",
                        "COUNT(DISTINCT page_url) AS uniquePages",
                        // 格式化在 SQL 里做：Map 结果里的 DATETIME 会被 JDBC 装成 java.sql.Timestamp，
                        // 到 service 层只能按 Object 强转，不如直接要一个前端能显示的串
                        "DATE_FORMAT(MAX(viewed_at), '%Y-%m-%d %H:%i:%s') AS lastActive")
                .ge("viewed_at", since)
                .isNotNull("username")
                .groupBy("username")
                .orderByDesc("pageViews")
                .last("LIMIT " + limit));
    }

    /**
     * 某个人的浏览轨迹。不校验 username 是否存在：查不到就返回空列表，
     * 否则这个接口会变成「账号存在性探针」。
     */
    default List<PortalPageViewDO> selectUserHistory(String username, LocalDateTime since, int limit) {
        return selectList(new LambdaQueryWrapperX<PortalPageViewDO>()
                .eq(PortalPageViewDO::getUsername, username)
                .ge(PortalPageViewDO::getViewedAt, since)
                .orderByDesc(PortalPageViewDO::getViewedAt)
                .last("LIMIT " + limit));
    }

}
