package com.cyt.center.module.portal.service.hrdoc;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cyt.center.module.portal.dal.dataobject.signoff.PortalHandbookSignoffDO;
import com.cyt.center.module.portal.dal.mysql.signoff.PortalHandbookSignoffMapper;
import com.cyt.center.module.portal.dal.mysql.signoff.PortalSignoffTargetRepository;
import com.cyt.center.module.portal.domain.signoff.PortalSignoffTargetUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PortalSignoffRepositoryQueryTest {
    @BeforeAll static void tableInfo(){TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"signoff-test"),PortalHandbookSignoffDO.class);}
    @Test void evidenceQueryKeepsVersionOutsideIdentityUnionAndDeduplicatesUsers(){
        var mapper=mock(PortalHandbookSignoffMapper.class,CALLS_REAL_METHODS);var a=new PortalHandbookSignoffDO();a.setUserId(1L);a.setDocumentId(7L);var b=new PortalHandbookSignoffDO();b.setUserId(1L);b.setDocument("handbook");var c=new PortalHandbookSignoffDO();c.setUserId(2L);
        doReturn(List.of(a,b,c)).when(mapper).selectList(any(Wrapper.class));
        assertEquals(Set.of(1L,2L),mapper.selectSignedUserIds(7L,List.of("article:7","handbook"),"v1.0"));
        var query=ArgumentCaptor.forClass(Wrapper.class);verify(mapper).selectList(query.capture());
        String sql=query.getValue().getSqlSegment();assertTrue(sql.contains("version ="));assertTrue(sql.contains("AND (document_id ="));assertTrue(sql.contains("OR (document_id IS NULL AND document IN"));
        var wrapper=(LambdaQueryWrapper<?>)query.getValue();assertTrue(wrapper.getParamNameValuePairs().containsValue("v1.0"));assertFalse(wrapper.getParamNameValuePairs().containsValue("v2.0"));
    }
    @Test void noDocumentIdentityNeverRunsUnboundedSignatureQuery(){
        var mapper=mock(PortalHandbookSignoffMapper.class,CALLS_REAL_METHODS);assertTrue(mapper.selectSignedUserIds(null,List.of(),"v1.0").isEmpty());verify(mapper,never()).selectList(any(Wrapper.class));
    }
    @Test void targetQueryIncludesOnlyEnabledUsersAndAnyActualDepartmentMembership(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);
        when(jdbc.query(anyString(),org.mockito.ArgumentMatchers.<RowMapper<PortalSignoffTargetUser>>any(),any(Object[].class))).thenReturn(List.of());
        new PortalSignoffTargetRepository(jdbc).targets(List.of("研发部"));
        var sql=ArgumentCaptor.forClass(String.class);var args=ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sql.capture(),org.mockito.ArgumentMatchers.<RowMapper<PortalSignoffTargetUser>>any(),args.capture());
        assertTrue(sql.getValue().contains("u.enabled=1"));assertTrue(sql.getValue().contains("target_dept.id=u.main_dept_id OR EXISTS"));assertTrue(sql.getValue().contains("membership.user_id=u.id"));assertTrue(sql.getValue().contains("membership.dept_id=target_dept.id"));assertArrayEquals(new Object[]{"研发部"},args.getValue());
    }
}
