package com.cyt.center.module.hrm.service.workspace;

import com.cyt.center.core.exception.BizException;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 纯文本变量替换，模板不能执行表达式；只扫描一次，用户值不作为模板再次求值。 */
@Component
public class HrmDocumentRenderer {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([a-zA-Z]+)\\s*}}");
    public static final Set<String> KEYS = Set.of("name", "jobNumber", "department", "postName", "entryDate",
            "probation", "salary", "company", "companyCode", "companyAddress", "signDate", "endDate",
            "leaveDate", "leaveType", "reason");

    public void validate(String content) {
        Matcher m = TOKEN.matcher(content);
        while (m.find()) if (!KEYS.contains(m.group(1))) throw new BizException("未知模板变量：" + m.group(1));
        if (TOKEN.matcher(content).replaceAll("").contains("{{")) throw new BizException("模板变量格式错误");
    }

    public String render(String content, Map<String, String> values) {
        validate(content);
        Matcher m = TOKEN.matcher(content);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String value = values.get(m.group(1));
            if (value == null || value.isBlank()) throw new BizException("请补充模板所需信息：" + m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}
