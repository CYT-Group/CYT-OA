package com.cyt.center.module.portal.service.admin;

import static org.junit.jupiter.api.Assertions.*;

import com.cyt.center.core.exception.BizException;

import org.junit.jupiter.api.Test;

import java.util.List;

class PortalImportServiceTest {
    @Test
    void readsBomCommasQuotesAndEmbeddedNewlines() {
        var rows = PortalImportService.parseCsv("\uFEFF标题,内容\r\n\"标题,一\",\"正文\n含\"\"引号\"\"\"\r\n");
        assertEquals(List.of("标题", "内容"), rows.getFirst());
        assertEquals(List.of("标题,一", "正文\n含\"引号\""), rows.get(1));
    }

    @Test
    void rejectsUnclosedQuotesAndOversizeCell() {
        assertThrows(BizException.class, () -> PortalImportService.parseCsv("title\n\"unfinished"));
        assertThrows(
                BizException.class, () -> PortalImportService.parseCsv("x\n" + "a".repeat(200001)));
    }
}
