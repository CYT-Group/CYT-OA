package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentAliasMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortalHrAdminAliasTest {
    @Test void oldAliasResolvesEvenWhenDocumentIsUnpublished(){
        var alias=mock(PortalHrDocumentAliasMapper.class);var documents=mock(PortalHrDocumentMapper.class);var service=new PortalHrDocumentServiceImpl();
        ReflectionTestUtils.setField(service,"aliasMapper",alias);ReflectionTestUtils.setField(service,"documentMapper",documents);
        var draft=new PortalHrDocumentDO();draft.setId(7L);draft.setSlug("new-slug");draft.setStatus("draft");
        when(alias.selectDocumentIdByAlias("old-slug")).thenReturn(7L);when(documents.selectById(7L)).thenReturn(draft);
        assertEquals(7L,service.resolveAdminSlug("old-slug"));verify(documents,never()).selectBySlug(anyString());
    }
    @Test void missingAliasFallsBackToCurrentSlugAndUnknownFails(){
        var alias=mock(PortalHrDocumentAliasMapper.class);var documents=mock(PortalHrDocumentMapper.class);var service=new PortalHrDocumentServiceImpl();
        ReflectionTestUtils.setField(service,"aliasMapper",alias);ReflectionTestUtils.setField(service,"documentMapper",documents);
        when(alias.selectDocumentIdByAlias(anyString())).thenReturn(null);
        var doc=new PortalHrDocumentDO();doc.setId(8L);when(documents.selectBySlug("current")).thenReturn(doc);
        assertEquals(8L,service.resolveAdminSlug("current"));assertThrows(RuntimeException.class,()->service.resolveAdminSlug("missing"));
    }
}
