package com.cyt.center.module.portal.service.contracts;

import com.cyt.center.core.exception.BizException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** 原 Portal DOCX 模板原样保留；只替换其 23 个冻结变量与职责循环，保留法律正文和排版。 */
@Component
public class PortalContractTemplate {
    public byte[] render(Map<String,String> values,List<String> duties) {
        try(InputStream in=new ClassPathResource("templates/contracts/labor_contract_v1.docx").getInputStream();
            ZipInputStream zip=new ZipInputStream(in);ByteArrayOutputStream bytes=new ByteArrayOutputStream();ZipOutputStream out=new ZipOutputStream(bytes)) {
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;) {
                byte[] content=zip.readAllBytes();
                if("word/document.xml".equals(entry.getName())) content=renderXml(new String(content,StandardCharsets.UTF_8),values,duties).getBytes(StandardCharsets.UTF_8);
                out.putNextEntry(new ZipEntry(entry.getName()));out.write(content);out.closeEntry();
            }
            out.finish(); return bytes.toByteArray();
        } catch(Exception ex) { throw new BizException("劳动合同模板渲染失败，请检查服务器模板文件"); }
    }

    String renderXml(String xml,Map<String,String> values,List<String> duties) {
        Pattern paragraph=Pattern.compile("<w:p(?:\\s[^>]*)?>.*?</w:p>",Pattern.DOTALL);
        Matcher m=paragraph.matcher(xml);
        String start=null,body=null,end=null;
        while(m.find()) {
            String p=m.group();
            if(p.contains("{%p for duty in duties %}")) start=p;
            else if(p.contains("{{ duty }}")) body=p;
            else if(p.contains("{%p endfor %}")) end=p;
        }
        if(start==null || body==null || end==null) throw new IllegalStateException("职责模板不完整");
        int first=xml.indexOf(start),last=xml.indexOf(end)+end.length();
        if(first<0||last<=first)throw new IllegalStateException("职责模板顺序错误");
        StringBuilder rendered=new StringBuilder(variables(xml.substring(0,first),values));
        for(int i=0;i<duties.size();i++) {
            Map<String,String> row=new java.util.LinkedHashMap<>(values);
            row.put("loop.index",Integer.toString(i+1));row.put("duty",duties.get(i));
            rendered.append(variables(body,row));
        }
        rendered.append(variables(xml.substring(last),values));return rendered.toString();
    }

    /** 单次扫描源模板，不会把用户文本中的 {{...}} 当作第二轮模板求值。 */
    private String variables(String source,Map<String,String> values) {
        Matcher tokens=Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}").matcher(source);StringBuffer out=new StringBuffer();
        while(tokens.find()) {String key=tokens.group(1).trim();if(!values.containsKey(key))throw new IllegalStateException("未知模板变量");tokens.appendReplacement(out,Matcher.quoteReplacement(escape(values.get(key))));}
        tokens.appendTail(out);return out.toString();
    }

    private String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&apos;").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","");
    }
}
