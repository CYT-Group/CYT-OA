package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortalRowsTest {
    @Test void quantitiesCannotBeSilentlyRoundedOrOverflow() {
        assertEquals(2, PortalRows.number(2));
        assertEquals(2, PortalRows.number("2"));
        assertThrows(BizException.class, () -> PortalRows.number(1.5));
        assertThrows(BizException.class, () -> PortalRows.number("9223372036854775808"));
    }
}
