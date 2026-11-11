package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Node;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** Word 导入只读正文与表格，不执行宏、不解析外链、不将上传原件公开。 */
@Component
public class PortalHrLayoutDocumentReader {
    static final int MAX_BYTES=10*1024*1024;
    private static final String WORD_NS="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    public record Input(String name,String text,String mime,byte[] binary) {}
    public Input read(MultipartFile file,boolean asynchronous){
        if(file==null||file.isEmpty())throw new BizException("请上传非空文件");
        if(file.getSize()>MAX_BYTES)throw new BizException("文件不能超过 10MB");
        String name=Objects.toString(file.getOriginalFilename(),"upload").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1);if(name.length()>255)name=name.substring(name.length()-255);
        String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
        byte[] bytes;try(var input=file.getInputStream()){bytes=input.readNBytes(MAX_BYTES+1);}catch(IOException e){throw new BizException("无法读取上传文件");}
        if(bytes.length>MAX_BYTES)throw new BizException("文件不能超过 10MB");
        if(ext.equals("docx"))return new Input(name,checkText(docx(bytes)),null,null);
        if(Set.of("txt","md","csv").contains(ext))return new Input(name,checkText(decode(bytes)),null,null);
        if(asynchronous)throw new BizException("异步任务只支持 docx / txt / md / csv（PDF 请先转成 docx）");
        String mime=switch(ext){
            case "pdf" -> starts(bytes,new byte[]{'%','P','D','F','-'})?"application/pdf":null;
            case "png" -> starts(bytes,new byte[]{(byte)137,80,78,71,13,10,26,10})?"image/png":null;
            case "jpg","jpeg" -> starts(bytes,new byte[]{(byte)255,(byte)216,(byte)255})?"image/jpeg":null;
            case "webp" -> bytes.length>12&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP")?"image/webp":null;
            default -> null;
        };
        if(mime==null)throw new BizException("文件格式与内容不匹配，支持 docx / txt / md / csv / PDF / PNG / JPEG / WebP");
        return new Input(name,"",mime,bytes);
    }
    public static String checkText(String text){String clean=Objects.toString(text,"").replace("\uFEFF","").trim();if(clean.isEmpty())throw new BizException("没有提取到正文");if(clean.length()>20000)throw new BizException("原文最多 20000 字，请分批排版");if(clean.indexOf('\0')>=0)throw new BizException("文件不是有效文本");return clean;}
    private String decode(byte[] bytes){for(String encoding:List.of("UTF-8","GB18030")){try{return Charset.forName(encoding).newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}catch(CharacterCodingException ignored){}}throw new BizException("文本编码无法识别，请另存为 UTF-8");}
    String docx(byte[] bytes){
        if(!starts(bytes,new byte[]{'P','K',3,4}))throw new BizException("不是有效的 docx 文件");
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))){
            byte[] document=null;ZipEntry entry;int count=0;
            int skipped=0;byte[] buffer=new byte[8192];
            while((entry=zip.getNextEntry())!=null){
                if(++count>5000)throw new BizException("Word 压缩包条目过多");
                if("word/document.xml".equals(entry.getName())){document=zip.readNBytes(8*1024*1024+1);if(document.length>8*1024*1024)throw new BizException("Word 正文解压后过大");break;}
                int n;while((n=zip.read(buffer))!=-1){skipped+=n;if(skipped>20*1024*1024)throw new BizException("Word 解压内容过大");}
            }
            if(document==null)throw new BizException("Word 文件缺少正文");
            var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
            var builder=factory.newDocumentBuilder();builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler(){@Override public void error(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}@Override public void fatalError(org.xml.sax.SAXParseException e)throws org.xml.sax.SAXException{throw e;}});
            var xml=builder.parse(new ByteArrayInputStream(document));
            var bodies=xml.getElementsByTagNameNS(WORD_NS,"body");if(bodies.getLength()==0)throw new BizException("Word 文件缺少正文");
            List<String> lines=new ArrayList<>();Node body=bodies.item(0);
            for(Node node=body.getFirstChild();node!=null;node=node.getNextSibling()){
                if("p".equals(node.getLocalName()))lines.add(text(node).trim());
                else if("tbl".equals(node.getLocalName()))for(Node row=node.getFirstChild();row!=null;row=row.getNextSibling())if("tr".equals(row.getLocalName())){List<String> cells=new ArrayList<>();for(Node cell=row.getFirstChild();cell!=null;cell=cell.getNextSibling())if("tc".equals(cell.getLocalName()))cells.add(text(cell).trim());lines.add(String.join("\t",cells));}
            }
            return String.join("\n",lines);
        }catch(BizException e){throw e;}catch(Exception e){throw new BizException("Word 文件无法解析，请重新另存为 docx");}
    }
    private String text(Node node){if(WORD_NS.equals(node.getNamespaceURI())&&"t".equals(node.getLocalName()))return node.getTextContent();if("tab".equals(node.getLocalName()))return "\t";if("br".equals(node.getLocalName()))return "\n";StringBuilder value=new StringBuilder();for(Node child=node.getFirstChild();child!=null;child=child.getNextSibling())value.append(text(child));if("p".equals(node.getLocalName()))value.append('\n');return value.toString();}
    private boolean starts(byte[] value,byte[] prefix){if(value.length<prefix.length)return false;for(int i=0;i<prefix.length;i++)if(value[i]!=prefix[i])return false;return true;}
}
