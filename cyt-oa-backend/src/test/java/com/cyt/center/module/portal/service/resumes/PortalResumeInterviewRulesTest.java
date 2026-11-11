package com.cyt.center.module.portal.service.resumes;

import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PortalResumeInterviewRulesTest {
    private final PortalResumeInterviewService service=new PortalResumeInterviewService(null,null,null,null);
    @Test void completedRoundRequiresBothDecisionAndFeedback() {
        assertThrows(BizException.class,()->service.cleanRound(Map.of("name","技术面","status","completed","result","passed"),true));
        var valid=service.cleanRound(Map.of("name"," 技术面 ","status","completed","result","passed","feedback"," 合格 "),true);
        assertEquals("技术面",valid.get("name"));assertEquals("合格",valid.get("feedback"));
    }
    @Test void pendingRoundCannotRetainOutcomeAndInputMustSpecifyTimezone() {
        assertThrows(BizException.class,()->service.cleanRound(Map.of("name","初面","status","pending","result","rejected"),true));
        assertThrows(BizException.class,()->service.cleanRound(Map.of("name","初面","scheduledAt","2026-11-11T09:00:00"),true));
        var valid=service.cleanRound(Map.of("name","初面","scheduledAt","2026-11-11T01:00:00Z"),true);
        assertEquals(LocalDateTime.of(2026,11,11,9,0),valid.get("scheduledAt"));
    }
}
