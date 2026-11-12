package com.cyt.center.core.integration;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.*;
import java.util.*;

/** 花名册字段按企业的字段名及选项配置发现，绝不使用通讯录账号 status 推断任职。 */
public final class WecomHrDates {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Set<String> DATE_FIELDS = Set.of("birthday", "entryTime", "regularTime", "leaveTime");
    private static final List<String> CORE_FIELDS = List.of("birthday", "entryTime", "employeeStatus", "probation", "regularTime");

    private WecomHrDates() { }

    public record Field(int id, int valueType, int fieldType, Map<String, String> options) {
        public Field(int id, int valueType) { this(id, valueType, 3, Map.of()); }
        public Field { options = Collections.unmodifiableMap(new LinkedHashMap<>(options)); }
    }
    public record Issue(String field, String reason) { }
    public record Configuration(Map<String, Field> fields, List<Issue> issues) {
        public Configuration {
            fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
            issues = List.copyOf(issues);
        }
    }
    public record Profile(Map<String, Object> values, List<Issue> issues, int readableFields) {
        public Profile {
            // 明确空日期可进入补丁，Map.copyOf 不允许 null。
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            issues = List.copyOf(issues);
        }
    }

    public static Map<String, Field> fields(JsonNode response) { return discover(response).fields(); }

    public static Configuration discover(JsonNode response) {
        JsonNode groups = response.path("group_list");
        if (!groups.isArray()) throw invalid("花名册字段配置不完整");
        Map<String, Field> fields = new LinkedHashMap<>();
        Set<String> unavailable = new HashSet<>();
        Set<Integer> ambiguousIds = new HashSet<>();
        List<Issue> issues = new ArrayList<>();
        for (JsonNode group : groups) {
            if (!group.path("field_list").isArray()) throw invalid("花名册字段配置不完整");
            for (JsonNode item : group.path("field_list")) {
                String key = key(item.path("field_name").asText());
                if (key.isEmpty() || unavailable.contains(key)) continue;
                if (fields.containsKey(key)) {
                    fields.remove(key); unavailable.add(key);
                    issues.add(new Issue(key, "花名册存在同名字段，保留本地值"));
                    continue;
                }
                try {
                    Field field = field(key, item);
                    if (ambiguousIds.contains(field.id())) throw invalid("花名册字段编号重复");
                    String duplicate = fields.entrySet().stream().filter(existing -> existing.getValue().id() == field.id())
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (duplicate != null) {
                        ambiguousIds.add(field.id());
                        fields.remove(duplicate); unavailable.add(duplicate);
                        issues.add(new Issue(duplicate, "花名册字段编号重复，保留本地值"));
                        throw invalid("花名册字段编号重复");
                    }
                    fields.put(key, field);
                } catch (BizException error) {
                    unavailable.add(key);
                    issues.add(new Issue(key, error.getMessage() + "，保留本地值"));
                }
            }
        }
        for (String key : CORE_FIELDS)
            if (!fields.containsKey(key) && !unavailable.contains(key))
                issues.add(new Issue(key, "花名册未配置此字段，保留本地值"));
        return new Configuration(fields, issues);
    }

    private static Field field(String key, JsonNode item) {
        JsonNode id = item.has("fieldid") ? item.path("fieldid") : item.path("field_id");
        if (!id.isIntegralNumber() || !id.canConvertToInt() || id.asInt() <= 0
                || !item.path("field_type").isIntegralNumber() || !item.path("value_type").isIntegralNumber())
            throw invalid("花名册字段类型不完整");
        int fieldType = item.path("field_type").asInt(), valueType = item.path("value_type").asInt();
        boolean supported = DATE_FIELDS.contains(key)
                ? fieldType == 3 && Set.of(2, 4).contains(valueType) || fieldType == 1 && valueType == 1
                : fieldType == 2 && valueType == 3 || fieldType == 1 && valueType == 1;
        if (!supported) throw invalid("花名册字段类型不受支持");
        Map<String, String> options = new LinkedHashMap<>();
        if (fieldType == 2) {
            if (!item.path("option_list").isArray()) throw invalid("花名册未返回字段选项配置");
            for (JsonNode option : item.path("option_list")) {
                String optionId = option.path("id").asText();
                if (!optionId.matches("\\d+") || !option.path("value").isTextual()
                        || options.put(optionId, option.path("value").asText()) != null)
                    throw invalid("花名册字段选项配置异常");
            }
        }
        return new Field(id.asInt(), valueType, fieldType, options);
    }

