package com.cyt.center.framework.mybatis.core.util;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 日期统计、逗号分隔列表关联、JSON 数组匹配、行数限制、分批删除 SQL 片段的方言工具类
 *
 * 芋道原版会按运行时数据源识别 PostgreSQL、Oracle、SQL Server 等方言；本项目固定 MySQL，
 * 因此只保留 MySQL 片段（即原版的“原有 SQL 片段”分支），方法签名保持不变，
 * 移植的 Mapper XML / 注解可以继续用
 * ${@com.cyt.center.framework.mybatis.core.util.SqlDialectUtils@formatDate('create_time')} 的方式调用。
 *
 * 参数只能是代码中固定的列名，经过白名单校验；格式均为常量，业务值必须继续使用 #{} 绑定。
 */
public class SqlDialectUtils {

    /**
     * MyBatis 参数名：参数或其属性路径，例如 limit、reqVO.limit
     */
    private static final Pattern PARAM_NAME_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    // ========== 日期字符串 ==========

    /**
     * 按天格式化为 yyyy-MM-dd 字符串，即 DATE_FORMAT(column, '%Y-%m-%d')
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String formatDate(String column) {
        checkColumn(column);
        return "DATE_FORMAT(" + column + ", '%Y-%m-%d')";
    }

    /**
     * 按月格式化为 yyyyMM 字符串，即 DATE_FORMAT(column, '%Y%m')
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String formatYearMonth(String column) {
        checkColumn(column);
        return "DATE_FORMAT(" + column + ", '%Y%m')";
    }

    /**
     * 按月格式化为 yyyy-MM 字符串
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String formatYearDashMonth(String column) {
        checkColumn(column);
        return "CONCAT(EXTRACT(YEAR FROM " + column + "), '-', LPAD(CONCAT(EXTRACT(MONTH FROM " + column + ")), 2, '0'))";
    }

    /**
     * 按天分组，即 DATE(column)；适用于同时兼容日期值与 yyyy-MM-dd 字符串的消费者
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String date(String column) {
        checkColumn(column);
        return "DATE(" + column + ")";
    }

    /**
     * 按天分组，即 CAST(column AS DATE)；适用于 String 类型的消费者
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String castDateText(String column) {
        checkColumn(column);
        return "CAST(" + column + " AS DATE)";
    }

    /**
     * 按天截断为日期值，即 CAST(column AS DATE)；适用于 LocalDate 类型的消费者
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String castDate(String column) {
        checkColumn(column);
        return "CAST(" + column + " AS DATE)";
    }

    // ========== 日期部件 ==========

    /**
     * 月份（1-12），即 MONTH(column)
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String month(String column) {
        checkColumn(column);
        return "MONTH(" + column + ")";
    }

    /**
     * 年份，即 YEAR(column)
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String year(String column) {
        checkColumn(column);
        return "YEAR(" + column + ")";
    }

    /**
     * 日（1-31），即 DAYOFMONTH(column)
     *
     * @param column 列名
     * @return SQL 片段
     */
    public static String dayOfMonth(String column) {
        checkColumn(column);
        return "DAYOFMONTH(" + column + ")";
    }

    // ========== 聚合 ==========

    /**
     * 平均成交周期（天）：两个时间之间的完整天数（向零截断）求平均，保留 1 位小数（截断），无数据时为 0
     *
     * 仅适用于不带时区的日期时间列（LocalDateTime 契约）
     *
     * @param beginColumn 开始时间列名
     * @param endColumn   结束时间列名
     * @return SQL 片段
     */
    public static String customerDealCycle(String beginColumn, String endColumn) {
        checkColumn(beginColumn);
        checkColumn(endColumn);
        return "COALESCE(TRUNCATE(AVG(TIMESTAMPDIFF(DAY, " + beginColumn + ", " + endColumn + ")), 1), 0)";
    }

    // ========== 其它 ==========

    /**
     * MySQL 无需 DUAL，返回空串，保持与芋道原版相同的调用方式
     *
     * @return SQL 片段
     */
    public static String fromDual() {
        return "";
    }

    // ========== 逗号分隔列表 ==========

    /**
     * 两列之间的 FIND_IN_SET 关联条件：数值 ID 列的值是否为逗号分隔列表列中的一个元素
     *
     * 1. 列表按英文逗号分隔、元素两侧不含空格，与 FIND_IN_SET 一致；元素重复时，同一行仍只匹配一次
     * 2. 任一列为 NULL 时不匹配
     *
     * 列与参数值比较时，使用 {@link MyBatisUtils#findInSet(String, java.util.Collection)}
     *
     * @param valueColumn 数值类型的 ID 列名，例如 cc.id、contact_id
     * @param listColumn  逗号分隔的列表列名，例如 cfur.contact_ids
     * @return SQL 条件片段
     */
    public static String findInSetColumn(String valueColumn, String listColumn) {
        checkIdColumn(valueColumn);
        checkColumn(listColumn);
        return "FIND_IN_SET(" + valueColumn + ", " + listColumn + ")";
    }

