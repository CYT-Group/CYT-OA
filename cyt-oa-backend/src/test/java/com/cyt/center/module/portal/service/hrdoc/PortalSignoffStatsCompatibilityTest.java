package com.cyt.center.module.portal.service.hrdoc;

import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentDO;
import com.cyt.center.module.portal.dal.dataobject.hrdoc.PortalHrDocumentAliasDO;
import com.cyt.center.module.portal.dal.dataobject.signoff.PortalHandbookSignoffDO;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentAliasMapper;
import com.cyt.center.module.portal.dal.postgresql.hrdoc.PortalHrDocumentMapper;
import com.cyt.center.module.portal.dal.postgresql.signoff.PortalHandbookSignoffMapper;
import com.cyt.center.module.portal.dal.postgresql.signoff.PortalSignoffTargetRepository;
import com.cyt.center.module.portal.domain.signoff.PortalSignoffTargetUser;
import com.cyt.center.module.portal.service.signoff.PortalHandbookSignoffServiceImpl;
import com.cyt.center.framework.common.exception.ServiceException;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_UNKNOWN_DOC;
import static com.cyt.center.module.portal.enums.ErrorCodeConstants.SIGNOFF_NOT_SIGNABLE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortalSignoffStatsCompatibilityTest {
    final PortalHrDocumentMapper documents=mock(PortalHrDocumentMapper.class);
    final PortalHrDocumentAliasMapper aliases=mock(PortalHrDocumentAliasMapper.class);
    final PortalHandbookSignoffMapper signoffs=mock(PortalHandbookSignoffMapper.class);
    final PortalSignoffTargetRepository people=mock(PortalSignoffTargetRepository.class);
    final PortalHandbookSignoffServiceImpl service=new PortalHandbookSignoffServiceImpl();
    final List<PortalHandbookSignoffDO> ledger=new ArrayList<>();
    final List<PortalSignoffTargetUser> targets=new ArrayList<>();
    @BeforeEach void setup(){
        ReflectionTestUtils.setField(service,"documentMapper",documents);ReflectionTestUtils.setField(service,"aliasMapper",aliases);ReflectionTestUtils.setField(service,"signoffMapper",signoffs);ReflectionTestUtils.setField(service,"signoffTargets",people);
        var document=new PortalHrDocumentDO();document.setId(7L);document.setSlug("current-handbook");document.setVersion("v2.0");document.setRequiresSignoff(true);document.setPublishedAt(LocalDateTime.of(2026,11,10,10,0));document.setStatus("draft");document.setDeptScope(List.of("研发部"));
        when(documents.selectById(7L)).thenReturn(document);when(aliases.selectDocumentIdByAlias("employee-handbook")).thenReturn(7L);
        when(aliases.selectByDocumentId(7L)).thenReturn(List.of(alias("employee-handbook"),alias("old-handbook"),alias("/hr-doc/path-handbook")));
        when(people.targets(any())).thenAnswer(call->new ArrayList<>(targets));
        when(signoffs.selectSignedUserIds(eq(7L),anyCollection(),anyString())).thenAnswer(call->{
            Collection<String> references=call.getArgument(1);String version=call.getArgument(2);Set<Long> ids=new HashSet<>();
            for(var row:ledger)if(row.getVersion().equals(version)&&(Objects.equals(row.getDocumentId(),7L)||(row.getDocumentId()==null&&references.contains(row.getDocument()))))ids.add(row.getUserId());
            return ids;
        });
    }
    @Test void idSelectionFindsNullIdLegacyAndHistoricalAliasesWithinRequestedVersion(){
        targets.addAll(List.of(person(1),person(2),person(3)));
        ledger.addAll(List.of(sign(1,null,"handbook","v1.0"),sign(2,null,"old-handbook","v1.0"),sign(3,7L,"article:7","v2.0"),sign(3,8L,"handbook","v1.0")));
        var stats=service.stats(null,7L,"v1.0");assertEquals(2,stats.get("signed_count"));assertEquals(1,stats.get("not_signed_count"));assertEquals(66.7,stats.get("coverage_pct"));
        verify(signoffs).selectSignedUserIds(eq(7L),argThat(values->values.containsAll(List.of("article:7","current-handbook","employee-handbook","handbook","old-handbook","/hr-doc/path-handbook","path-handbook"))),eq("v1.0"));
        var unsigned=(List<?>)stats.get("not_signed");assertEquals("employee3",((Map<?,?>)unsigned.getFirst()).get("username"));
    }
    @Test void departedAndTransferredSignaturesAreExcludedFromCurrentScope(){
        targets.addAll(List.of(person(1),person(2)));
        ledger.addAll(List.of(sign(1,7L,"article:7","v2.0"),sign(3,7L,"article:7","v2.0"),sign(4,null,"employee-handbook","v2.0")));
        var stats=service.stats(null,7L,null);assertEquals(2,stats.get("total_active"));assertEquals(1,stats.get("signed_count"));assertEquals(1,stats.get("not_signed_count"));assertEquals(50.0,stats.get("coverage_pct"));verify(people).targets(List.of("研发部"));
    }
    @Test void duplicateSignaturesUnderDifferentReferencesCountOnce(){
        targets.add(person(1));ledger.addAll(List.of(sign(1,7L,"article:7","v2.0"),sign(1,null,"employee-handbook","v2.0"),sign(1,null,"handbook","v2.0"),sign(1,null,"old-handbook","v2.0")));
        var stats=service.stats(null,7L,null);assertEquals(1,stats.get("signed_count"));assertEquals(0,stats.get("not_signed_count"));assertEquals(100.0,stats.get("coverage_pct"));
    }
    @Test void legacyDropdownStillMatchesTheSameEvidenceAsStableId(){
        targets.add(person(1));ledger.add(sign(1,null,"handbook","v1.0"));
        var byId=service.stats(null,7L,"v1.0");var byLegacy=service.stats("handbook",null,"v1.0");
        assertEquals(byId.get("signed_count"),byLegacy.get("signed_count"));assertEquals(byId.get("coverage_pct"),byLegacy.get("coverage_pct"));assertEquals("handbook",byLegacy.get("document"));assertEquals(7L,byLegacy.get("document_id"));
    }
    @Test void zeroCurrentTargetsCannotProduceHistoricalCoverageAboveZero(){
        ledger.add(sign(1,null,"handbook","v2.0"));var stats=service.stats(null,7L,null);assertEquals(0,stats.get("total_active"));assertEquals(0,stats.get("signed_count"));assertEquals(0.0,stats.get("coverage_pct"));
    }
    @Test void fixedLegacyStatsSurviveAliasesPointingAtUnpublishedIndexes(){
        staticLookups();targets.addAll(List.of(person(1),person(2)));
        for(boolean requiresSignoff:List.of(false,true)){
            var index=fixedCms("index-only",requiresSignoff,false);when(documents.selectById(7L)).thenReturn(index);
            for(String reference:List.of("handbook","employee-handbook","incentive","hr-incentive")){
                String expected=reference.contains("handbook")?"v1.0":"v2.0";ledger.clear();ledger.add(sign(1,null,reference,expected));
                var stats=service.stats(reference,null,null);assertEquals(expected,stats.get("version"));assertNull(stats.get("document_id"));assertEquals(1,stats.get("signed_count"));assertEquals(50.0,stats.get("coverage_pct"));
            }
        }
        verify(people,atLeastOnce()).targets(isNull());
    }
    @Test void fixedLegacyStatsSurviveCmsIndexesWithoutSigningOrAUsableVersion(){
        staticLookups();targets.add(person(1));
        for(var cms:List.of(fixedCms("index-only",false,true),fixedCms("",true,true))){
            when(documents.selectById(7L)).thenReturn(cms);ledger.clear();ledger.add(sign(1,null,"incentive","v2.0"));
            var stats=service.stats("incentive",null,null);assertEquals("v2.0",stats.get("version"));assertNull(stats.get("document_id"));assertEquals(1,stats.get("signed_count"));
        }
    }
    @Test void usablePublishedCmsVersionWinsOverTheFixedLegacyVersion(){
        var cms=fixedCms("v7.0",true,true);when(documents.selectById(7L)).thenReturn(cms);targets.add(person(1));ledger.add(sign(1,7L,"article:7","v7.0"));
        var stats=service.stats("handbook",null,null);assertEquals("v7.0",stats.get("version"));assertEquals(7L,stats.get("document_id"));assertEquals(1,stats.get("signed_count"));
    }
    @Test void ordinaryUnknownSlugIsNeverDowngradedToAFixedLegacyDocument(){
        when(aliases.selectDocumentIdByAlias("ordinary-slug")).thenReturn(7L);when(documents.selectById(7L)).thenReturn(fixedCms("index-only",false,false));
        var failure=assertThrows(ServiceException.class,()->service.stats("ordinary-slug",null,null));assertEquals(SIGNOFF_UNKNOWN_DOC.getCode(),failure.getCode());verifyNoInteractions(signoffs,people);
    }
    @Test void stableIdOfUnpublishedIndexIsNotAStaticLegacyFallback(){
        when(documents.selectById(7L)).thenReturn(fixedCms("index-only",false,false));
        var failure=assertThrows(ServiceException.class,()->service.stats(null,7L,null));assertEquals(SIGNOFF_UNKNOWN_DOC.getCode(),failure.getCode());verifyNoInteractions(signoffs,people);
    }
    @Test void statsFallbackDoesNotPermitReminderOnUnusableManagedDocument(){
        when(aliases.selectDocumentIdByAlias("hr-incentive")).thenReturn(7L);when(documents.selectById(7L)).thenReturn(fixedCms("index-only",false,false));
        var failure=assertThrows(ServiceException.class,()->service.notifyUnsigned(99L,"incentive",null,null,null));assertEquals(SIGNOFF_NOT_SIGNABLE.getCode(),failure.getCode());verifyNoInteractions(signoffs,people);
    }
    void staticLookups(){
        when(aliases.selectDocumentIdByAlias("hr-incentive")).thenReturn(7L);
        when(signoffs.selectSignedUserIds(isNull(),anyCollection(),anyString())).thenAnswer(call->{Collection<String> references=call.getArgument(1);String version=call.getArgument(2);Set<Long> ids=new HashSet<>();for(var row:ledger)if(row.getDocumentId()==null&&row.getVersion().equals(version)&&references.contains(row.getDocument()))ids.add(row.getUserId());return ids;});
    }
    PortalHrDocumentDO fixedCms(String version,boolean signing,boolean published){var doc=new PortalHrDocumentDO();doc.setId(7L);doc.setSlug("historical-index");doc.setVersion(version);doc.setRequiresSignoff(signing);if(published)doc.setPublishedAt(LocalDateTime.of(2026,11,10,10,0));return doc;}
    PortalSignoffTargetUser person(long id){return new PortalSignoffTargetUser(id,"employee"+id,"员工"+id,"研发部");}
    PortalHrDocumentAliasDO alias(String value){var row=new PortalHrDocumentAliasDO();row.setDocumentId(7L);row.setAlias(value);return row;}
    PortalHandbookSignoffDO sign(long userId,Long docId,String reference,String version){var row=new PortalHandbookSignoffDO();row.setUserId(userId);row.setDocumentId(docId);row.setDocument(reference);row.setVersion(version);return row;}
}