    private static String key(String name) {
        return switch (normalize(name)) {
            case "生日", "出生日期" -> "birthday";
            case "入职日期", "入职时间" -> "entryTime";
            case "员工状态", "人事状态", "任职状态" -> "employeeStatus";
            case "试用期", "试用期(月)", "试用期月数" -> "probation";
            case "转正日期", "转正时间", "计划转正日期" -> "regularTime";
            case "离职日期", "离职时间" -> "leaveTime";
            default -> "";
        };
    }

    /** 每个字段单独校验；不可读、未知选项和异常值不进入补丁，其余字段仍可同步。 */
    public static Profile parseProfile(JsonNode response, Map<String, Field> fields, LocalDate today) {
        JsonNode items = response.path("field_info");
        if (!items.isArray()) throw invalid("花名册未返回字段资料");
        Map<Integer, List<JsonNode>> byId = new HashMap<>();
        for (JsonNode item : items) {
            if (!item.path("fieldid").isIntegralNumber() || !item.path("fieldid").canConvertToInt()) continue;
            if (item.has("sub_idx") && (!item.path("sub_idx").isIntegralNumber() || item.path("sub_idx").asInt() != 0)) continue;
            byId.computeIfAbsent(item.path("fieldid").asInt(), ignored -> new ArrayList<>()).add(item);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        List<Issue> issues = new ArrayList<>();
        int readable = 0;
        for (var entry : fields.entrySet()) {
            String key = entry.getKey(); Field field = entry.getValue();
            List<JsonNode> rows = byId.get(field.id());
            if (rows == null) continue;
            if (rows.size() != 1) { issues.add(new Issue(key, "花名册返回重复字段，保留本地值")); continue; }
            JsonNode item = rows.getFirst();
            if (!item.path("result").isIntegralNumber() || item.path("result").asInt() != 1) {
                issues.add(new Issue(key, item.path("result").asInt(-1) == 3
                        ? "花名册字段不存在，保留本地值" : "花名册字段不可读，保留本地值")); continue;
            }
            String valueKey = switch (field.valueType()) {
                case 1 -> "value_string"; case 2 -> "value_uint64";
                case 3 -> "value_uint32"; case 4 -> "value_int64";
                default -> "";
            };
            if (!item.has(valueKey)) continue;
            if (item.path("value_type").asInt(-1) != field.valueType()
                    || hasUnexpectedValue(item, valueKey)) {
                issues.add(new Issue(key, "花名册字段值类型不匹配，保留本地值")); continue;
            }
            try {
                if (DATE_FIELDS.contains(key)) values.put(key, parseDate(key, item.path(valueKey), field.valueType(), today));
                else {
                    String text = optionText(field, item.path(valueKey));
                    if (key.equals("employeeStatus")) values.putAll(employment(text));
                    else if (key.equals("probation")) values.put("probation", probation(text));
                    else throw invalid("不支持的花名册字段");
                }
                readable++;
            } catch (BizException error) { issues.add(new Issue(key, error.getMessage() + "，保留本地值")); }
        }
        return new Profile(values, issues, readable);
    }

    /** 旧日期调用保留严格失败语义，新的整个人事同步使用逐字段的 parseProfile。 */
    public static Map<String, LocalDate> parse(JsonNode response, Map<String, Field> fields, LocalDate today) {
        Profile profile = parseProfile(response, fields, today);
        profile.issues().stream().filter(issue -> !issue.reason().contains("字段不存在")).findFirst()
                .ifPresent(issue -> { throw invalid(issue.reason()); });
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        profile.values().forEach((key, value) -> { if (DATE_FIELDS.contains(key)) dates.put(key, (LocalDate) value); });
        return dates;
    }

    private static boolean hasUnexpectedValue(JsonNode item, String expected) {
        var names = item.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (name.startsWith("value_") && !name.equals("value_type") && !name.equals(expected)) return true;
        }
        return false;
    }

