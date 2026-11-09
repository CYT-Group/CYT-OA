package com.cyt.center.module.portal.domain;

import com.cyt.center.module.portal.dal.dataobject.category.PortalCategoryDO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_ATTRS_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_CODE_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_CODE_IMMUTABLE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_CODE_SETTLED;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_DEPTH_IMMUTABLE;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_NAME_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_SELF_PARENT;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_SHAPE_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.CATEGORY_SCOPE_INVALID;

/**
 * 通用分类的 scope 注册表与行级校验（迁自 Portal 的 domain/categories.py）。
 *
 * 一张 portal_categories 服务多个业务，靠 scope 认归属；每个 scope 的形状（平铺 / 两层）、
 * 名长、管理能力、attrs 白名单、code 规则全部登记在这里。
 * **加业务 = 加一行注册表**，不走迁移、不改表——所以 scope 刻意没有 DB CHECK，
 * 合法值只在这判（点号分层也给将来按 material.* 前缀配权限留口子）。
 *
 * 能力位用的是 Portal 迁来时原样搬进 sys_menu 的那串（can_manage_materials 等），
 * 与 @SaCheckPermission 用的是同一批字符串，B1 的角色授权因此不用再配一遍。
 */
public final class PortalCategoryScope {

    /** 平铺：全部行 depth 1，不许带 parent */
    public static final String SHAPE_FLAT = "flat";
    /** 两层：depth 1 = 大类，depth 2 = 小类 */
    public static final String SHAPE_TWO_LEVEL = "two_level";

    public static final String CODE_FORBIDDEN = "forbidden";
    public static final String CODE_OPTIONAL = "optional";
    public static final String CODE_REQUIRED_SLUG_IMMUTABLE = "required_slug_immutable";

    /** 引用计数 / 改名级联指向的业务表 */
    public static final String REF_MATERIAL_PRODUCTS = "material_products";
    public static final String REF_TRAINING = "training";

    /** 列宽 varchar(32)；物资 code 可选但仍受列宽约束 */
    public static final int CODE_MAX_LEN = 32;

    /** 大类 code 进 URL（/training/group/<code>），取值收紧到 slug */
    private static final Pattern GROUP_KEY = Pattern.compile("^[a-z][a-z0-9_-]{1,31}$");

    /** attrs 键的类型声明：类型 + 字符串长度上限（null = 不限） */
    public record AttrRule(Class<?> type, Integer maxLen) {
    }

    /** 一个 scope 的全部规则；attrsSchema / codeRule 按 depth 分键 */
    public record Scope(String key, String label, String shape, int nameMax, String capability,
                        Map<Integer, Map<String, AttrRule>> attrsSchema, Map<Integer, String> codeRule) {

        public boolean isFlat() {
            return SHAPE_FLAT.equals(shape);
        }

        /** 业务表引用的那一层：平铺 = 1，两层 = 2（培训课程只挂小类） */
        public int leafDepth() {
            return isFlat() ? 1 : 2;
        }
    }

    /**
     * 管理能力位按「精确 scope 优先，否则 &lt;前缀&gt;.* 兜底」解析：
     * material.sample / material.office 共用物资管理权，将来 material.consumable 加进来自然继承。
     * 分类是字典不是台账，归档 / 恢复 / 删除也走同一能力，不沿用物资档案硬删的 can_delete。
     */
    private static final Map<String, String> CAPABILITIES = Map.of(
            "material.*", PortalCapability.MANAGE_MATERIALS,
            "training.course", PortalCapability.MANAGE_TRAINING
    );

    /**
     * scope 注册表（加业务 = 加一行）。
     * nameMax：物资侧 32 是冻结面请求体 max_length=32 的镜像，永不放宽；
     * 培训 100 = NAS 顶层目录名宽度。
     */
    private static final Map<String, Scope> SCOPES = new LinkedHashMap<>();

    static {
        register(scope("material.sample", "样品品类", SHAPE_FLAT, 32,
                Map.of(1, Map.of()), Map.of(1, CODE_OPTIONAL)));
        register(scope("material.office", "办公用品品类", SHAPE_FLAT, 32,
                // employee_hidden：员工自助目录隐藏该品类（入职礼包走行政直发）
                Map.of(1, Map.of("employee_hidden", new AttrRule(Boolean.class, null))),
                Map.of(1, CODE_OPTIONAL)));
        register(scope("training.course", "培训分类", SHAPE_TWO_LEVEL, 100,
                // 大类卡片的 emoji 与副标题（原 portal_training_groups.icon / description）
                Map.of(1, Map.of("icon", new AttrRule(String.class, 16),
                                "description", new AttrRule(String.class, 200)),
                        2, Map.of()),
                Map.of(1, CODE_REQUIRED_SLUG_IMMUTABLE, 2, CODE_FORBIDDEN)));
    }

