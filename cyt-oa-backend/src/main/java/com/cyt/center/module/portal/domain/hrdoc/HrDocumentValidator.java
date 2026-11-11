package com.cyt.center.module.portal.domain.hrdoc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cyt.center.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_CATEGORY_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_FIELDS_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_SLUG_INVALID;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.HRDOC_TOPIC_INVALID;

/**
 * HR 文档领域规则（对齐 Portal 的 domain/hr_documents.py 的展示字段 / slug / 封面 / 签收 / 发布段）。
 *
 * 纯规则零 SQL：分类/主题/ slug 值域、整屏封面与签收卡的整体校验 + 补默认、三态晋升、
 * 发布版本号校验。块 schema 见 {@link HrDocumentBlockValidator}。
 */
public final class HrDocumentValidator {

    public static final Set<String> VALID_CATEGORIES = Set.of("handbook", "incentive", "hr-sop", "hr", "policy");
    public static final Set<String> VALID_TOPICS = Set.of("performance", "attendance", "salary", "benefits", "admin", "growth");

    public static final int MAX_OFFLINE_REASON = 200;
    public static final int MAX_INTRO = 500;
    public static final int MAX_TAGS = 10;
    public static final int MAX_TAG_LEN = 20;

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern SLUG_RE = Pattern.compile("^[a-z0-9-]+$");
    private static final int MAX_SLUG = 64;

    private static final Set<String> THEME_PRESETS = Set.of("violet", "blue", "orange", "green", "slate", "rose");
    private static final Set<String> TOC_POSITIONS = Set.of("left", "right");

    private static final Set<String> COVER_BG_TYPES = Set.of("dark", "gradient", "image");
    private static final int MAX_COVER_LABEL = 40;
    private static final int MAX_COVER_ICON = 8;
    private static final int MAX_COVER_DESC = 300;
    private static final int MAX_COVER_META = 60;
    private static final int MAX_COVER_TABS = 8;
    private static final int MAX_COVER_TAB_LEN = 20;

    private static final int MAX_SIGNOFF_TITLE = 60;
    private static final int MAX_SIGNOFF_ACK = 200;
    private static final int MAX_SIGNOFF_BUTTON = 200;
    private static final int MAX_SIGNOFF_NOTE = 300;

    private HrDocumentValidator() {
    }

    // ── 分类 / 主题 / slug ──

    public static void requireValidCategory(String category) {
        if (category == null || !VALID_CATEGORIES.contains(category)) {
            throw exception(HRDOC_CATEGORY_INVALID, "category 必须在 " + VALID_CATEGORIES + " 内");
        }
    }

