package com.cyt.center.framework.mybatis.core.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.cyt.center.core.result.PageResult;
import com.cyt.center.framework.common.pojo.PageParam;
import com.cyt.center.framework.common.pojo.SortablePageParam;
import com.cyt.center.framework.common.pojo.SortingField;
import com.cyt.center.framework.common.util.object.PageUtils;
import com.cyt.center.framework.mybatis.core.util.CaseInsensitiveResultMap;
import com.cyt.center.framework.mybatis.core.util.MyBatisUtils;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.interfaces.MPJBaseJoin;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.exceptions.TooManyResultsException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 在 MyBatis Plus 的 BaseMapper 的基础上拓展，提供更多的能力
 *
 * 1. {@link BaseMapper} 为 MyBatis Plus 的基础接口，提供基础的 CRUD 能力
 * 2. {@link MPJBaseMapper} 为 MyBatis Plus Join 的基础接口，提供连表 Join 能力
 *
 * 与芋道原版的差异（本项目使用 PostgreSQL，因此做了刻意简化）：
 * 1. 分页返回 {@link PageResult} 来自 com.cyt.center.core.result，与个人中心已有接口保持一致；
 * 2. 锁定读不再依赖自定义 SqlInjector（SelectForUpdateMethod）、ForUpdateInnerInterceptor、
 *    JdbcUtils 的方言识别，直接用 PostgreSQL 的 FOR UPDATE / LIMIT 1 FOR UPDATE 拼在 SQL 末尾；
 * 3. insertBatch 统一走 Db.saveBatch。
 */
public interface BaseMapperX<T> extends MPJBaseMapper<T> {

    /**
     * Wrapper 的 set(column, null, mapping) 使用：显式声明 VARCHAR 类型
     *
     * 显式约定字符串列的空值类型，避免依赖驱动推断。
     */
    String JDBC_TYPE_VARCHAR = "jdbcType=VARCHAR";

    /**
     * PostgreSQL 锁定读：追加在 SQL 末尾的 FOR UPDATE
     */
    String FOR_UPDATE = "FOR UPDATE";

    /**
     * PostgreSQL 锁定读，且只取第一条：追加在 SQL 末尾
     */
    String LIMIT_1_FOR_UPDATE = "LIMIT 1 " + FOR_UPDATE;

    default PageResult<T> selectPage(SortablePageParam pageParam, @Param("ew") Wrapper<T> queryWrapper) {
        return selectPage(pageParam, pageParam.getSortingFields(), queryWrapper);
    }

    default PageResult<T> selectPage(PageParam pageParam, @Param("ew") Wrapper<T> queryWrapper) {
        return selectPage(pageParam, null, queryWrapper);
    }

