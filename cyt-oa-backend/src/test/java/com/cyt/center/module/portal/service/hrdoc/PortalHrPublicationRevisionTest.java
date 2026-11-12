package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PortalHrPublicationRevisionTest {
    final PortalHrDocumentServiceImpl service=new PortalHrDocumentServiceImpl();
    @Test void savedRevisionSurvivesNestedJsonKeyReordering(){
        var before=doc();var after=doc();
        before.setDraftCover(ordered("bg_type","gradient","desc","封面"));
        after.setDraftCover(ordered("desc","封面","bg_type","gradient"));
        before.setDraftBlocks(List.of(ordered("type","cards","columns",2,"items",List.of(ordered("title","机票","text","行政安排","icon","")))));
        after.setDraftBlocks(List.of(ordered("items",List.of(ordered("icon","","text","行政安排","title","机票")),"columns",2,"type","cards")));
        assertEquals(service.describe(before).revision(),service.describe(after).revision());
        after.setDraftTitle("另一个人改稿");assertNotEquals(service.describe(before).revision(),service.describe(after).revision());
    }
    @Test void writeResponseAndDatabaseSecondPrecisionProduceSameRevision(){
        var before=doc();var after=doc();before.setPublishedAt(LocalDateTime.of(2026,11,10,15,30,1,123456789));after.setPublishedAt(LocalDateTime.of(2026,11,10,15,30,1));
        assertEquals(service.describe(before).revision(),service.describe(after).revision());
    }
    PortalHrDocumentDO doc(){var row=new PortalHrDocumentDO();row.setId(7L);row.setVersion("v1.0");row.setTitle("制度");row.setDraftTitle("制度");row.setRequiresSignoff(false);row.setContentHtml("");return row;}
    Map<String,Object> ordered(Object... entries){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<entries.length;i+=2)result.put(entries[i].toString(),entries[i+1]);return result;}
}