    public static void requireValidTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return;
        }
        if (!VALID_TOPICS.contains(topic)) {
            throw exception(HRDOC_TOPIC_INVALID, "topic 必须在 " + VALID_TOPICS + " 内");
        }
    }

    public static void requireValidSlug(String slug) {
        if (slug == null || !SLUG_RE.matcher(slug).matches()) {
            throw exception(HRDOC_SLUG_INVALID, "slug 只能由小写字母、数字与连字符组成");
        }
        if (slug.length() < 2 || slug.length() > MAX_SLUG) {
            throw exception(HRDOC_SLUG_INVALID, "slug 长度需在 2-" + MAX_SLUG + " 之间");
        }
    }

    public static void requireValidOfflineReason(String reason) {
        if (reason != null && reason.length() > MAX_OFFLINE_REASON) {
            throw exception(HRDOC_FIELDS_INVALID, "下架原因不能超过 " + MAX_OFFLINE_REASON + " 字");
        }
    }

    // ── 文档级展示字段 ──

    public static void requireValidIntro(String intro) {
        if (intro == null) {
            return;
        }
        if (intro.length() > MAX_INTRO) {
            throw exception(HRDOC_FIELDS_INVALID, "引言 超长（上限 " + MAX_INTRO + " 字）");
        }
    }

    public static void requireValidCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isEmpty()) {
            return;
        }
        requireAssetUrl("封面图", coverImage);
    }

    public static void requireValidTheme(String theme) {
        if (theme == null) {
            throw exception(HRDOC_FIELDS_INVALID, "主题必须是字符串");
        }
        if (theme.isEmpty() || THEME_PRESETS.contains(theme) || HEX_COLOR.matcher(theme).matches()) {
            return;
        }
        throw exception(HRDOC_FIELDS_INVALID, "主题只能是空串、" + THEME_PRESETS + " 之一或 #RRGGBB 形式");
    }

    public static void requireValidTocPosition(String tocPosition) {
        if (tocPosition == null || !TOC_POSITIONS.contains(tocPosition)) {
            throw exception(HRDOC_FIELDS_INVALID, "目录位置必须是 " + TOC_POSITIONS + " 之一");
        }
    }

    public static void requireValidTags(List<String> tags) {
        if (tags == null) {
            return;
        }
        if (tags.size() > MAX_TAGS) {
            throw exception(HRDOC_FIELDS_INVALID, "标签最多 " + MAX_TAGS + " 个（当前 " + tags.size() + "）");
        }
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            if (tag == null || tag.isBlank()) {
                throw exception(HRDOC_FIELDS_INVALID, "标签[" + i + "] 不能为空");
            }
            if (tag.length() > MAX_TAG_LEN) {
                throw exception(HRDOC_FIELDS_INVALID, "标签[" + i + "] 超长（上限 " + MAX_TAG_LEN + " 字）");
            }
        }
    }

    private static void requireAssetUrl(String where, String value) {
        if (value.length() > 500) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 超长（上限 500 字）");
        }
        if (value.startsWith("//") || !(value.startsWith("https://") || value.startsWith("/"))) {
            throw exception(HRDOC_FIELDS_INVALID, where + " 地址必须以 https:// 或 / 开头");
        }
    }

    // ── 整屏封面（10 键整体 JSON，整体替换 + 缺键补默认 + 未知键 422）──

    private static final List<String> COVER_KEYS = List.of(
            "bg_type", "image", "label_en", "label_cn", "icon", "desc", "date", "scope", "dept", "nav_tabs");

    public static void requireValidCover(Map<String, Object> cover) {
        if (cover == null) {
            throw exception(HRDOC_FIELDS_INVALID, "封面配置不能为 null（要清空请传空对象 {}）");
        }
        Set<String> extra = new java.util.TreeSet<>(cover.keySet());
        extra.removeAll(COVER_KEYS);
        if (!extra.isEmpty()) {
            throw exception(HRDOC_FIELDS_INVALID, "封面配置含未知键: " + extra);
        }
        if (cover.containsKey("bg_type") && !(cover.get("bg_type") instanceof String)
                || cover.containsKey("bg_type") && !COVER_BG_TYPES.contains(cover.get("bg_type"))) {
            throw exception(HRDOC_FIELDS_INVALID, "封面背景风格必须是 " + COVER_BG_TYPES + " 之一");
        }
        if (cover.containsKey("image")) {
            Object img = cover.get("image");
            if (!(img instanceof String)) {
                throw exception(HRDOC_FIELDS_INVALID, "封面图必须是字符串");
            }
            if (!((String) img).isEmpty()) {
                requireAssetUrl("封面图", (String) img);
            }
        }
        checkStrLimit(cover, "label_en", MAX_COVER_LABEL);
        checkStrLimit(cover, "label_cn", MAX_COVER_LABEL);
        checkStrLimit(cover, "icon", MAX_COVER_ICON);
        checkStrLimit(cover, "desc", MAX_COVER_DESC);
        checkStrLimit(cover, "date", MAX_COVER_META);
        checkStrLimit(cover, "scope", MAX_COVER_META);
        checkStrLimit(cover, "dept", MAX_COVER_META);
        if (cover.containsKey("nav_tabs")) {
            Object tabs = cover.get("nav_tabs");
            if (!(tabs instanceof List)) {
                throw exception(HRDOC_FIELDS_INVALID, "封面导航标签必须是数组");
            }
            List<?> list = (List<?>) tabs;
            if (list.size() > MAX_COVER_TABS) {
                throw exception(HRDOC_FIELDS_INVALID, "封面导航标签最多 " + MAX_COVER_TABS + " 个（当前 " + list.size() + "）");
            }
            for (int i = 0; i < list.size(); i++) {
                Object tab = list.get(i);
                if (!(tab instanceof String) || ((String) tab).length() > MAX_COVER_TAB_LEN) {
                    throw exception(HRDOC_FIELDS_INVALID, "封面导航标签[" + i + "] 超长（上限 " + MAX_COVER_TAB_LEN + " 字）");
                }
            }
        }
    }

    private static void checkStrLimit(Map<String, Object> cover, String key, int limit) {
        if (!cover.containsKey(key)) {
            return;
        }
        Object v = cover.get(key);
        if (!(v instanceof String)) {
            throw exception(HRDOC_FIELDS_INVALID, "封面." + key + " 必须是字符串");
        }
        if (((String) v).length() > limit) {
            throw exception(HRDOC_FIELDS_INVALID, "封面." + key + " 超长（上限 " + limit + " 字）");
        }
    }

    public static Map<String, Object> normalizeCover(Map<String, Object> value) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bg_type", "gradient");
        out.put("image", "");
        out.put("label_en", "");
        out.put("label_cn", "");
        out.put("icon", "");
        out.put("desc", "");
        out.put("date", "");
        out.put("scope", "");
        out.put("dept", "");
        out.put("nav_tabs", new ArrayList<>());
        if (value != null) {
            out.putAll(value);
        }
        Object tabs = out.get("nav_tabs");
        out.put("nav_tabs", tabs instanceof List ? new ArrayList<>((List<?>) tabs) : new ArrayList<>());
        return out;
    }

    // ── 员工签收卡配置（整体 JSON，同 cover 口径）──

    private static final Set<String> SIGNOFF_HTML_KEYS = Set.of("lead", "tip");

    public static void requireValidSignoff(Map<String, Object> signoff) {
        if (signoff == null) {
            throw exception(HRDOC_FIELDS_INVALID, "签收卡配置不能为 null（要清空请传空对象 {}）");
        }
        Set<String> extra = new java.util.TreeSet<>(signoff.keySet());
        extra.removeAll(List.of("icon", "title", "lead", "ack_label", "button_text", "tip", "signed_note"));
        if (!extra.isEmpty()) {
            throw exception(HRDOC_FIELDS_INVALID, "签收卡配置含未知键: " + extra);
        }
        for (String key : List.of("icon", "title", "ack_label", "button_text", "signed_note")) {
            if (signoff.containsKey(key) && !(signoff.get(key) instanceof String)) {
                throw exception(HRDOC_FIELDS_INVALID, "签收卡." + key + " 必须是字符串");
            }
        }
        checkSignoffStrLimit(signoff, "icon", MAX_COVER_ICON);
        checkSignoffStrLimit(signoff, "title", MAX_SIGNOFF_TITLE);
        checkSignoffStrLimit(signoff, "ack_label", MAX_SIGNOFF_ACK);
        checkSignoffStrLimit(signoff, "button_text", MAX_SIGNOFF_BUTTON);
        checkSignoffStrLimit(signoff, "signed_note", MAX_SIGNOFF_NOTE);
        for (String key : SIGNOFF_HTML_KEYS) {
            if (!signoff.containsKey(key)) {
                continue;
            }
            Object v = signoff.get(key);
            if (!(v instanceof String)) {
                throw exception(HRDOC_FIELDS_INVALID, "签收卡." + key + " 必须是字符串");
            }
            if (((String) v).getBytes(StandardCharsets.UTF_8).length > HrDocumentBlockValidator.MAX_HTML_BYTES) {
                throw exception(HRDOC_FIELDS_INVALID, "签收卡." + key + " 超长（上限 200KB）");
            }
        }
    }

    private static void checkSignoffStrLimit(Map<String, Object> signoff, String key, int limit) {
        if (!signoff.containsKey(key)) {
            return;
        }
        Object v = signoff.get(key);
        if (v instanceof String && ((String) v).length() > limit) {
            throw exception(HRDOC_FIELDS_INVALID, "签收卡." + key + " 超长（上限 " + limit + " 字）");
        }
    }

    public static Map<String, Object> normalizeSignoff(Map<String, Object> value) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("icon", "📝");
        out.put("title", "员工签收");
        out.put("lead", "");
        out.put("ack_label", "我确认已完整阅读并理解本文档全部内容");
        out.put("button_text", "✓ 我已阅读并同意（线上签收）");
        out.put("tip", "");
        out.put("signed_note", "签收记录已存档。本文档发布新版本时，您需要重新签收。");
        if (value != null) {
            out.putAll(value);
        }
        return out;
    }

    // ── 三态晋升 ──

    /** 通用三态晋升：draft 为 null = 这次没动过 → 沿用正式值；否则用 draft 值（空串/空数组也算改过）。 */
    public static <T> T promoted(T draftValue, T publishedValue) {
        return draftValue == null ? publishedValue : draftValue;
    }

    /** dept_scope 草稿三态：null（没动）/ []（全员，正式列 NULL）/ 数组（限定）。 */
    public static List<String> promotedDeptScope(List<String> draftDeptScope, List<String> publishedDeptScope) {
        if (draftDeptScope == null) {
            return publishedDeptScope;
        }
        if (draftDeptScope.isEmpty()) {
            return null;
        }
        return draftDeptScope;
    }

    // ── 发布 ──

    /** 发布目标版本号必填且 ≤16 字；已发布过且换号时要求数字新号不得倒退。 */
    public static String validatePublicationVersion(String target, String current, boolean publishedBefore) {
        if (target == null || target.isBlank() || target.trim().length() > 16) {
            throw exception(HRDOC_FIELDS_INVALID, "发布目标版本号必填，且最多 16 个字符");
        }
        String t = target.trim();
        String c = current == null ? "" : current.trim();
        if (publishedBefore && !t.equals(c)) {
            Pattern pattern = Pattern.compile("^[^\\d]*(\\d+(?:\\.\\d+)*).*$");
            Matcher oldM = pattern.matcher(c);
            Matcher newM = pattern.matcher(t);
            if (oldM.matches() && newM.matches()) {
                int[] before = parseVersion(oldM.group(1));
                int[] after = parseVersion(newM.group(1));
                int count = Math.max(before.length, after.length);
                for (int i = 0; i < count; i++) {
                    int b = i < before.length ? before[i] : 0;
                    int a = i < after.length ? after[i] : 0;
                    if (a < b) {
                        throw exception(HRDOC_FIELDS_INVALID, "新版本号须高于当前版本 " + c);
                    }
                    if (a > b) {
                        break;
                    }
                }
            }
        }
        return t;
    }

    private static int[] parseVersion(String digits) {
        String[] parts = digits.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i]);
        }
        return out;
    }

}