    default PageResult<T> selectPage(PageParam pageParam, Collection<SortingField> sortingFields, @Param("ew") Wrapper<T> queryWrapper) {
        // 特殊：不分页，直接查询全部
        if (PageParam.PAGE_SIZE_NONE.equals(pageParam.getPageSize())) {
            MyBatisUtils.addOrder(queryWrapper, sortingFields);
            List<T> list = selectList(queryWrapper);
            return PageUtils.buildPageResult(pageParam, list);
        }

        // MyBatis Plus 查询
        IPage<T> mpPage = MyBatisUtils.buildPage(pageParam, sortingFields);
        selectPage(mpPage, queryWrapper);
        // 转换返回
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    default <D> PageResult<D> selectJoinPage(PageParam pageParam, Class<D> clazz, MPJLambdaWrapper<T> lambdaWrapper) {
        // 特殊：不分页，直接查询全部
        if (PageParam.PAGE_SIZE_NONE.equals(pageParam.getPageSize())) {
            List<D> list = selectJoinList(clazz, lambdaWrapper);
            return PageUtils.buildPageResult(pageParam, list);
        }

        // MyBatis Plus Join 查询
        IPage<D> mpPage = MyBatisUtils.buildPage(pageParam);
        mpPage = selectJoinPage(mpPage, clazz, lambdaWrapper);
        // 转换返回
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    /**
     * 执行分页查询并返回结果。
     *
     * @param pageParam 分页参数，包含页码、每页条数和排序字段信息。如果 pageSize 为 {@link PageParam#PAGE_SIZE_NONE}，则不分页，直接查询所有数据。
     * @param clazz     结果集的类类型
     * @param lambdaWrapper MyBatis Plus Join 查询条件包装器
     * @param <D>       结果集的泛型类型
     * @return 返回分页查询的结果，包括总记录数和当前页的数据列表
     */
    default <D> PageResult<D> selectJoinPage(SortablePageParam pageParam, Class<D> clazz, MPJLambdaWrapper<T> lambdaWrapper) {
        // 特殊：不分页，直接查询全部
        if (PageParam.PAGE_SIZE_NONE.equals(pageParam.getPageSize())) {
            List<D> list = selectJoinList(clazz, lambdaWrapper);
            return PageUtils.buildPageResult(pageParam, list);
        }

        // MyBatis Plus Join 查询
        IPage<D> mpPage = MyBatisUtils.buildPage(pageParam, pageParam.getSortingFields());
        mpPage = selectJoinPage(mpPage, clazz, lambdaWrapper);
        // 转换返回
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    default <DTO> PageResult<DTO> selectJoinPage(PageParam pageParam, Class<DTO> resultTypeClass, MPJBaseJoin<T> joinQueryWrapper) {
        IPage<DTO> mpPage = MyBatisUtils.buildPage(pageParam);
        selectJoinPage(mpPage, resultTypeClass, joinQueryWrapper);
        // 转换返回
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }

    @Override
    @SuppressWarnings("unchecked")
    default Map<String, Object> selectJoinMap(@Param(Constants.WRAPPER) MPJBaseJoin<T> wrapper) {
        return selectJoinOne(CaseInsensitiveResultMap.class, wrapper);
    }

    @Override
    @SuppressWarnings("unchecked")
    default List<Map<String, Object>> selectJoinMaps(@Param(Constants.WRAPPER) MPJBaseJoin<T> wrapper) {
        return (List<Map<String, Object>>) ((Object) selectJoinList(CaseInsensitiveResultMap.class, wrapper));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default <P extends IPage<Map<String, Object>>> P selectJoinMapsPage(P page, @Param(Constants.WRAPPER) MPJBaseJoin<T> wrapper) {
        return (P) selectJoinPage((IPage) page, CaseInsensitiveResultMap.class, wrapper);
    }

    default T selectOne(String field, Object value) {
        return selectOne(new QueryWrapper<T>().eq(field, value));
    }

    default T selectOne(SFunction<T, ?> field, Object value) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default T selectOne(String field1, Object value1, String field2, Object value2) {
        return selectOne(new QueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default T selectOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default T selectOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2,
                        SFunction<T, ?> field3, Object value3) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2).eq(field3, value3));
    }

    // ========== 锁定读：需要在事务中调用；Wrapper 不能使用 last、groupBy、having、参数别名 ==========

    /**
     * 获得满足条件的一条记录，并使用 FOR UPDATE 锁定。
     *
     * 注意：需要在事务中调用，否则锁会立即释放；多于 1 条时抛出 {@link TooManyResultsException}（已锁定的记录在事务结束时释放）
     *
     * @param queryWrapper 查询条件
     * @return 实体
     */
    default T selectOneForUpdate(LambdaQueryWrapper<T> queryWrapper) {
        List<T> list = selectListForUpdate(queryWrapper);
        if (list.size() > 1) {
            throw new TooManyResultsException("Expected one result (or null) to be returned by selectOneForUpdate(), but found: " + list.size());
        }
        return CollUtil.getFirst(list);
    }

    default T selectOneForUpdate(SFunction<T, ?> field, Object value) {
        return selectOneForUpdate(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default T selectOneForUpdate(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        return selectOneForUpdate(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    default T selectOneForUpdate(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2,
                                 SFunction<T, ?> field3, Object value3) {
        return selectOneForUpdate(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2).eq(field3, value3));
    }

    /**
     * 获得满足条件的全部记录，并使用 FOR UPDATE 锁定。
     *
     * 注意：需要在事务中调用，否则锁会立即释放；返回顺序为 queryWrapper 的排序，未设置时不排序
     *
     * @param queryWrapper 查询条件
     * @return 实体列表
     */
    default List<T> selectListForUpdate(LambdaQueryWrapper<T> queryWrapper) {
        return selectList(forUpdate(queryWrapper, FOR_UPDATE));
    }

    default List<T> selectListForUpdate(SFunction<T, ?> field, Object value) {
        return selectListForUpdate(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectListForUpdate(SFunction<T, ?> field, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return CollUtil.newArrayList();
        }
        return selectListForUpdate(new LambdaQueryWrapper<T>().in(field, values));
    }

    /**
     * 获得满足条件的第 1 条记录，并使用 FOR UPDATE 锁定。
     *
     * 注意：需要在事务中调用，否则锁会立即释放；queryWrapper 未设置排序时，为任意一条满足条件的记录（只适合判断是否存在）
     *
     * PostgreSQL 直接使用 LIMIT 1 FOR UPDATE 一条 SQL 完成。
     *
     * @param queryWrapper 查询条件
     * @return 实体
     */
    default T selectFirstOneForUpdate(LambdaQueryWrapper<T> queryWrapper) {
        return CollUtil.getFirst(selectList(forUpdate(queryWrapper, LIMIT_1_FOR_UPDATE)));
    }

    /**
     * 追加锁定读的子句。
     *
     * 说明：PostgreSQL 的 FOR UPDATE 必须出现在语句末尾，这里用 Wrapper.last 拼接；
     * 调用方的 Wrapper 一般只在方法内构造，last 被本方法占用，若调用方已自行设置 last 则会报错，
     * 与原版的“锁定读的查询条件不能使用 last”约束一致。
     *
     * @param queryWrapper 查询条件
     * @param clause       追加的子句
     * @return 查询条件
     */
    default LambdaQueryWrapper<T> forUpdate(LambdaQueryWrapper<T> queryWrapper, String clause) {
        if (queryWrapper == null) {
            throw new IllegalArgumentException("锁定读的查询条件不能为空");
        }
        return queryWrapper.last(clause);
    }

    /**
     * 获取满足条件的第 1 条记录
     *
     * 目的：解决并发场景下，插入多条记录后，使用 selectOne 会报错的问题
     *
     * @param field 字段名
     * @param value 字段值
     * @return 实体
     */
    default T selectFirstOne(SFunction<T, ?> field, Object value) {
        List<T> list = selectList(new LambdaQueryWrapper<T>().eq(field, value));
        return CollUtil.getFirst(list);
    }

    default T selectFirstOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        List<T> list = selectList(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
        return CollUtil.getFirst(list);
    }

    default T selectFirstOne(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2,
                             SFunction<T, ?> field3, Object value3) {
        List<T> list = selectList(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2).eq(field3, value3));
        return CollUtil.getFirst(list);
    }

    /**
     * 获取满足条件的最新一条记录
     * <p>
     * 目的：解决并发场景下，插入多条记录后，使用 selectOne 会报错的问题
     *
     * @param queryWrapper 查询条件
     * @return 最新一条；不存在返回 null
     */
    default T selectLastOne(LambdaQueryWrapper<T> queryWrapper) {
        return CollUtil.getLast(selectList(queryWrapper));
    }

    /**
     * 获取满足条件的第 1 条记录，见 {@link #selectListLimitN(Wrapper, int)}
     *
     * @param queryWrapper 查询条件
     * @return 第 1 条；不存在返回 null
     */
    default T selectFirstOne(Wrapper<T> queryWrapper) {
        return CollUtil.getFirst(selectListLimitN(queryWrapper, 1));
    }

    /**
     * 获取满足条件的前 limit 条记录
     *
     * 使用 MyBatis Plus 分页插件生成 LIMIT 语句，并且不执行 COUNT 查询；
     * 保留 queryWrapper 的排序与 select 投影，不能再通过 last 追加行数限制
     *
     * @param queryWrapper 查询条件
     * @param limit        条数，不能为负数；为 0 时不查询，直接返回空列表
     * @return 记录列表
     */
    default List<T> selectListLimitN(Wrapper<T> queryWrapper, int limit) {
        if (limit < 0) {
            // 分页插件会把负数的 size 视为不分页，从而查询全表
            throw new IllegalArgumentException("limit 不能为负数：" + limit);
        }
        if (limit == 0) {
            return CollUtil.newArrayList();
        }
        return selectPage(MyBatisUtils.<T>buildPage(0, limit, false), queryWrapper).getRecords();
    }

    default Long selectCount() {
        return selectCount(new QueryWrapper<>());
    }

    default Long selectCount(String field, Object value) {
        return selectCount(new QueryWrapper<T>().eq(field, value));
    }

    default Long selectCount(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList() {
        return selectList(new QueryWrapper<>());
    }

    default List<T> selectList(String field, Object value) {
        return selectList(new QueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default List<T> selectList(String field, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return CollUtil.newArrayList();
        }
        return selectList(new QueryWrapper<T>().in(field, values));
    }

    default List<T> selectList(SFunction<T, ?> field, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return CollUtil.newArrayList();
        }
        return selectList(new LambdaQueryWrapper<T>().in(field, values));
    }

    default List<T> selectList(SFunction<T, ?> field1, Object value1, SFunction<T, ?> field2, Object value2) {
        return selectList(new LambdaQueryWrapper<T>().eq(field1, value1).eq(field2, value2));
    }

    /**
     * 批量插入，适合大量数据插入
     *
     * @param entities 实体们
     */
    default Boolean insertBatch(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        return Db.saveBatch(entities);
    }

    /**
     * 批量插入，适合大量数据插入
     *
     * @param entities 实体们
     * @param size     插入数量 Db.saveBatch 默认为 1000
     */
    default Boolean insertBatch(Collection<T> entities, int size) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        return Db.saveBatch(entities, size);
    }

    default int updateBatch(T update) {
        return update(update, new QueryWrapper<>());
    }

    default Boolean updateBatch(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        return Db.updateBatchById(entities);
    }

    default Boolean updateBatch(Collection<T> entities, int size) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        return Db.updateBatchById(entities, size);
    }

    default int delete(String field, String value) {
        return delete(new QueryWrapper<T>().eq(field, value));
    }

    default int delete(SFunction<T, ?> field, Object value) {
        return delete(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default int deleteBatch(SFunction<T, ?> field, Collection<?> values) {
        if (CollUtil.isEmpty(values)) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<T>().in(field, values));
    }

}
