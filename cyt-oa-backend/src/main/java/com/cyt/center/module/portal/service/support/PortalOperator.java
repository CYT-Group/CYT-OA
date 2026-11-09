package com.cyt.center.module.portal.service.support;

/**
 * 当前登录人在 Portal 口径下的身份。
 *
 * portal_* 表认的是 username（= 企微 userid = Portal 的 portal_users.username），不是 sys_user.id：
 * 作者、点赞人、评论人、埋点都是按名认人，账号注销后历史行仍要读得出是谁，
 * 所以迁过来时没有改成用户编号外键（口径 docs/Portal管理面迁移契约.md §一「身份与权限」）。
 *
 * @param displayName 通讯录姓名，可能为空——Portal 侧两处口径不同：内容行用「姓名或账号」兜底，
 *                    埋点快照按原值存 null，所以这里保留原始值，兜底交给调用方
 * @param department  主部门名快照，埋点行要用；无主部门时为 null
 * @param avatar      头像地址快照，评论行要在写入时把头像一起存下来；通讯录没填时为 null
 */
public record PortalOperator(Long userId, String username, String displayName, String department, String avatar) {

    /** Portal 写内容行的口径：display_name or username */
    public String displayNameOrUsername() {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }

}
