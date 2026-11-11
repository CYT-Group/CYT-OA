package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.module.portal.domain.hrdoc.HrDocumentBlockValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PortalHrLayoutNormalizerTest {
    private final PortalHrLayoutNormalizer normalizer=new PortalHrLayoutNormalizer(new ObjectMapper());
    @Test void fillsOnlyDeclaredFieldsAndNestedDefaults(){
        var result=normalizer.normalize("""
          {"blocks":[{"type":"cards","title":null,"tabs":[],"items":[{"title":"机票","text":"行政安排","unused":true}]},
            {"type":"tabcard","tabs":[{"name":"费用","metrics":[{"value":"120"}]}]}],"summary":"按费用分类"}
          ""","");
        HrDocumentBlockValidator.validate(result.blocks());
        Map<?,?> cards=(Map<?,?>)result.blocks().getFirst();assertFalse(cards.containsKey("tabs"));assertEquals(2,cards.get("columns"));
        Map<?,?> item=(Map<?,?>)((List<?>)cards.get("items")).getFirst();assertEquals("",item.get("icon"));assertFalse(item.containsKey("unused"));
        Map<?,?> tab=(Map<?,?>)((List<?>)((Map<?,?>)result.blocks().get(1)).get("tabs")).getFirst();assertTrue(tab.containsKey("subHeader"));
    }
    @Test void preservesOmittedConditionsAsEscapedOriginalText(){
        String source="员工需要在出差前提交申请\n特殊情况必须经分管领导审批<script>alert(1)</script>";
        var result=normalizer.normalize("{\"blocks\":[{\"type\":\"text\",\"text\":\"<p>员工需要在出差前提交申请</p>\"}]}",source);
        assertEquals(1,result.preservedLines());String finalText=((Map<?,?>)result.blocks().getLast()).get("text").toString();
        assertTrue(finalText.contains("特殊情况必须经分管领导审批"));assertFalse(finalText.contains("<script>"));assertTrue(finalText.contains("&lt;script&gt;"));
    }
    @Test void retainsChapterNumberInRendererTitleAndSanitizesHtml(){
        var result=normalizer.normalize("{\"blocks\":[{\"type\":\"numsec\",\"num\":\"第一章\",\"title\":\"总则\",\"text\":\"<p onclick='evil()'>内容</p><script>evil()</script>\"}]}","");
        Map<?,?> section=(Map<?,?>)result.blocks().getFirst();assertEquals("第一章 总则",section.get("title"));assertEquals("",section.get("num"));
        assertFalse(section.get("text").toString().contains("onclick"));assertFalse(section.get("text").toString().contains("<script"));
    }
    @Test void unknownBlockOrEmptyResultFailsInsteadOfInventingSuccess(){
        assertThrows(BizException.class,()->normalizer.normalize("{\"blocks\":[{\"type\":\"invented\"}]}","原文"));
        assertThrows(BizException.class,()->normalizer.normalize("{\"blocks\":[{\"type\":\"text\",\"text\":\"\"}]}","原文"));
    }
    @Test void boundedSegmentationPreservesLongLinesAndChapterOrder(){
        String original="第一章 总则\n"+"甲".repeat(4000)+"\n第二章 审批\n"+"乙".repeat(1600);
        var segments=PortalHrLayoutService.segments(original);
        assertTrue(segments.size()>2);assertTrue(segments.stream().allMatch(s->s.length()<=1800));
        assertEquals(original.replaceAll("\\s", ""),String.join("",segments).replaceAll("\\s", ""));
    }
}