    private PortalCategoryScope() {
    }

    private static void register(Scope spec) {
        SCOPES.put(spec.key(), spec);
    }

    private static Scope scope(String key, String label, String shape, int nameMax,
                               Map<Integer, Map<String, AttrRule>> attrsSchema, Map<Integer, String> codeRule) {
        return new Scope(key, label, shape, nameMax, resolveCapability(key), attrsSchema, codeRule);
    }

    /**
     * 注册表漏配能力位属于开发期错误，不该在线上出现，所以直接抛 IllegalState 而不是业务异常。
     */
    private static String resolveCapability(String scopeKey) {
        String exact = CAPABILITIES.get(scopeKey);
        if (exact != null) {
            return exact;
        }
        String prefix = scopeKey.split("\\.", 2)[0] + ".*";
        String byPrefix = CAPABILITIES.get(prefix);
        if (byPrefix == null) {
            throw new IllegalStateException("scope " + scopeKey + " 没有登记管理能力位");
        }
        return byPrefix;
    }

    /** 全部已登记的 scope，管理页的scope下拉用 */
    public static Set<String> keys() {
        return SCOPES.keySet();
    }

    public static Map<String, Scope> all() {
        return new LinkedHashMap<>(SCOPES);
    }

    /** 取 scope 的注册表项；不在注册表里直接报错——scope 无 DB CHECK，合法值只在这判 */
    public static Scope get(String scope) {
        Scope spec = SCOPES.get(scope == null ? "" : scope.trim());
        if (spec == null) {
            throw exception(CATEGORY_SCOPE_INVALID, scope);
        }
        return spec;
    }

