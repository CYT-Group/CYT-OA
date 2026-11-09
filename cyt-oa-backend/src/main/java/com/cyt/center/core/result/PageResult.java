package com.cyt.center.core.result;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果，避免把 MyBatis-Plus 的 Page 对象直接暴露给前端。
 *
 * page/size 允许为空：从 ruoyi 移植过来的代码只关心 list/total（上游 PageResult 只有这两个字段），
 * 个人中心自己的接口则会带上真实页码。
 */
public record PageResult<T>(long total, Long page, Long size, List<T> list) {

    public PageResult(List<T> list, Long total) {
        this(total == null ? 0L : total, null, null, list);
    }

    public PageResult(Long total) {
        this(total == null ? 0L : total, null, null, new ArrayList<>());
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L);
    }

    public static <T> PageResult<T> empty(Long total) {
        return new PageResult<>(total);
    }

    public List<T> getList() {
        return list;
    }

    public Long getTotal() {
        return total;
    }

}
