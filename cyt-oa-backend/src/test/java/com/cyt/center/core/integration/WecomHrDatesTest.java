package com.cyt.center.core.integration;

import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WecomHrDatesTest {
    final ObjectMapper json = new ObjectMapper();
    final Map<String,WecomHrDates.Field> fields = Map.of("birthday",new WecomHrDates.Field(7,4),"entryTime",new WecomHrDates.Field(8,2));
    @Test void discoversDateFieldsWithoutHardCodedEnterpriseIds() throws Exception {
        var result=WecomHrDates.fields(json.readTree("{\"group_list\":[{\"field_list\":[{\"field_name\":\"生日\",\"fieldid\":7,\"field_type\":3,\"value_type\":4},{\"field_name\":\"入职日期\",\"fieldid\":8,\"field_type\":3,\"value_type\":2}]}]}"));
        assertEquals(fields,result);
    }
    @Test void parsesSignedPre1970BirthdayAndBeijingMidnight() throws Exception {
        long birthday=LocalDate.of(1960,1,2).atStartOfDay(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        long hired=LocalDate.of(2020,5,1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        var result=parse("{\"field_info\":[{\"fieldid\":7,\"result\":1,\"value_type\":4,\"value_int64\":"+birthday+"},{\"fieldid\":8,\"result\":1,\"value_type\":2,\"value_uint64\":\""+hired+"\"}]}");
        assertEquals(LocalDate.of(1960,1,2),result.get("birthday"));assertEquals(LocalDate.of(2020,5,1),result.get("entryTime"));
    }
    @Test void explicitEmptyIsAResetButUnavailableAndOmittedFieldsAreNot() throws Exception {
        var dates = parse("{\"field_info\":[{\"fieldid\":7,\"result\":3},{\"fieldid\":8,\"result\":1,\"value_type\":2,\"value_uint64\":null}]}");
        assertFalse(dates.containsKey("birthday")); assertTrue(dates.containsKey("entryTime")); assertNull(dates.get("entryTime"));
        assertTrue(parse("{\"field_info\":[]}").isEmpty());
        assertTrue(parse("{\"field_info\":[{\"fieldid\":7,\"result\":1}]}").isEmpty());
    }
    @Test void fieldErrorsAndFutureDatesRejectEntireEmployeePatch() {
        assertThrows(BizException.class,()->parse("{\"field_info\":[{\"fieldid\":7,\"result\":2}]}"));
        assertThrows(BizException.class,()->parse("{\"field_info\":[{\"fieldid\":8,\"result\":1,\"value_type\":2,\"value_uint64\":4102444800}]}"));
    }
    @Test void duplicateFieldResponsesAreRejected() {
        assertThrows(BizException.class,()->parse("{\"field_info\":[{\"fieldid\":7,\"result\":3},{\"fieldid\":7,\"result\":3}]}"));
    }
    Map<String,LocalDate> parse(String value) throws Exception {return WecomHrDates.parse(json.readTree(value),fields,LocalDate.of(2026,11,6));}
}
