package com.cyt.center.module.portal.domain.hrdoc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_FIELDS_INVALID;

/**
 * HR 文档块格式 schema（冻结契约，对齐 Portal 的 domain/hr_documents.py 块校验段）。
 *
 * 块文档 = JSON 数组，每项按判别字段 type 分发；校验必须 fail-loud——未知 type / 未知键 / 缺键 /
 * 类型不符一律 422，不静默丢弃。纯函数零 IO；update_doc 写 draft_blocks 前必须先过这里。
 *
 * 形状表是唯一真相：sanitize 也按同一张表递归推导 HTML 叶子字段，不另存手写映射。
 */
public final class HrDocumentBlockValidator {

    public static final int MAX_BLOCKS = 100;
    public static final int MAX_ITEMS_PER_BLOCK = 30;
    public static final int MAX_LIST_ITEMS = 30;
    public static final int MAX_TABS = 8;
    public static final int MAX_SHORT_TEXT = 200;
    public static final int MAX_ICON = 16;
    public static final int MAX_PLAIN_TEXT = 2000;
    public static final int MAX_HTML_BYTES = 200 * 1024;
    public static final int MAX_IMAGE_URL = 500;
    public static final int MAX_HR_DOCUMENT_SLUG = 64;

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern SLUG_RE = Pattern.compile("^[a-z0-9-]+$");

    private static final Set<String> CALLOUT_VARIANTS = Set.of("info", "success", "warning", "danger");
    private static final Set<Integer> CARDS_COLUMNS = Set.of(2, 3);
    private static final Set<String> STATUS_LEVELS = Set.of("pass", "fail", "warn");
    private static final Set<String> ALERT_VARIANTS = Set.of("info", "warn", "danger", "success");

    /** 结构化块的条目字段形状（kind 含义见 checkValue） */
    private static final Map<String, Map<String, String>> ITEM_FIELDS = Map.of(
            "keypoints", Map.of("icon", "icon", "title", "short", "desc", "plain"),
            "cards", Map.of("icon", "icon", "title", "short", "text", "plain"),
            "steps", Map.of("title", "short", "desc", "plain"),
            "metrics", Map.of("value", "short", "label", "short"),
            "status", Map.of("status", "status", "text", "short", "tag", "short"),
            "bento", Map.of("icon", "icon", "title", "short", "desc", "html", "color", "color"),
            "accordion", Map.of("icon", "icon", "title", "short", "lines", "list", "open", "bool"));

    private static final Map<String, String> SIDE_FIELDS = Map.of("title", "short", "color", "color", "items", "list");

    private static final Map<String, String> TAB_FIELDS = Map.of(
            "name", "short", "header", "short", "subHeader", "short", "metrics", "items", "note", "html");

    /** 19 种块的字段形状 */
    private static final Map<String, Map<String, String>> BLOCK_FIELDS = buildBlockFields();

    /** 后加主题覆盖 / 兼容缺口的可选字段（老 JSONB 块没有这些键也必须继续可编辑/发布） */
    private static final Map<String, Set<String>> OPTIONAL_BLOCK_FIELDS = Map.of(
            "quote", Set.of("color", "bg"),
            "docref", Set.of("slug", "document_id", "text", "button_text", "button_url"));

    private HrDocumentBlockValidator() {
    }

    /** 排版模型输出补全复用同一份形状，避免 AI 和编辑器字段漂移。 */
    public static Map<String,String> blockFieldSpec(String type){return BLOCK_FIELDS.getOrDefault(type,Map.of());}
    public static Map<String,String> itemFieldSpec(String type){return ITEM_FIELDS.getOrDefault(type,Map.of());}
    public static Map<String,String> sideFieldSpec(){return SIDE_FIELDS;}
    public static Map<String,String> tabFieldSpec(){return TAB_FIELDS;}

