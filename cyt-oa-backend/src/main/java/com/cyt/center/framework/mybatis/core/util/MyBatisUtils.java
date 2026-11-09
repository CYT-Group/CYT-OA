package com.cyt.center.framework.mybatis.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.func.Func1;
import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.pojo.SortingField;
import com.cyt.center.framework.mybatis.core.enums.DbTypeEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MyBatis 工具类
 *
 * 说明：芋道原版还有 getTableName(Table)、buildColumn(...) 两个基于 JSqlParser 的方法，
 * 只被多租户、数据权限插件使用，本项目不引入这些能力，故不保留。
 */
public class MyBatisUtils {

    private static final Pattern SAFE_COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*$");

    private static final String FIND_IN_SET_VALUE_PLACEHOLDER = "#{value}";

    private static final String FIND_IN_SET_COLUMN_PLACEHOLDER = "#{column}";

    /**
     * 本项目固定使用 MySQL，无需按运行时数据源识别方言
     */
    static final DbType DB_TYPE = DbType.MYSQL;

    public static <T> Page<T> buildPage(PageParam pageParam) {
        return buildPage(pageParam, null);
    }

    public static <T> Page<T> buildPage(PageParam pageParam, Collection<SortingField> sortingFields) {
        // 页码 + 数量
        Page<T> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        page.setOptimizeJoinOfCountSql(false); // 关联 issue：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID2QLL
        // 排序字段
        if (CollUtil.isNotEmpty(sortingFields)) {
            for (SortingField sortingField : sortingFields) {
                String columnName = buildSafeOrderColumn(sortingField.getField());
                if (columnName == null) {
                    continue;
                }
                page.addOrder(new OrderItem().setAsc(isAscOrder(sortingField.getOrder())).setColumn(columnName));
            }
        }
        return page;
    }

