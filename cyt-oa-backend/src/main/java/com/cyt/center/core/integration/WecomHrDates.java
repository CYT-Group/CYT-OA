package com.cyt.center.core.integration;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** 与 Portal 的 employee_sync 口径一致：按名称发现字段，秒值按北京时间转日期。 */
public final class WecomHrDates {
    private WecomHrDates() { }
    public record Field(int id, int valueType) { }
    public static Map<String, Field> fields(JsonNode response) {
        JsonNode groups = response.path("group_list");
        if (!groups.isArray()) throw invalid("花名册字段配置不完整");
        Map<String, Field> result = new LinkedHashMap<>();
        for (JsonNode group : groups) {
            if (!group.path("field_list").isArray()) throw invalid("花名册字段配置不完整");
            for (JsonNode item : group.path("field_list")) {
                String key = switch (item.path("field_name").asText()) {
                    case "生日", "出生日期" -> "birthday";
                    case "入职日期" -> "entryTime";
                    default -> "";
                };
                if (key.isEmpty()) continue;
                if (!item.path("fieldid").isIntegralNumber() || !item.path("fieldid").canConvertToInt()
                        || item.path("fieldid").asInt() <= 0 || !item.path("field_type").isIntegralNumber()
                        || item.path("field_type").asInt(-1) != 3 || !item.path("value_type").isIntegralNumber()
                        || !Set.of(2, 4).contains(item.path("value_type").asInt(-1))) {
                    throw invalid("花名册日期字段类型不受支持");
                }
                if (result.put(key, new Field(item.path("fieldid").asInt(), item.path("value_type").asInt())) != null)
                    throw invalid("花名册生日或入职日期字段重名");
            }
        }
        if (result.size() != 2 || result.get("birthday").id() == result.get("entryTime").id())
            throw invalid("花名册生日或入职日期字段缺失或冲突");
        return result;
    }

    public static Map<String, LocalDate> parse(JsonNode response, Map<String, Field> fields, LocalDate today) {
        JsonNode values = response.path("field_info");
        if (!values.isArray()) throw invalid("花名册未返回字段资料");
        Set<String> seen = new HashSet<>();
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        for (JsonNode item : values) {
            if (!item.path("fieldid").isIntegralNumber() || !item.path("sub_idx").isMissingNode()
                    && !item.path("sub_idx").isIntegralNumber()) throw invalid("花名册字段格式异常");
            if (!seen.add(item.path("fieldid").asText() + ":" + item.path("sub_idx").asInt(0)))
                throw invalid("花名册返回了重复字段");
            for (var entry : fields.entrySet()) {
                Field field = entry.getValue();
                if (item.path("fieldid").asInt() != field.id() || item.path("sub_idx").asInt(0) != 0) continue;
                if (!item.path("result").isIntegralNumber()) throw invalid("花名册字段状态不合法");
                int status = item.path("result").asInt(-1);
                if (status == 3) continue;
                if (status != 1) throw invalid("花名册日期读取失败，请检查字段权限");
                String valueKey = field.valueType() == 2 ? "value_uint64" : "value_int64";
                List<String> valueKeys = new ArrayList<>();
                item.fieldNames().forEachRemaining(k -> { if (k.startsWith("value_") && !k.equals("value_type")) valueKeys.add(k); });
                if (valueKeys.isEmpty()) continue;
                if (item.path("value_type").asInt(-1) != field.valueType()
                        || valueKeys.stream().anyMatch(k -> !k.equals(valueKey))) throw invalid("花名册日期值类型异常");
                JsonNode value = item.path(valueKey);
                if (value.isNull() || value.isMissingNode() || value.isTextual() && value.asText().isEmpty()) {
                    dates.put(entry.getKey(), null);
                    continue;
                }
                if (!value.isIntegralNumber() && !(value.isTextual() && value.asText().matches("-?\\d+")))
                    throw invalid("花名册日期值不合法");
                try {
                    long epoch = Long.parseLong(value.asText());
                    if (field.valueType() == 2 && epoch < 0) throw new IllegalArgumentException();
                    LocalDate date = Instant.ofEpochSecond(epoch).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
                    if (date.getYear() < 1000 || date.isAfter(today)) throw new IllegalArgumentException();
                    dates.put(entry.getKey(), date);
                } catch (RuntimeException error) { throw invalid("花名册日期值不合法或晚于今天"); }
            }
        }
        return dates;
    }
    private static BizException invalid(String message) { return new BizException("企微" + message); }
}
