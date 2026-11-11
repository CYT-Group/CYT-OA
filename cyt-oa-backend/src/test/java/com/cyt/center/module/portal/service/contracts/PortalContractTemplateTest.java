package com.cyt.center.module.portal.service.contracts;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.jupiter.api.Assertions.*;

class PortalContractTemplateTest {
    @Test void rendersOriginalTemplateWithEscapedValuesAndRepeatedDuties() throws Exception {
        var values=new LinkedHashMap<String,String>();
        for(String key:List.of("sign_date_cn","company_name","company_address","legal_rep","employee_name","id_number","phone","home_address","emergency_contact","start_date_cn","end_date_cn","probation_start_cn","probation_end_cn","position_title","report_to","work_city","work_schedule","salary_probation_base","salary_probation_allowance","salary_regular_base","salary_regular_allowance","salary_performance"))values.put(key,"测试<&>"+key);
        byte[] result=new PortalContractTemplate().render(values,List.of("设计 <UI> & 评审","维护数据"));
        String xml=null;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(result))){for(var e=zip.getNextEntry();e!=null;e=zip.getNextEntry())if(e.getName().equals("word/document.xml"))xml=new String(zip.readAllBytes(),StandardCharsets.UTF_8);}
        assertNotNull(xml);assertFalse(xml.contains("{{"));assertFalse(xml.contains("{%"));
        var parsed=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String text=parsed.getDocumentElement().getTextContent();
        assertTrue(text.contains("设计 <UI> & 评审"));assertTrue(text.contains("维护数据"));assertTrue(text.contains("知识产权"));
        assertTrue(text.contains("测试<&>employee_name"));
        // 旧模板法律正文与协议均保留，不得缩成只有表单字段的文档。
        assertTrue(text.length()>6000);
        values.put("employee_name","测试{{ phone }}");
        byte[] literal=new PortalContractTemplate().render(values,List.of("维护 {{ employee_name }} 模板"));
        try(var zip=new ZipInputStream(new ByteArrayInputStream(literal))){for(var e=zip.getNextEntry();e!=null;e=zip.getNextEntry())if(e.getName().equals("word/document.xml")){String source=new String(zip.readAllBytes(),StandardCharsets.UTF_8);assertTrue(source.contains("测试{{ phone }}"));assertTrue(source.contains("维护 {{ employee_name }} 模板"));}}
    }
}
