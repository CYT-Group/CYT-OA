package com.cyt.center.module.portal.service.workspace;

import com.cyt.center.core.exception.AuthException;
import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceTeamAttendanceTest {
    record Query(String sql,Object[] args) {}
    final List<Query> queries=new ArrayList<>();
    final WorkspaceIdentity identity=mock(WorkspaceIdentity.class);
    final JdbcTemplate jdbc=mock(JdbcTemplate.class,invocation->{
        var args=invocation.getRawArguments();if(args.length>0&&args[0] instanceof String sql)queries.add(new Query(sql,args));
        return switch(invocation.getMethod().getName()){
            case "queryForList" -> List.of();case "queryForObject" -> 0L;case "queryForMap" -> Map.of("records",0,"normal",0,"late",0,"early",0,"leave",0,"travel",0);
            default -> RETURNS_DEFAULTS.answer(invocation);
        };
    });
    final WorkspaceTeamAttendanceService service=new WorkspaceTeamAttendanceService(identity,jdbc);
    @Test void companyScopeCannotBeRequestedByNonAdmin(){
        assertThrows(AuthException.class,()->service.page("2026-11","company",1,20));assertTrue(queries.isEmpty());
    }
    @Test void noTeamAndMalformedMonthCannotReachAttendanceData(){
        assertThrows(AuthException.class,()->service.page("2026-11","team",1,20));
        assertThrows(BizException.class,()->service.page("2026-13","team",1,20));
        assertThrows(BizException.class,()->service.page("2026-11","team",0,20));assertTrue(queries.isEmpty());
    }
    @Test void serverDerivedTeamAndMonthBoundListCountAndSummaryIdentically(){
        when(identity.team()).thenReturn(List.of(7L,8L));var page=service.page("2026-11","team",2,20);
        assertEquals(0L,page.get("total"));assertEquals(3,queries.size());
        String condition=null;
        for(var query:queries){
            String where=query.sql().substring(query.sql().indexOf(" WHERE "));if(where.contains(" ORDER BY"))where=where.substring(0,where.indexOf(" ORDER BY"));
            assertTrue(where.contains("a.deleted=0"));assertTrue(where.contains("a.user_id IN (?,?)"));
            if(condition==null)condition=where;else assertEquals(condition,where);
            Object[] args=(Object[])query.args()[query.args().length-1];
            assertEquals(Timestamp.valueOf("2026-11-01 00:00:00"),args[0]);assertEquals(Timestamp.valueOf("2026-12-01 00:00:00"),args[1]);assertEquals(7L,args[2]);assertEquals(8L,args[3]);
        }
        verify(identity,never()).allUsers();
    }
}
