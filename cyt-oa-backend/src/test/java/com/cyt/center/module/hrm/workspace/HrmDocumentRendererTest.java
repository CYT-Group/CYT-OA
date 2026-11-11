package com.cyt.center.module.hrm.workspace;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.hrm.service.workspace.HrmDocumentRenderer;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HrmDocumentRendererTest {
    private final HrmDocumentRenderer renderer=new HrmDocumentRenderer();
    @Test void replacesRepeatedVariablesAndPreservesUserTextLiterally() {
        assertEquals("{{salary}} / $10\\name / {{salary}}", renderer.render("{{name}} / {{reason}} / {{ name }}",
                Map.of("name","{{salary}}","reason","$10\\name")));
    }
    @Test void rejectsUnknownMalformedAndMissingVariables() {
        assertThrows(BizException.class,()->renderer.validate("{{system}}"));
        assertThrows(BizException.class,()->renderer.validate("{{name"));
        assertThrows(BizException.class,()->renderer.validate("{{ name.toString() }}"));
        assertThrows(BizException.class,()->renderer.render("{{company}}",Map.of()));
        assertThrows(BizException.class,()->renderer.render("{{name}}",Map.of("name","   ")));
    }
    @Test void preservesChineseClausesAndLineBreaks() {
        assertEquals("劳动合同\n甲方：测试公司\n乙方：张三",renderer.render("劳动合同\n甲方：{{company}}\n乙方：{{name}}",Map.of("company","测试公司","name","张三")));
    }
}
