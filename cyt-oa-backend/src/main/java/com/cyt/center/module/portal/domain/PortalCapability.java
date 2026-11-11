package com.cyt.center.module.portal.domain;

/**
 * Portal 管理面的能力位（权限串）。
 *
 * 值是 B1 从 Portal 原样搬进 sys_menu.permission 的那批字符串（见
 * scripts/migrate_portal_identity.py 的 CAPABILITY_LABELS），一个都不改名：
 * Portal 的 require(can_review) 与 SSO 换到的 OA token 判的是同一批串，
 * 改了等于两头角色授权都要重配。
 *
 * 接口层用 @SaCheckPermission 闸整端点，服务层用
 * {@link com.cyt.center.module.portal.service.support.PortalAccess} 按人判
 * ——Portal 里「可见性按能力收敛」的规则全都落在应用层，搬过来也不上提到端点。
 */
public final class PortalCapability {

    public static final String ENTER_ADMIN = "can_enter_admin";
    public static final String CREATE_CONTENT = "can_create_content";
    public static final String EDIT_ANY = "can_edit_any";
    public static final String REVIEW = "can_review";
    public static final String DELETE = "can_delete";
    /** 审阅者视角：能看到任意状态的稿，普通用户只有自己写的 */
    public static final String VIEW_ANY_STATUS = "can_view_any_status";
    public static final String MODERATE_COMMENTS = "can_moderate_comments";
    public static final String VIEW_DASHBOARDS = "can_view_dashboards";
    public static final String MANAGE_MATERIALS = "can_manage_materials";
    public static final String MANAGE_TRAINING = "can_manage_training";
    public static final String MANAGE_EVENTS = "can_manage_events";
    public static final String MANAGE_PRODUCTS = "can_manage_products";
    public static final String EDIT_HR_DOCUMENTS = "can_edit_hr_documents";
    /** SOP 类文档（category=hr-sop）的读权限：只限 HR + admin，员工读侧可见性判断用 */
    public static final String VIEW_HR_SOP = "can_view_hr_sop";
    /** 制度签收覆盖率与未签清单查看（Portal 的 can_view_signoff_stats） */
    public static final String VIEW_SIGNOFF_STATS = "can_view_signoff_stats";

    private PortalCapability() {
    }

}
