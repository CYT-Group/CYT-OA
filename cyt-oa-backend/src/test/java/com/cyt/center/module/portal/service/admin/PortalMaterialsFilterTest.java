package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.security.CurrentUser;
import com.cyt.center.module.portal.service.support.PortalAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortalMaterialsFilterTest {
    record Call(String sql,Object[] args) {}
    final List<Call> calls=new ArrayList<>();
    final JdbcTemplate jdbc=mock(JdbcTemplate.class,invocation->{
        var args=invocation.getRawArguments();
        if(args.length>0&&args[0] instanceof String sql)calls.add(new Call(sql,args));
        return switch(invocation.getMethod().getName()){
            case "queryForList" -> List.of();
            case "queryForObject" -> 0L;
            case "queryForMap" -> Map.of("skuCount",0,"stockQty",0,"outOfStockCount",0);
            default -> RETURNS_DEFAULTS.answer(invocation);
        };
    });
    final PortalMaterialsService service=new PortalMaterialsService(jdbc,mock(CurrentUser.class),mock(PortalAudit.class),new ObjectMapper());
    @Test void inventoryFiltersUseSameSqlPredicateForListAndTotal(){
        var result=service.page("sample","inventory","camera",false,2,20,new PortalMaterialsService.Filters(12L,"品牌","SZ",null,"out"));
        var list=calls.stream().filter(c->c.sql().startsWith("SELECT m.*")).findFirst().orElseThrow();
        var count=calls.stream().filter(c->c.sql().startsWith("SELECT COUNT(*) FROM portal_materials m")).findFirst().orElseThrow();
        assertEquals(list.sql().substring(list.sql().indexOf(" WHERE "),list.sql().indexOf(" ORDER BY")),count.sql().substring(count.sql().indexOf(" WHERE ")));
        assertTrue(list.sql().contains("p.category_id=?"));assertTrue(list.sql().contains("m.brand=?"));assertTrue(list.sql().contains("m.site=?"));assertTrue(list.sql().contains("m.stock_qty=0"));
        assertEquals(List.of("sample","%camera%",12L,"品牌","SZ",20,20),Arrays.asList((Object[])list.args()[1]));
        assertEquals(List.of("sample","%camera%",12L,"品牌","SZ"),Arrays.asList((Object[])count.args()[2]));
        assertTrue(((Map<?,?>)result.get("summary")).containsKey("outOfStockCount"));
    }
    @Test void applicationSiteFilterUsesExistsSoMultiItemAppliesDoNotDuplicate(){
        service.page("sample","applies",null,false,1,20,new PortalMaterialsService.Filters(null,null,"CD","borrowed",null));
        var sql=calls.stream().filter(c->c.sql().startsWith("SELECT m.*")).findFirst().orElseThrow().sql();
        assertTrue(sql.contains("EXISTS (SELECT 1 FROM portal_material_apply_items"));assertTrue(sql.contains("i.apply_id=m.id AND s.site=?"));assertTrue(sql.contains("m.status=?"));
    }
    @Test void invalidFilterValuesFailBeforeAnyQuery(){
        assertThrows(BizException.class,()->service.page("sample","inventory",null,false,1,20,new PortalMaterialsService.Filters(null,null,"invalid",null,null)));
        assertTrue(calls.isEmpty());
    }
}