    /**
     * 构建指定偏移量的分页对象
     *
     * @param offset      偏移量，负数按 0 处理，不要求是每页条数的整数倍
     * @param size        每页条数
     * @param searchCount 是否查询总数
     * @param <T>         记录类型
     * @return 分页对象
     */
    public static <T> Page<T> buildPage(long offset, long size, boolean searchCount) {
        return new Page<T>(1, size, searchCount) {

            @Override
            public long offset() {
                return Math.max(offset, 0);
            }

        };
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static <T> void addOrder(Wrapper<T> wrapper, Collection<SortingField> sortingFields) {
        if (CollUtil.isEmpty(sortingFields)) {
            return;
        }
        if (wrapper instanceof QueryWrapper<T>) {
            QueryWrapper<T> query = (QueryWrapper<T>) wrapper;
            for (SortingField sortingField : sortingFields) {
                String columnName = buildSafeOrderColumn(sortingField.getField());
                if (columnName == null) {
                    continue;
                }
                query.orderBy(true, isAscOrder(sortingField.getOrder()), columnName);
            }
        } else if (wrapper instanceof LambdaQueryWrapper<T>) {
            // LambdaQueryWrapper 不直接支持字符串字段排序，使用 last 方法拼接 ORDER BY
            LambdaQueryWrapper<T> lambdaQuery = (LambdaQueryWrapper<T>) wrapper;
            StringBuilder orderBy = new StringBuilder();
            for (SortingField sortingField : sortingFields) {
                String columnName = buildSafeOrderColumn(sortingField.getField());
                if (columnName == null) {
                    continue;
                }
                if (StrUtil.isNotEmpty(orderBy)) {
                    orderBy.append(", ");
                }
                orderBy.append(columnName).append(" ").append(getOrderDirection(sortingField.getOrder()));
            }
            if (StrUtil.isNotEmpty(orderBy)) {
                lambdaQuery.last("ORDER BY " + orderBy);
            }
        } else {
            throw new IllegalArgumentException("Unsupported wrapper type: " + wrapper.getClass().getName());
        }
    }

    public static boolean isAscOrder(String order) {
        return SortingField.ORDER_ASC.equals(order);
    }

    public static String getOrderDirection(String order) {
        return isAscOrder(order) ? "ASC" : "DESC";
    }

    private static String buildSafeOrderColumn(String field) {
        String columnName = StrUtil.toUnderlineCase(field);
        if (StrUtil.isEmpty(columnName) || !SAFE_COLUMN_NAME_PATTERN.matcher(columnName).matches()) {
            return null;
        }
        return columnName;
    }

    /**
     * 将拦截器添加到链中
     * 由于 MybatisPlusInterceptor 不支持添加拦截器，所以只能全量设置
     *
     * @param interceptor 链
     * @param inner       拦截器
     * @param index       位置
     */
    public static void addInterceptor(MybatisPlusInterceptor interceptor, InnerInterceptor inner, int index) {
        List<InnerInterceptor> inners = new ArrayList<>(interceptor.getInterceptors());
        inners.add(index, inner);
        interceptor.setInterceptors(inners);
    }

    /**
     * 跨数据库的 find_in_set 实现
     *
     * @param columnName 字段名称
     * @return sql
     */
    public static String findInSet(String columnName) {
        return findInSet(columnName, 0);
    }

    /**
     * 跨数据库的 find_in_set 实现，适用于同一个 apply 语句中有多个参数的场景
     *
     * @param columnName 字段名称
     * @param paramIndex apply 参数序号
     * @return sql
     */
    public static String findInSetWithParamIndex(String columnName, int paramIndex) {
        return findInSet(columnName, paramIndex);
    }

    /**
     * 跨数据库的 find_in_set 实现，适用于同一字段匹配多个参数的场景
     *
     * 每个参数生成一个 find_in_set 条件，并使用 OR 连接。
     *
     * @param columnName 字段名称
     * @param values     参数集合
     * @return sql
     */
    public static String findInSet(String columnName, Collection<?> values) {
        return findInSet(DB_TYPE, columnName, values);
    }

    static String findInSet(DbType dbType, String columnName, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            throw new IllegalArgumentException("Values cannot be empty");
        }
        return IntStream.range(0, values.size())
                .mapToObj(index -> findInSet(dbType, columnName, index))
                .collect(Collectors.joining(" OR "));
    }

    private static String findInSet(String columnName, int paramIndex) {
        return findInSet(DB_TYPE, columnName, paramIndex);
    }

    static String findInSet(DbType dbType, String columnName, int paramIndex) {
        if (!isSafeColumnName(columnName)) {
            throw new IllegalArgumentException("Invalid column name: " + columnName);
        }
        if (paramIndex < 0) {
            throw new IllegalArgumentException("Invalid param index: " + paramIndex);
        }
        return DbTypeEnum.getFindInSetTemplate(dbType)
                .replace(FIND_IN_SET_COLUMN_PLACEHOLDER, columnName)
                .replace(FIND_IN_SET_VALUE_PLACEHOLDER, "{" + paramIndex + "}");
    }

    /**
     * 判断表名或列名是否只包含字母、数字、下划线及层级分隔点
     *
     * @param columnName 表名或列名，可带表别名前缀
     * @return 是否满足安全标识符格式
     */
    static boolean isSafeColumnName(String columnName) {
        return StrUtil.isNotEmpty(columnName) && SAFE_COLUMN_NAME_PATTERN.matcher(columnName).matches();
    }

    /**
     * 将驼峰命名转换为下划线命名
     *
     * 使用场景：
     * 1. <a href="https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1357/files">fix:修复"商品统计聚合函数的别名与排序字段不符"导致的 SQL 异常</a>
     *
     * @param func 字段名函数(驼峰命名)
     * @return 字段名(下划线命名)
     */
    public static <T> String toUnderlineCase(Func1<T, ?> func) {
        String fieldName = LambdaUtil.getFieldName(func);
        return StrUtil.toUnderlineCase(fieldName);
    }

}
