package com.cyt.center.module.portal.service.celebrations;

import com.cyt.center.module.portal.dal.mysql.celebrations.PortalCelebrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortalCelebrationRulesTest {
    @Test void leapDayUsesFebruary28AndBirthdayYearNeverLeavesEmployeeView() {
        var repo=mock(PortalCelebrationRepository.class);
        Map<String,Object> person=new LinkedHashMap<>(Map.of("userId",8L,"username","demo","name","测试员工","enabled",1,"optOut",0,"birthday",LocalDateTime.of(2000,2,29,0,0),"hireDate",LocalDateTime.of(2027,2,1,0,0)));
        when(repo.employees(null)).thenReturn(List.of(person));
        var service=new PortalCelebrationService(repo,null,null,null,new ObjectMapper(),null);
        var calendar=service.calendar("2027-02");assertEquals(1,calendar.size());
        assertEquals("2027-02-28",calendar.getFirst().get("date"));assertNull(calendar.getFirst().get("years"));
        var employee=service.employees(null).getFirst();assertEquals("02-29",employee.get("birthday"));assertFalse(employee.containsKey("wecomUserid"));
    }
    @Test void utf8TextChunksPreserveWholeUnicodeCodePoints() {
        String message="生日🎉".repeat(1000);List<String> chunks=PortalCelebrationService.chunks(message);
        assertEquals(message,String.join("",chunks));assertTrue(chunks.size()>1);
        for(String chunk:chunks)assertTrue(chunk.getBytes(StandardCharsets.UTF_8).length<=1900);
    }
}
