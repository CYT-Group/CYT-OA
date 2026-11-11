package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/** JDBC 管理页的输出约定与 OA 其余接口一致，JSON 列返回对象而不是二次编码字符串。 */
public final class PortalRows {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final Set<String> JSON_COLUMNS =
            Set.of(
                    "variant_values",
                    "variant_dims",
                    "image_urls",
                    "current_borrowers",
                    "extracted",
                    "templates",
                    "template_defaults");

    private PortalRows() {}

    public static Map<String, Object> camel(Map<String, Object> raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        raw.forEach(
                (key, value) -> {
                    StringBuilder name = new StringBuilder();
                    boolean upper = false;
                    for (char ch : key.toCharArray()) {
                        if (ch == '_') {
                            upper = true;
                            continue;
                        }
                        name.append(upper ? Character.toUpperCase(ch) : ch);
                        upper = false;
                    }
                    Object v = value;
                    if (value instanceof String s && JSON_COLUMNS.contains(key)) {
                        try {
                            v = JSON.readValue(s, Object.class);
                        } catch (Exception ignored) {
                        }
                    }
                    out.put(name.toString(), v);
                });
        return out;
    }

    public static List<Map<String, Object>> camel(List<Map<String, Object>> rows) {
        return rows.stream().map(PortalRows::camel).toList();
    }

    public static String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException("JSON 格式错误");
        }
    }

    public static String text(Map<String, Object> row, String key, String fallback) {
        Object value = row.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public static long number(Object value) {
        try {
            return new java.math.BigDecimal(String.valueOf(value)).longValueExact();
        } catch (Exception e) {
            throw new BizException("编号或数量格式错误");
        }
    }
}
