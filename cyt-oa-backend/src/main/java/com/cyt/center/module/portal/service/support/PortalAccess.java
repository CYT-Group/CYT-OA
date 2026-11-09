package com.cyt.center.module.portal.service.support;

import com.cyt.center.module.portal.domain.PortalCapability;
import com.cyt.center.module.portal.domain.PortalCategoryScope;
import com.cyt.center.module.system.api.permission.PermissionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_MANAGE_FORBIDDEN;

/**
 * Portal 管理面的能力判定：按人 + 能力位问一句「有没有」。
 *
 * 为什么服务层还要自己判一次、不全靠 @SaCheckPermission：Portal 的可见性规则本来就是
 * 按能力分叉的（审阅者看任意状态、普通用户只看自己的草稿；评论删除权 = 作者本人或治理者），
 * 这类「同一个端点里按人走不同分支」的判断没法用整端点注解表达，口径出处
 * docs/DATA_CONTRACT.md 第 4 节，搬过来保持同构。
 */
@Component
@RequiredArgsConstructor
public class PortalAccess {

    private final PermissionApi permissionApi;

    public boolean has(Long userId, String capability) {
        return userId != null && permissionApi.hasAnyPermissions(userId, capability);
    }

    /** 审阅者：能看到并操作任意状态的稿（admin / management） */
    public boolean canViewAnyStatus(Long userId) {
        return has(userId, PortalCapability.VIEW_ANY_STATUS);
    }

    public boolean canReview(Long userId) {
        return has(userId, PortalCapability.REVIEW);
    }

    /** 全量编辑权：改任何人的稿都不会把已发布内容打回审批 */
    public boolean canEditAny(Long userId) {
        return has(userId, PortalCapability.EDIT_ANY);
    }

    public boolean canModerateComments(Long userId) {
        return has(userId, PortalCapability.MODERATE_COMMENTS);
    }

    /**
     * 分类写操作的权限闸：scope 必须先登记（未登记直接报错），再判当前用户持有该 scope 的管理能力。
     *
     * 返回注册表项，调用方顺手拿形状 / 名长 / 能力位，不用再 get 一次。
     */
    public PortalCategoryScope.Scope requireScopeManager(Long userId, String scope) {
        PortalCategoryScope.Scope spec = PortalCategoryScope.get(scope);
        if (!has(userId, spec.capability())) {
            throw exception(CATEGORY_MANAGE_FORBIDDEN);
        }
        return spec;
    }

}
