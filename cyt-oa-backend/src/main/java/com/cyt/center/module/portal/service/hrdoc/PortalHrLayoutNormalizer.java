package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.domain.hrdoc.HrDocumentBlockValidator;
import com.cyt.center.module.portal.service.support.PortalHtmlSanitizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/** AI 输出只在此补齐兼容字段；人工保存仍执行严格的文档块校验。 */
@Component
@RequiredArgsConstructor
public class PortalHrLayoutNormalizer {
    private final ObjectMapper mapper;
    public record Result(List<Object> blocks,String summary,int preservedLines) {}

    public Result normalize(String response,String source) {
        Map<String,Object> payload;
        try {
            String text=response.trim();
            if(text.startsWith("```")){int newline=text.indexOf('\n');text=text.substring(newline+1);int end=text.lastIndexOf("```");if(end>=0)text=text.substring(0,end);}
            payload=mapper.readValue(text,new com.fasterxml.jackson.core.type.TypeReference<>(){});
        }catch(Exception e){throw new BizException("排版结果不是有效 JSON，请重试");}
        if(!(payload.get("blocks") instanceof List<?> raw))throw new BizException("排版结果缺少 blocks");
        List<Object> blocks=new ArrayList<>();
        for(Object value:raw){
            if(!(value instanceof Map<?,?> block))throw new BizException("排版块不是对象");
            String type=Objects.toString(block.get("type"),"");
            var fields=HrDocumentBlockValidator.blockFieldSpec(type);
            if(fields.isEmpty())throw new BizException("排版返回未知块类型："+type);
            Map<String,Object> normalized=fill(block,fields,type);
            normalized.put("type",type);
            if(type.equals("numsec")){
                String num=Objects.toString(normalized.get("num"),"").trim();
                String title=Objects.toString(normalized.get("title"),"").trim();
                if(title.isEmpty()&&!num.isEmpty())for(String line:source.split("\\R")){String candidate=line.trim();if(candidate.startsWith(num)){title=candidate;break;}}
                if(!num.isEmpty()&&!title.startsWith(num))title=num+" "+title;
                normalized.put("title",title);normalized.put("num","");
            }
            if(Set.of("text","callout","quote","alert").contains(type)&&empty(normalized.get("text"))&&empty(normalized.get("title")))continue;
            if(Set.of("section","numsec").contains(type)&&empty(normalized.get("title")))continue;
            blocks.add(normalized);
        }
        if(blocks.isEmpty())throw new BizException("排版未生成有效内容");
        int preserved=0;
        if(source!=null&&source.length()<=2500){
            String output;
            try{output=mapper.writeValueAsString(blocks);}catch(Exception e){throw new IllegalStateException(e);}
            List<String> missing=new ArrayList<>();
            for(String rawLine:source.split("\\R")){
                String line=rawLine.trim();if(line.length()<8)continue;
                boolean found=output.contains(line.substring(0,Math.min(10,line.length())));
                if(!found&&line.length()>=16){int middle=(line.length()-10)/2;found=output.contains(line.substring(middle,middle+10))||output.contains(line.substring(line.length()-10));}
                if(!found)missing.add(line);
            }
            if(!missing.isEmpty()){blocks.add(fallback(String.join("\n",missing)));preserved=missing.size();}
        }
        HrDocumentBlockValidator.validate(blocks);
        blocks=HrDocumentBlockValidator.sanitize(blocks,PortalHtmlSanitizer::sanitize);
        String summary=Objects.toString(payload.get("summary"),"");
        if(preserved>0)summary+=(summary.isBlank()?"":"；")+"已保留模型遗漏的 "+preserved+" 行原文，请复核";
        return new Result(blocks,summary,preserved);
    }

    private Map<String,Object> fill(Map<?,?> raw,Map<String,String> fields,String type){
        Map<String,Object> result=new LinkedHashMap<>();
        for(var entry:fields.entrySet()){
            String key=entry.getKey(),kind=entry.getValue();Object value=raw.get(key);
            if(type.equals("docref")&&Set.of("slug","document_id").contains(key)&&(value==null||value.toString().isBlank()))continue;
            if(type.equals("quote")&&Set.of("color","bg").contains(key)&&value==null)continue;
            Object normalized=switch(kind){
                case "items" -> items(value,type.equals("tabcard")?"metrics":type);
                case "side" -> fill(asMap(value),HrDocumentBlockValidator.sideFieldSpec(),type);
                case "tabs" -> {
                    List<Object> tabs=new ArrayList<>();if(value instanceof List<?> values)for(Object item:values)tabs.add(fill(asMap(item),HrDocumentBlockValidator.tabFieldSpec(),"tabcard"));yield tabs;
                }
                case "list" -> value==null?List.of():value;
                case "bool" -> value==null?false:value;
                case "columns" -> value==null?2:value;
                case "tabindex" -> value==null?0:value;
                case "color","themecolor" -> value==null?"#6366F1":value;
                case "variant","alertvariant" -> value==null?"info":value;
                case "status" -> value==null?"pass":value;
                default -> value==null?"":value;
            };
            result.put(key,normalized);
        }
        return result;
    }
    private List<Object> items(Object value,String type){List<Object> items=new ArrayList<>();if(value instanceof List<?> values)for(Object item:values)items.add(fill(asMap(item),HrDocumentBlockValidator.itemFieldSpec(type),type));return items;}
    private Map<?,?> asMap(Object value){return value instanceof Map<?,?> map?map:Map.of();}
    private boolean empty(Object value){return value==null||value.toString().isBlank();}
    public static Map<String,Object> fallback(String source){return Map.of("type","text","text","<p>"+escape(source).replace("\n","</p><p>")+"</p>");}
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
