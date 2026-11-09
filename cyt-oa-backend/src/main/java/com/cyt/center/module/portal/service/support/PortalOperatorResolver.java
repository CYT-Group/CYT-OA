package com.cyt.center.module.portal.service.support;

import com.cyt.center.module.system.api.dept.DeptApi;
import com.cyt.center.module.system.api.dept.dto.DeptRespDTO;
import com.cyt.center.module.system.api.user.AdminUserApi;
import com.cyt.center.module.system.api.user.dto.AdminUserRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.cyt.center.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;
import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 把 Sa-Token 的登录编号换成 {@link PortalOperator}。
 *
 * Portal 那边这些值是登录时一次性挂在 session 上的 ORM 行，OA 这边按编号现查：
 * 姓名与部门是通讯录字段，会随企微同步变动，落库的快照列才是历史事实，读的时候不该回头猜。
 */
@Component
@RequiredArgsConstructor
public class PortalOperatorResolver {

    private final AdminUserApi adminUserApi;
    private final DeptApi deptApi;

    /**
     * @param userId 登录用户编号
     * @return 当前人；账号已被删掉（令牌还在但人没了）按未登录处理，不给一个假身份
     */
    public PortalOperator resolve(Long userId) {
        AdminUserRespDTO user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null || user.getUsername() == null) {
            throw exception(UNAUTHORIZED);
        }
        return new PortalOperator(user.getId(), user.getUsername(), user.getNickname(),
                deptName(user.getDeptId()), user.getAvatar());
    }

    /** 主部门名单值；埋点快照用，账号没挂部门就是 null（不猜、不填「未知」） */
    private String deptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        DeptRespDTO dept = deptApi.getDept(deptId);
        return dept == null ? null : dept.getName();
    }

}
