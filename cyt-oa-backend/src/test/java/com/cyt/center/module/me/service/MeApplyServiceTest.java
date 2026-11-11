package com.cyt.center.module.me.service;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.framework.common.pojo.PageParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeApplyServiceTest {
    private final MeApplyService service = new MeApplyService(
            null, null, null, null, null, null, null, null, null, null, null);

    @Test
    void exposesOnlySupportedApplicationTypes() {
        assertEquals(List.of("leave", "overtime", "reimbursement", "travel",
                "travel-reimbursement", "supply", "schedule"), MeApplyService.types());
    }

    @Test
    void rejectsRetiredSealTypeBeforeAccessingBusinessServices() {
        assertThrows(BizException.class, () -> service.page(1L, "seal", null, new PageParam()));
        assertThrows(BizException.class, () -> service.sync(1L, "seal", 1L));
        assertThrows(BizException.class, () -> service.cancel(1L, "seal", 1L, null));
    }
}