    private static Map<String, Map<String, String>> buildBlockFields() {
        Map<String, Map<String, String>> m = new LinkedHashMap<>();
        m.put("text", Map.of("text", "html"));
        m.put("image", Map.of("image", "url", "caption", "short"));
        m.put("imageText", Map.of("image", "url", "title", "short", "text", "html"));
        m.put("section", Map.of("title", "short"));
        m.put("callout", Map.of("variant", "variant", "text", "html"));
        m.put("keypoints", Map.of("items", "items"));
        m.put("cards", Map.of("columns", "columns", "items", "items"));
        m.put("steps", Map.of("items", "items"));
        m.put("divider", Map.of("text", "plain"));
        m.put("rawhtml", Map.of("html", "html"));
        m.put("quote", Map.of("text", "html", "color", "themecolor", "bg", "themecolor"));
        m.put("metrics", Map.of("items", "items"));
        m.put("compare", Map.of("left", "side", "right", "side"));
        m.put("status", Map.of("items", "items"));
        m.put("alert", Map.of("variant", "alertvariant", "title", "short", "text", "html"));
        m.put("bento", Map.of("cards", "items"));
        m.put("numsec", Map.of("num", "short", "title", "short", "text", "html", "color", "themecolor"));
        m.put("tabcard", Map.of("activeTab", "tabindex", "tabs", "tabs"));
        m.put("accordion", Map.of("items", "items"));
        m.put("docref", Map.of("slug", "docslug", "document_id", "documentid", "title", "requiredshort",
                "text", "html", "button_text", "short", "button_url", "linkurl"));
        return m;
    }

