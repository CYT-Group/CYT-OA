package com.cyt.center.module.portal.service.resumes;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PortalResumeRulesTest {
    private final PortalResumeService service=new PortalResumeService(null,null,null,null,null,new ObjectMapper());
    @Test void normalizesColumnsAndPreservesStructuredData() {
        var input=new LinkedHashMap<String,Object>(Map.of("is_resume",true,"name"," 张三 ","phone","+86 138-1234-5678","email"," HR@EXAMPLE.COM ","years_of_experience",5.26));
        var out=service.normalize(input);
        assertEquals("张三",out.get("name"));assertEquals("13812345678",out.get("phone"));assertEquals("hr@example.com",out.get("email"));assertEquals("5.3",out.get("years_of_experience").toString());
        assertNotNull(out.get("education"));assertNotNull(out.get("work_history"));
    }
    @Test void rejectsUnexpectedSensitiveFieldsAndInvalidDates() {
        assertThrows(BizException.class,()->service.normalize(Map.of("is_resume",true,"id_number","123")));
        assertThrows(BizException.class,()->service.normalize(Map.of("is_resume",true,"birth_date","2026-02-30")));
        assertThrows(BizException.class,()->service.normalize(Map.of("is_resume",true,"years_of_experience",-1)));
    }
    @Test void rejectsExtensionMismatchAndFakeWordZip() {
        assertThrows(BizException.class,()->service.validateFile("resume.pdf",new byte[]{80,75,3,4}));
        assertThrows(BizException.class,()->service.validateFile("resume.docx",new byte[]{80,75,3,4}));
        assertThrows(BizException.class,()->service.validateFile("resume.doc",new byte[]{1}));
        assertEquals("pdf",service.validateFile("resume.PDF","%PDF-1.7\n".getBytes()));
    }
}