    // ========== JSON 数组 ==========

    /**
     * JSON 数组列是否包含 {0} 绑定的数值 ID，供 Wrapper 的 apply 使用，ID 必须作为 apply 的参数绑定
     *
     * @param column    JSON 数组列名，元素为数值 ID
     * @param wrapArray 是否使用 JSON_ARRAY 包装参数
     * @return 包含 {0} 占位符的 SQL 条件片段
     */
    public static String jsonArrayContains(String column, boolean wrapArray) {
        checkColumn(column);
        return wrapArray ? "JSON_CONTAINS(" + column + ", JSON_ARRAY({0}))" : "JSON_CONTAINS(" + column + ", {0})";
    }

    // ========== 行数限制 ==========

    /**
     * 只返回前 rows 行，追加在语句末尾（ORDER BY 之后），即 LIMIT rows
     *
     * 仅用于 XML 或注解中手写的 SQL；使用 Wrapper 查询时，使用 BaseMapperX 的 selectListLimitN(queryWrapper, limit)
     *
     * @param rows    行数，不能为负数
     * @param ordered 语句是否已有 ORDER BY，MySQL 该参数不影响结果
     * @return SQL 片段
     */
    public static String limitRows(int rows, boolean ordered) {
        if (rows < 0) {
            throw new IllegalArgumentException("行数不能为负数：" + rows);
        }
        return "LIMIT " + rows;
    }

    /**
     * 只返回前 #{param} 行，行数使用 #{} 绑定；规则同 {@link #limitRows(int, boolean)}
     *
     * @param param   行数的参数名，例如 limit
     * @param ordered 语句是否已有 ORDER BY
     * @return SQL 片段
     */
    public static String limitParam(String param, boolean ordered) {
        checkParam(param);
        return "LIMIT #{" + param + "}";
    }

    // ========== 分批删除 ==========

    /**
     * 分批删除 column &lt; #{valueParam} 的记录，每次最多删除 #{limitParam} 行；
     * 语句为 DELETE FROM table WHERE column &lt; #{valueParam} LIMIT #{limitParam}
     *
     * 要求表的主键列为 id；表名、列名只能是代码中固定的常量，值均使用 #{} 绑定
     *
     * @param table      表名
     * @param column     比较的列名
     * @param valueParam 比较值的参数名
     * @param limitParam 每批行数的参数名
     * @return SQL 语句
     */
    public static String deleteBatchByLessThan(String table, String column, String valueParam, String limitParam) {
        checkColumn(table);
        checkColumn(column);
        checkParam(valueParam);
        checkParam(limitParam);
        return "DELETE FROM " + table + " WHERE " + column + " < #{" + valueParam + "} LIMIT #{" + limitParam + "}";
    }

    // ========== 校验 ==========

    /**
     * 校验代码中固定的表名或列名，拒绝表达式与语句片段。
     *
     * @param column 待校验的表名或列名
     */
    private static void checkColumn(String column) {
        if (!MyBatisUtils.isSafeColumnName(column)) {
            throw new IllegalArgumentException("Invalid column name: " + column);
        }
    }

    /**
     * 校验代码中固定的 ID 列名：在列名校验的基础上，最后一段需要为 id 或以 _id 结尾，避免用于任意业务字符串列。
     *
     * @param column 待校验的列名，可带表别名前缀
     */
    private static void checkIdColumn(String column) {
        checkColumn(column);
        String name = column.substring(column.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!name.equals("id") && !name.endsWith("_id")) {
            throw new IllegalArgumentException("Invalid id column name: " + column);
        }
    }

    /**
     * 校验 MyBatis 参数名或属性路径，拒绝占位符逃逸与 SQL 片段。
     *
     * @param param 待校验的参数名
     */
    private static void checkParam(String param) {
        if (param == null || !PARAM_NAME_PATTERN.matcher(param).matches()) {
            throw new IllegalArgumentException("Invalid param name: " + param);
        }
    }

    /**
     * 兼容芋道原版按数据源 URL 判断的写法，本项目固定 MySQL，仅保留入口避免移植时逐个改调用点
     *
     * @param url JDBC URL
     * @return 是否为受支持的数据库
     */
    public static boolean isSupportedUrl(String url) {
        return StrUtil.contains(url, ":mysql:") || StrUtil.contains(url, ":mariadb:");
    }

}