    /** 分类名去首尾空白、非空、不超过该 scope 的 nameMax */
    public static String cleanName(String scope, String name) {
        Scope spec = get(scope);
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            throw exception(CATEGORY_NAME_INVALID, "分类名不能为空");
        }
        if (value.length() > spec.nameMax()) {
            throw exception(CATEGORY_NAME_INVALID, "分类名不能超过 " + spec.nameMax() + " 字");
        }
        return value;
    }

    /**
     * 按 scope / depth 的白名单校验 attrs，返回清洗后的 Map（整体替换语义，不做合并）。
     *
     * 字符串值去首尾空白，空串视同没传；bool 只认真 bool——JSON 里的 1 / "true" 都不算。
     * 纪律靠契约 + 校验，不靠真列：开真列的话，每个只对某个 scope 有意义的开关
     * 都会变成通用表上的一个可空列 + 一次迁移。
     */
    public static Map<String, Object> cleanAttrs(String scope, Integer depth, Map<String, Object> attrs) {
        Scope spec = get(scope);
        if (attrs == null) {
            return Map.of();
        }
        Map<String, AttrRule> schema = spec.attrsSchema().getOrDefault(depth, Map.of());
        if (schema.isEmpty()) {
            if (!attrs.isEmpty()) {
                throw exception(CATEGORY_ATTRS_INVALID, "该层级的分类不允许带 attrs");
            }
            return Map.of();
        }
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            AttrRule rule = schema.get(key);
            if (rule == null) {
                throw exception(CATEGORY_ATTRS_INVALID, "attrs 不支持的键: " + key);
            }
            if (rule.type() == Boolean.class) {
                if (!(value instanceof Boolean bool)) {
                    throw exception(CATEGORY_ATTRS_INVALID, "attrs." + key + " 必须是布尔值");
                }
                cleaned.put(key, bool);
            } else if (rule.type() == String.class) {
                if (!(value instanceof String text)) {
                    throw exception(CATEGORY_ATTRS_INVALID, "attrs." + key + " 必须是字符串");
                }
                String trimmed = text.trim();
                if (rule.maxLen() != null && trimmed.length() > rule.maxLen()) {
                    throw exception(CATEGORY_ATTRS_INVALID, "attrs." + key + " 不能超过 " + rule.maxLen() + " 字");
                }
                if (!trimmed.isEmpty()) {
                    cleaned.put(key, trimmed);
                }
            } else {
                throw new IllegalStateException("attrs 类型 " + rule.type() + " 未支持");
            }
        }
        return cleaned;
    }

    /**
     * 按 scope / depth 的 code 规则校验，返回清洗后的 code（null = 不设）。
     *
     * - forbidden：传了非空就报错（培训小类的身份就是名字）
     * - optional：建时可给可不给；改时只有当前为 null 才能填，已有值不许改 / 清
     *   （code 是 seed 幂等键，改了迁移脚本就找不到行）
     * - required_slug_immutable：建时必填且是 slug；改时一律拒（进了前端路由）
     * 调用方改行时若新值与当前值相同，应当视为没传、不要调本方法。
     */
    public static String cleanCode(String scope, Integer depth, String code, boolean isCreate, String current) {
        Scope spec = get(scope);
        String rule = spec.codeRule().getOrDefault(depth, CODE_FORBIDDEN);
        String value = code == null || code.trim().isEmpty() ? null : code.trim();
        if (CODE_FORBIDDEN.equals(rule)) {
            if (value != null) {
                throw exception(CATEGORY_CODE_INVALID, "该层级的分类不允许带 code");
            }
            return null;
        }
        if (CODE_REQUIRED_SLUG_IMMUTABLE.equals(rule)) {
            if (!isCreate) {
                throw exception(CATEGORY_CODE_IMMUTABLE);
            }
            return validateGroupKey(value);
        }
        // optional
        if (!isCreate && current != null) {
            throw exception(CATEGORY_CODE_SETTLED);
        }
        if (value != null && value.length() > CODE_MAX_LEN) {
            throw exception(CATEGORY_CODE_INVALID, "code 不能超过 " + CODE_MAX_LEN + " 字");
        }
        return value;
    }

    private static String validateGroupKey(String value) {
        String key = value == null ? "" : value.trim();
        if (!GROUP_KEY.matcher(key).matches()) {
            throw exception(CATEGORY_CODE_INVALID,
                    "大类 code 只能是小写字母开头的 2-32 位 slug（a-z 0-9 _ -）");
        }
        return key;
    }

    /**
     * 由 parent 行推出新行 / 改后行的 depth，并校验形状。
     *
     * two_level 下 parent 必须是同 scope、depth 1、在役的行——挂到归档大类下
     * 等于挂到一个员工端永远看不到的地方。
     */
    public static int resolveDepth(String scope, PortalCategoryDO parent) {
        Scope spec = get(scope);
        if (spec.isFlat()) {
            if (parent != null) {
                throw exception(CATEGORY_SHAPE_INVALID, "该分类不分层，不能指定 parent_id");
            }
            return 1;
        }
        if (parent == null) {
            return 1;
        }
        if (!spec.key().equals(parent.getScope())) {
            throw exception(CATEGORY_SHAPE_INVALID, "父分类不属于同一 scope");
        }
        if (!Integer.valueOf(1).equals(parent.getDepth())) {
            throw exception(CATEGORY_SHAPE_INVALID, "父分类必须是大类（depth 1）");
        }
        if (parent.getArchivedAt() != null) {
            throw exception(CATEGORY_SHAPE_INVALID, "父分类已归档，不能挂到它下面");
        }
        return 2;
    }

    /**
     * 改既有行的父分类，返回要写入的新值。
     *
     * 父为空 = 摘父：小类变未归类（depth 不变），大类 / 平铺行本来无父 → 无事。
     * 父非空：不能是自己；按 resolveDepth 校验形状，且推出的 depth 必须等于当前 depth——
     * 大类 ↔ 小类互转会让 code 规则、子行、课程引用一起失效，一律拒。
     */
    public static Long validateReparent(String scope, PortalCategoryDO row, PortalCategoryDO parent) {
        if (parent == null) {
            return null;
        }
        if (parent.getId().equals(row.getId())) {
            throw exception(CATEGORY_SELF_PARENT);
        }
        if (resolveDepth(scope, parent) != row.getDepth()) {
            throw exception(CATEGORY_DEPTH_IMMUTABLE);
        }
        return parent.getId();
    }

    /**
     * 该行被哪张业务表引用（引用计数 / 改名级联据此选表）；null = 没有业务表直接引用。
     * 培训大类没有业务表挂它，归档前置看 childrenCount 不看引用数。
     */
    public static String refTarget(String scope, Integer depth) {
        Scope spec = get(scope);
        if (spec.key().startsWith("material.")) {
            return REF_MATERIAL_PRODUCTS;
        }
        if ("training.course".equals(spec.key()) && Integer.valueOf(2).equals(depth)) {
            return REF_TRAINING;
        }
        return null;
    }

}