    private static LocalDate parseDate(String key, JsonNode value, int valueType, LocalDate today) {
        if (value.isNull() || value.isTextual() && value.asText().isBlank()) return null;
        try {
            LocalDate date;
            if (valueType == 1) {
                if (!value.isTextual() || !value.asText().matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) throw new IllegalArgumentException();
                String[] parts = value.asText().split("[-/]");
                date = LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } else {
                if (!value.isIntegralNumber() && !(value.isTextual() && value.asText().matches("-?\\d+"))) throw new IllegalArgumentException();
                long epoch = Long.parseLong(value.asText());
                if (valueType == 2 && epoch < 0) throw new IllegalArgumentException();
                date = Instant.ofEpochSecond(epoch).atZone(SHANGHAI).toLocalDate();
            }
            if (date.getYear() < 1000 || date.getYear() > 9999 || key.equals("birthday") && date.isAfter(today))
                throw new IllegalArgumentException();
            return date;
        } catch (RuntimeException error) { throw invalid(key.equals("birthday") ? "生日日期无效或晚于今天" : "花名册日期值无效"); }
    }

    private static String optionText(Field field, JsonNode value) {
        if (value.isNull()) throw invalid("花名册状态或试用期为空");
        if (field.fieldType() == 2) {
            if (!value.isIntegralNumber() && !(value.isTextual() && value.asText().matches("\\d+")))
                throw invalid("花名册选项值无效");
            String text = field.options().get(value.asText());
            if (text == null) throw invalid("花名册返回未配置的选项编号");
            return text;
        }
        if (!value.isTextual() || value.asText().isBlank()) throw invalid("花名册状态或试用期为空");
        return value.asText();
    }

    private static Map<String, Object> employment(String text) {
        return switch (normalize(text)) {
            case "正式", "正式员工", "已转正", "在职(正式)" -> Map.of("entryStatus", 1, "type", 1, "status", 1);
            case "试用", "试用期", "试用员工", "试用期员工", "在职(试用)" -> Map.of("entryStatus", 1, "type", 1, "status", 2);
            case "在职", "在职员工" -> Map.of("entryStatus", 1);
            case "待完善", "待完善信息" -> Map.of("entryStatus", 0);
            case "待入职", "待入职员工" -> Map.of("entryStatus", 2);
            case "待离职", "待离职员工" -> Map.of("entryStatus", 3);
            case "离职", "已离职", "离职员工", "已离职员工" -> Map.of("entryStatus", 4);
            case "放弃入职", "已放弃入职" -> Map.of("entryStatus", 5);
            default -> throw invalid("员工状态尚无明确映射");
        };
    }

    private static int probation(String text) {
        String value = normalize(text);
        if (Set.of("无", "无试用期", "无需试用", "不设试用期").contains(value)) return 0;
        value = value.replace("试用期", "").replace("个月", "").replace("月", "");
        value = switch (value) { case "零" -> "0"; case "一" -> "1"; case "二", "两" -> "2"; case "三" -> "3"; case "四" -> "4"; case "五" -> "5"; case "六" -> "6"; default -> value; };
        if (!value.matches("[0-6]")) throw invalid("试用期月数无法识别或超过现有支持范围");
        return Integer.parseInt(value);
    }

    private static String normalize(String value) { return value.trim().replaceAll("\\s+", "").replace('（', '(').replace('）', ')'); }
    private static BizException invalid(String message) { return new BizException("企微" + message); }
}
