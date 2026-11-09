package com.cyt.center.framework.mybatis.core.handler;

import com.cyt.center.framework.mybatis.core.dataobject.BaseDO;
import com.cyt.center.framework.security.core.util.SecurityFrameworkUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * 通用参数填充实现类
 *
 * 如果没有显式的对通用参数进行赋值，这里会对通用参数进行填充、赋值
 *
 * 覆盖两类实体：芋道移植表继承 {@link BaseDO}（creator / createTime / updater / updateTime），
 * Warm-Flow 的流程实体（createBy / createTime / updateBy / updateTime，时间是 java.util.Date）。
 * 本项目自有的 sys_* 表用 created_at / updated_at，两类实体都没有 createTime 的 setter，
 * 填充前会先判断 hasGetter / hasSetter，所以不会被误写。
 *
 * @author hexiaowu
 */
public class DefaultDBFieldHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        fillTimeIfAbsent(metaObject, "createTime");
        fillTimeIfAbsent(metaObject, "updateTime");
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        // 芋道移植表用 creator / updater 记操作人，Warm-Flow 的流程表用 createBy / updateBy，两边都填
        if (metaObject.getOriginalObject() instanceof BaseDO) {
            fillIfAbsent(metaObject, "creator", userId);
            fillIfAbsent(metaObject, "updater", userId);
        } else {
            fillIfAbsent(metaObject, "createBy", userId);
            fillIfAbsent(metaObject, "updateBy", userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        fillTimeIfAbsent(metaObject, "updateTime");
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        if (metaObject.getOriginalObject() instanceof BaseDO) {
            fillIfAbsent(metaObject, "updater", userId);
        } else {
            fillIfAbsent(metaObject, "updateBy", userId);
        }
    }

    /**
     * 按 setter 声明的类型填充时间字段：移植表用 LocalDateTime，Warm-Flow 的实体用 java.util.Date，
     * 填错类型反射赋值会直接抛异常
     *
     * @param metaObject 元对象
     * @param fieldName  字段名
     */
    private void fillTimeIfAbsent(MetaObject metaObject, String fieldName) {
        if (!metaObject.hasSetter(fieldName) || !metaObject.hasGetter(fieldName)) {
            return;
        }
        Object value = Date.class.equals(metaObject.getSetterType(fieldName)) ? new Date() : LocalDateTime.now();
        fillIfAbsent(metaObject, fieldName, value);
    }

    /**
     * 字段存在、可读且当前值为空时，才写入值
     *
     * @param metaObject 元对象
     * @param fieldName  字段名
     * @param value      填充值
     */
    private void fillIfAbsent(MetaObject metaObject, String fieldName, Object value) {
        if (!metaObject.hasSetter(fieldName) || !metaObject.hasGetter(fieldName)) {
            return;
        }
        if (Objects.nonNull(getFieldValByName(fieldName, metaObject))) {
            return;
        }
        setFieldValByName(fieldName, value, metaObject);
    }

    private String currentUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? null : userId.toString();
    }

}
