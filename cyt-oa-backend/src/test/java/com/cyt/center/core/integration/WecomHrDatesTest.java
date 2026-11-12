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
        assertThrows(BizException.class,()->parse("{\"field_info\":[{\"fieldid\":7,\"result\":1,\"value_type\":4,\"value_int64\":4102444800}]}"));
    }
    @Test void duplicateFieldResponsesAreRejected() {
        assertThrows(BizException.class,()->parse("{\"field_info\":[{\"fieldid\":7,\"result\":3},{\"fieldid\":7,\"result\":3}]}"));
    }

    @Test void mapsEmploymentAndProbationUsingConfiguredOptionTextRatherThanOptionId() throws Exception {
        var config = WecomHrDates.discover(json.readTree("""
                {"group_list":[{"field_list":[
                  {"field_name":"员工状态","fieldid":904,"field_type":2,"value_type":3,
                   "option_list":[{"id":91,"value":"试用"},{"id":2,"value":"正式"}]},
                  {"field_name":"试用期","fieldid":905,"field_type":2,"value_type":3,
                   "option_list":[{"id":77,"value":"3个月"},{"id":99,"value":"无试用期"}]}
                ]}]}
                """));
        var parsed = WecomHrDates.parseProfile(json.readTree("""
                {"field_info":[
                  {"fieldid":904,"result":1,"value_type":3,"value_uint32":91},
                  {"fieldid":905,"result":1,"value_type":3,"value_uint32":77}
                ]}
                """), config.fields(), LocalDate.of(2026,11,12));
        assertEquals(Map.of("entryStatus",1,"type",1,"status",2,"probation",3), parsed.values());
        assertEquals(2, parsed.readableFields());
        var formal = WecomHrDates.parseProfile(json.readTree("""
                {"field_info":[{"fieldid":904,"result":1,"value_type":3,"value_uint32":2}]}
                """), config.fields(), LocalDate.of(2026,11,12));
        assertEquals(1, formal.values().get("status"));
        assertEquals(1, formal.values().get("type"));
    }

    @Test void supportsFutureEntryAndRegularDatesButRejectsFutureBirthdayIndependently() throws Exception {
        Map<String,WecomHrDates.Field> config = Map.of("entryTime",new WecomHrDates.Field(8,2),
                "regularTime",new WecomHrDates.Field(9,2),"birthday",new WecomHrDates.Field(7,4));
        long future = LocalDate.of(2027,3,1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        var parsed = WecomHrDates.parseProfile(json.readTree("""
                {"field_info":[
                  {"fieldid":8,"result":1,"value_type":2,"value_uint64":%d},
                  {"fieldid":9,"result":1,"value_type":2,"value_uint64":%d},
                  {"fieldid":7,"result":1,"value_type":4,"value_int64":%d}
                ]}
                """.formatted(future,future,future)), config, LocalDate.of(2026,11,12));
        assertEquals(LocalDate.of(2027,3,1), parsed.values().get("entryTime"));
        assertEquals(LocalDate.of(2027,3,1), parsed.values().get("regularTime"));
        assertFalse(parsed.values().containsKey("birthday"));
        assertEquals("birthday", parsed.issues().getFirst().field());
    }

    @Test void unreadableAndUnknownStatesPreserveThoseFieldsWithoutDiscardingReadableDates() throws Exception {
        var config = new LinkedHashMap<>(fields);
        config.put("employeeStatus",new WecomHrDates.Field(30,3,2,Map.of("17","未配置的人事类型")));
        var parsed = WecomHrDates.parseProfile(json.readTree("""
                {"field_info":[
                  {"fieldid":7,"result":2},
                  {"fieldid":8,"result":1,"value_type":2,"value_uint64":1577808000},
                  {"fieldid":30,"result":1,"value_type":3,"value_uint32":17}
                ]}
                """), config, LocalDate.of(2026,11,12));
        assertEquals(Map.of("entryTime",LocalDate.of(2020,1,1)),parsed.values());
        assertEquals(1,parsed.readableFields());
        assertEquals(Set.of("birthday","employeeStatus"),parsed.issues().stream().map(WecomHrDates.Issue::field).collect(java.util.stream.Collectors.toSet()));
        assertFalse(parsed.values().containsKey("entryStatus"));
    }

    @Test void duplicateFieldIdConfigurationRemovesEveryAmbiguousDestination() throws Exception {
        var config = WecomHrDates.discover(json.readTree("""
                {"group_list":[{"field_list":[
                  {"field_name":"生日","fieldid":7,"field_type":3,"value_type":4},
                  {"field_name":"入职日期","fieldid":7,"field_type":3,"value_type":4},
                  {"field_name":"转正日期","fieldid":7,"field_type":3,"value_type":4}
                ]}]}
                """));
        assertTrue(config.fields().isEmpty());
        assertTrue(config.issues().stream().anyMatch(issue -> issue.field().equals("birthday") && issue.reason().contains("编号重复")));
        assertTrue(config.issues().stream().anyMatch(issue -> issue.field().equals("entryTime") && issue.reason().contains("编号重复")));
        assertTrue(config.issues().stream().anyMatch(issue -> issue.field().equals("regularTime") && issue.reason().contains("编号重复")));
    }

    @Test void statusOptionsMustBePresentAndNeverFallBackToNumericAssumptions() throws Exception {
        var config = WecomHrDates.discover(json.readTree("""
                {"group_list":[{"field_list":[{"field_name":"员工状态","fieldid":12004,"field_type":2,"value_type":3}]}]}
                """));
        assertFalse(config.fields().containsKey("employeeStatus"));
        assertTrue(config.issues().stream().anyMatch(issue -> issue.field().equals("employeeStatus") && issue.reason().contains("选项配置")));
    }

    @Test void distinguishesAbandonedPendingAndLeftWithoutInventingEmploymentDates() throws Exception {
        var config = Map.of("employeeStatus",new WecomHrDates.Field(44,1,1,Map.of()));
        var labels = Map.of("放弃入职",5,"待入职",2,"待离职",3,"已离职",4,"在职",1);
        for (var label:labels.entrySet()) {
            var node = json.createObjectNode(); node.putArray("field_info").addObject()
                    .put("fieldid",44).put("result",1).put("value_type",1).put("value_string",label.getKey());
            var parsed = WecomHrDates.parseProfile(node, config, LocalDate.of(2026,11,12));
            assertEquals(Map.of("entryStatus",label.getValue()),parsed.values());
        }
    }

    @Test void explicitEmptyRegularDateClearsButOmittedOrDeniedFieldsAreNotPatches() throws Exception {
        var config = Map.of("regularTime",new WecomHrDates.Field(11,2),"leaveTime",new WecomHrDates.Field(12,2));
        var parsed = WecomHrDates.parseProfile(json.readTree("""
                {"field_info":[{"fieldid":11,"result":1,"value_type":2,"value_uint64":null},{"fieldid":12,"result":5}]}
                """),config,LocalDate.of(2026,11,12));
        assertTrue(parsed.values().containsKey("regularTime"));
        assertNull(parsed.values().get("regularTime"));
        assertFalse(parsed.values().containsKey("leaveTime"));
        assertEquals(1,parsed.readableFields());
    }
    Map<String,LocalDate> parse(String value) throws Exception {return WecomHrDates.parse(json.readTree(value),fields,LocalDate.of(2026,11,6));}
}
