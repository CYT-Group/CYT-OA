package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class PortalHrLayoutDocumentReaderTest {
    final PortalHrLayoutDocumentReader reader=new PortalHrLayoutDocumentReader();
    @Test void wordKeepsBodyTableOrderAndRunSpaces()throws Exception{
        String xml="""
          <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
          <w:p><w:r><w:t xml:space="preserve">Hello </w:t></w:r><w:r><w:t>world</w:t></w:r></w:p>
          <w:tbl><w:tr><w:tc><w:p><w:r><w:t>机票</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>行政安排</w:t></w:r></w:p></w:tc></w:tr></w:tbl>
          <w:p><w:r><w:t>结束</w:t></w:r></w:p></w:body></w:document>
          """;
        var input=reader.read(new MockMultipartFile("file","rules.docx","application/octet-stream",word(xml)),true);
        assertEquals("Hello world\n机票\t行政安排\n结束",input.text());assertNull(input.binary());
    }
    @Test void rejectsExternalEntitiesAndExpandedXmlBomb()throws Exception{
        String entity="<?xml version='1.0'?><!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///does-not-exist'>]><w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:p><w:r><w:t>&secret;</w:t></w:r></w:p></w:body></w:document>";
        assertThrows(BizException.class,()->reader.docx(word(entity)));
        assertThrows(BizException.class,()->reader.docx(word(" ".repeat(8*1024*1024+1))));
    }
    @Test void validatesAttachmentContentAndNeverQueuesPdfBinary(){
        var fake=new MockMultipartFile("file","rules.pdf","application/pdf","actually text".getBytes(StandardCharsets.UTF_8));
        assertThrows(BizException.class,()->reader.read(fake,false));
        var pdf=new MockMultipartFile("file","rules.pdf","application/pdf","%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
        assertEquals("application/pdf",reader.read(pdf,false).mime());assertThrows(BizException.class,()->reader.read(pdf,true));
    }
    @Test void handlesUtf8BomAndChineseTextWithoutSilentlyTruncating(){
        var file=new MockMultipartFile("file","rules.txt","text/plain","\uFEFF制度条款".getBytes(StandardCharsets.UTF_8));
        assertEquals("制度条款",reader.read(file,true).text());
        assertThrows(BizException.class,()->PortalHrLayoutDocumentReader.checkText("字".repeat(20001)));
    }
    private byte[] word(String xml)throws IOException{var output=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(output)){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write(xml.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}return output.toByteArray();}
}