    /** 校验块数组完整形状，不合规抛 422。 */
    public static void validate(List<Object> blocks) {
        if (blocks == null) {
            throw exception(HRDOC_FIELDS_INVALID, "blocks 必须是数组");
        }
        if (blocks.size() > MAX_BLOCKS) {
            throw exception(HRDOC_FIELDS_INVALID, "blocks 最多 " + MAX_BLOCKS + " 块（当前 " + blocks.size() + "）");
        }
        for (int i = 0; i < blocks.size(); i++) {
            String where = "blocks[" + i + "]";
            Object raw = blocks.get(i);
            if (!(raw instanceof Map)) {
                throw exception(HRDOC_FIELDS_INVALID, where + " 必须是对象");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> block = (Map<String, Object>) raw;
            Object typeObj = block.get("type");
            if (!(typeObj instanceof String) || !BLOCK_FIELDS.containsKey(typeObj)) {
                throw exception(HRDOC_FIELDS_INVALID, where + ".type 未知块类型: " + typeObj);
            }
            String btype = (String) typeObj;
            Map<String, String> fields = BLOCK_FIELDS.get(btype);
            Set<String> optional = OPTIONAL_BLOCK_FIELDS.getOrDefault(btype, Set.of());
            Set<String> extra = new java.util.TreeSet<>(block.keySet());
            extra.removeAll(fields.keySet());
            extra.remove("type");
            if (!extra.isEmpty()) {
                throw exception(HRDOC_FIELDS_INVALID, where + "(" + btype + ") 含未知键: " + extra);
            }
            for (Map.Entry<String, String> e : fields.entrySet()) {
                String key = e.getKey();
                if (!block.containsKey(key)) {
                    if (optional.contains(key)) {
                        continue;
                    }
                    throw exception(HRDOC_FIELDS_INVALID, where + "(" + btype + ") 缺字段 " + key);
                }
                checkValue(where + "." + key, e.getValue(), block.get(key), btype);
            }
            if ("docref".equals(btype) && !docrefHasIdentity(block)) {
                throw exception(HRDOC_FIELDS_INVALID, where + "(docref) 必须提供 document_id 或 legacy slug");
            }
        }
    }

    private static boolean docrefHasIdentity(Map<String, Object> block) {
        Object documentId = block.get("document_id");
        if (documentId instanceof Number && !(documentId instanceof Boolean)) {
            return true;
        }
        Object slug = block.get("slug");
        return slug instanceof String && !((String) slug).isBlank();
    }

    /** 返回新数组：HTML 载荷字段逐个过 sanitizer，其余字段原样透传。调用前提：blocks 已过 validate。 */
    public static List<Object> sanitize(List<Object> blocks, UnaryOperator<String> sanitizer) {
        List<Object> out = new ArrayList<>(blocks.size());
        for (Object raw : blocks) {
            if (!(raw instanceof Map)) {
                out.add(raw);
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> block = (Map<String, Object>) raw;
            String btype = block.get("type") instanceof String ? (String) block.get("type") : "";
            out.add(sanitizeFields(BLOCK_FIELDS.getOrDefault(btype, Map.of()), block, btype, sanitizer));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitizeFields(Map<String, String> spec, Map<String, Object> value,
                                                      String btype, UnaryOperator<String> sanitizer) {
        Map<String, Object> out = new LinkedHashMap<>(value);
        for (Map.Entry<String, String> e : spec.entrySet()) {
            String key = e.getKey();
            if (out.containsKey(key)) {
                out.put(key, sanitizeByKind(e.getValue(), out.get(key), btype, sanitizer));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object sanitizeByKind(String kind, Object value, String btype, UnaryOperator<String> sanitizer) {
        if ("html".equals(kind)) {
            return value instanceof String ? sanitizer.apply((String) value) : value;
        }
        if ("items".equals(kind) && value instanceof List) {
            Map<String, String> spec = ITEM_FIELDS.getOrDefault(btype, Map.of());
            List<Object> items = (List<Object>) value;
            List<Object> out = new ArrayList<>(items.size());
            for (Object it : items) {
                out.add(it instanceof Map ? sanitizeFields(spec, (Map<String, Object>) it, btype, sanitizer) : it);
            }
            return out;
        }
        if ("side".equals(kind) && value instanceof Map) {
            return sanitizeFields(SIDE_FIELDS, (Map<String, Object>) value, "compare", sanitizer);
        }
        if ("tabs".equals(kind) && value instanceof List) {
            List<Object> tabs = (List<Object>) value;
            List<Object> out = new ArrayList<>(tabs.size());
            for (Object t : tabs) {
                out.add(t instanceof Map ? sanitizeFields(TAB_FIELDS, (Map<String, Object>) t, "metrics", sanitizer) : t);
            }
            return out;
        }
        return value;
    }

    // ── 字段 kind 分发 ──

    private static void checkValue(String where, String kind, Object value, String btype) {
        switch (kind) {
            case "short" -> checkStr(where, value, MAX_SHORT_TEXT);
            case "requiredshort" -> checkRequiredStr(where, value, MAX_SHORT_TEXT);
            case "plain" -> checkStr(where, value, MAX_PLAIN_TEXT);
            case "icon" -> checkStr(where, value, MAX_ICON);
            case "html" -> checkHtml(where, value);
            case "url" -> requireAssetUrl(where, value, "图片");
            case "variant" -> checkEnum(where, value, CALLOUT_VARIANTS);
            case "columns" -> checkColumns(where, value);
            case "status" -> checkEnum(where, value, STATUS_LEVELS);
            case "alertvariant" -> checkEnum(where, value, ALERT_VARIANTS);
            case "color" -> checkHexColor(where, value, false);
            case "themecolor" -> checkThemeColor(where, value);
            case "linkurl" -> checkLinkUrl(where, value);
            case "docslug" -> checkDocSlug(where, value);
            case "documentid" -> checkPositiveInt(where, value);
            case "bool" -> checkBool(where, value);
            case "tabindex" -> checkTabIndex(where, value);
            case "list" -> checkList(where, value);
            case "side" -> checkSide(where, value);
            case "tabs" -> checkTabs(where, value);
            case "items" -> checkItems(where, value, btype);
            default -> throw exception(HRDOC_FIELDS_INVALID, where + " 未知字段种类 " + kind);
        }
    }

    private static void checkStr(String where, Object value, int maxLen) {
        if (!(value instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是字符串");
        }
        if (((String) value).length() > maxLen) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 超长（上限 " + maxLen + " 字）");
        }
    }

    private static void checkRequiredStr(String where, Object value, int maxLen) {
        checkStr(where, value, maxLen);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 不能为空");
        }
    }

    private static void checkHtml(String where, Object value) {
        if (!(value instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是字符串");
        }
        if (((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            throw exception(HRDOC_FIELDS_INVALID, where + " HTML 超长（单块上限 200KB）");
        }
    }

    private static void requireAssetUrl(String where, Object value, String what) {
        if (!(value instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是字符串");
        }
        String s = (String) value;
        if (s.length() > MAX_IMAGE_URL) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 超长（上限 " + MAX_IMAGE_URL + " 字）");
        }
        if (s.startsWith("//") || !(s.startsWith("https://") || s.startsWith("/"))) {
            throw exception(HRDOC_FIELDS_INVALID, where + " " + what + "地址必须以 https:// 或 / 开头");
        }
    }

    private static void checkEnum(String where, Object value, Set<String> allowed) {
        if (!(value instanceof String) || !allowed.contains(value)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须在 " + new java.util.TreeSet<>(allowed) + " 内");
        }
    }

    private static void checkColumns(String where, Object value) {
        if (value instanceof Boolean || !(value instanceof Number) || !CARDS_COLUMNS.contains(((Number) value).intValue())) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 只能是 2 或 3");
        }
    }

    private static void checkHexColor(String where, Object value, boolean allowEmpty) {
        if (!(value instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是 #RRGGBB 形式的颜色");
        }
        String s = (String) value;
        if (allowEmpty && s.isEmpty()) {
            return;
        }
        if (!HEX_COLOR.matcher(s).matches()) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是 #RRGGBB 形式的颜色");
        }
    }

    private static void checkThemeColor(String where, Object value) {
        if (!(value instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是字符串");
        }
        String s = (String) value;
        if (!s.isEmpty() && !HEX_COLOR.matcher(s).matches()) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是 #RRGGBB 形式的颜色或空串（空串=跟随文档主题）");
        }
    }

    private static void checkLinkUrl(String where, Object value) {
        if (!(value instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是字符串");
        }
        String s = (String) value;
        if (!s.isEmpty()) {
            requireAssetUrl(where, value, "链接");
        }
    }

    private static void checkDocSlug(String where, Object value) {
        if (!(value instanceof String) || ((String) value).length() > MAX_HR_DOCUMENT_SLUG
                || !SLUG_RE.matcher((String) value).matches()) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是 1~" + MAX_HR_DOCUMENT_SLUG + " 位小写字母、数字或连字符");
        }
    }

    private static void checkPositiveInt(String where, Object value) {
        if (value instanceof Boolean || !(value instanceof Number) || ((Number) value).intValue() <= 0) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是正整数");
        }
    }

    private static void checkBool(String where, Object value) {
        if (!(value instanceof Boolean)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是布尔值");
        }
    }

    private static void checkTabIndex(String where, Object value) {
        if (value instanceof Boolean || !(value instanceof Number)
                || ((Number) value).intValue() < 0 || ((Number) value).intValue() >= MAX_TABS) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是 0~" + (MAX_TABS - 1) + " 的整数");
        }
    }

    private static void checkList(String where, Object value) {
        if (!(value instanceof List)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是数组");
        }
        List<?> list = (List<?>) value;
        if (list.size() > MAX_LIST_ITEMS) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 最多 " + MAX_LIST_ITEMS + " 条（当前 " + list.size() + "）");
        }
        for (int j = 0; j < list.size(); j++) {
            checkStr(where + "[" + j + "]", list.get(j), MAX_PLAIN_TEXT);
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkSide(String where, Object value) {
        checkShape(where, value, SIDE_FIELDS, "compare");
    }

    @SuppressWarnings("unchecked")
    private static void checkTabs(String where, Object value) {
        if (!(value instanceof List)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是数组");
        }
        List<?> tabs = (List<?>) value;
        if (tabs.size() > MAX_TABS) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 最多 " + MAX_TABS + " 个页签（当前 " + tabs.size() + "）");
        }
        for (int j = 0; j < tabs.size(); j++) {
            checkShape(where + "[" + j + "]", tabs.get(j), TAB_FIELDS, "metrics");
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkItems(String where, Object value, String btype) {
        if (!(value instanceof List)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是数组");
        }
        List<?> items = (List<?>) value;
        if (items.size() > MAX_ITEMS_PER_BLOCK) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 最多 " + MAX_ITEMS_PER_BLOCK + " 条（当前 " + items.size() + "）");
        }
        Map<String, String> itemFields = ITEM_FIELDS.get(btype);
        for (int j = 0; j < items.size(); j++) {
            checkShape(where + "[" + j + "]", items.get(j), itemFields, btype);
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkShape(String where, Object value, Map<String, String> fields, String btype) {
        if (!(value instanceof Map)) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 必须是对象");
        }
        Map<String, Object> obj = (Map<String, Object>) value;
        Set<String> extra = new java.util.TreeSet<>(obj.keySet());
        extra.removeAll(fields.keySet());
        if (!extra.isEmpty()) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 含未知键: " + extra);
        }
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String key = e.getKey();
            if (!obj.containsKey(key)) {
                throw exception(HRDOC_FIELDS_INVALID, where + " 缺字段 " + key);
            }
            checkValue(where + "." + key, e.getValue(), obj.get(key), btype);
        }
    }

}
