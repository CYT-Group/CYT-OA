package com.cyt.center.module.portal.service.hrdoc;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.handler.SaCheckPermissionHandler;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpLogic;
import com.cyt.center.module.portal.controller.admin.signoff.PortalHandbookSignoffController;
import com.cyt.center.module.portal.dal.postgresql.signoff.PortalSignoffDocumentRepository;
import com.cyt.center.module.portal.domain.signoff.PortalSignoffDocumentOption;
import com.cyt.center.module.portal.service.signoff.PortalHandbookSignoffServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortalSignoffDocumentOptionsTest {
    @Test void statsPermissionAlonePassesActualAnnotationHandler()throws Exception{
        var method=PortalHandbookSignoffController.class.getMethod("documentOptions");
        var annotation=method.getAnnotation(SaCheckPermission.class);
        assertArrayEquals(new String[]{"can_view_signoff_stats"},annotation.value());
        StpLogic logic=mock(StpLogic.class);
        doAnswer(call->{String[] permissions=(String[])call.getRawArguments()[0];for(String permission:permissions)if(!permission.equals("can_view_signoff_stats"))throw new NotPermissionException(permission);return null;}).when(logic).checkPermissionAnd(any(String[].class));
        try(var manager=mockStatic(SaManager.class)){
            manager.when(()->SaManager.getStpLogic("",false)).thenReturn(logic);
            assertDoesNotThrow(()->new SaCheckPermissionHandler().checkMethod(annotation,method));
            verify(logic).checkPermissionAnd("can_view_signoff_stats");
        }
    }
    @Test void editPermissionDoesNotBypassStatsPermission()throws Exception{
        var method=PortalHandbookSignoffController.class.getMethod("documentOptions");StpLogic logic=mock(StpLogic.class);
        doThrow(new NotPermissionException("can_view_signoff_stats")).when(logic).checkPermissionAnd(any(String[].class));
        try(var manager=mockStatic(SaManager.class)){
            manager.when(()->SaManager.getStpLogic("",false)).thenReturn(logic);
            assertThrows(NotPermissionException.class,()->new SaCheckPermissionHandler().checkMethod(method.getAnnotation(SaCheckPermission.class),method));
        }
    }
    @Test void repositoryProjectsOnlyPublishedMetadataWithoutDepartmentOrDraftFilters()throws Exception{
        JdbcTemplate jdbc=mock(JdbcTemplate.class);ResultSet row=mock(ResultSet.class);
        when(row.getLong("id")).thenReturn(7L);when(row.getString("title")).thenReturn("正式制度标题");when(row.getString("version")).thenReturn("v1.0");when(row.getBoolean("requires_signoff")).thenReturn(true);
        when(row.getTimestamp("published_at")).thenReturn(Timestamp.valueOf("2026-11-10 10:00:00"));when(row.getTimestamp("offline_at")).thenReturn(Timestamp.valueOf("2026-11-11 10:00:00"));
        when(jdbc.query(anyString(),org.mockito.ArgumentMatchers.<RowMapper<PortalSignoffDocumentOption>>any())).thenAnswer(call->{
            String sql=call.getArgument(0);assertTrue(sql.contains("published_at IS NOT NULL"));assertTrue(sql.contains("requires_signoff=1"));assertTrue(sql.contains("deleted_at IS NULL"));
            assertFalse(sql.contains("draft"));assertFalse(sql.contains("content"));assertFalse(sql.contains("dept"));assertFalse(sql.contains("status="));
            RowMapper<PortalSignoffDocumentOption> mapping=call.getArgument(1);return List.of(mapping.mapRow(row,0));
        });
        var result=new PortalSignoffDocumentRepository(jdbc).options().getFirst();
        assertEquals("正式制度标题",result.title());assertEquals("v1.0",result.version());assertEquals(LocalDateTime.of(2026,11,11,10,0),result.offlineAt());
        verify(row,never()).getString(startsWith("draft"));verify(row,never()).getString(contains("content"));
    }
    @Test void serviceAndHttpPayloadCannotLeakDraftOrBodyFields(){
        var repository=mock(PortalSignoffDocumentRepository.class);var service=new PortalHandbookSignoffServiceImpl();ReflectionTestUtils.setField(service,"signoffDocuments",repository);
        var item=new PortalSignoffDocumentOption(7L,"已发布快照","v1.0",true,LocalDateTime.of(2026,11,10,10,0),null);when(repository.options()).thenReturn(List.of(item));
        var response=new PortalHandbookSignoffController(service).documentOptions();
        var json=new ObjectMapper().findAndRegisterModules().valueToTree(response.getData().getFirst());Set<String> keys=new HashSet<>();json.fieldNames().forEachRemaining(keys::add);
        assertEquals(Set.of("id","title","version","requiresSignoff","publishedAt","offlineAt"),keys);assertEquals("已发布快照",json.path("title").asText());assertEquals(1,response.getData().size());verify(repository,only()).options();
    }
}
