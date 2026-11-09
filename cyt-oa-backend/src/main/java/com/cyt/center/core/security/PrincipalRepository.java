package com.cyt.center.core.security;

/**
 * 身份装载入口。core 不直接依赖任何业务模块的 Mapper，由 system 模块提供实现。
 */
public interface PrincipalRepository {

    /** 账号不存在或被停用时返回 null。 */
    Principal load(Long userId);

}
