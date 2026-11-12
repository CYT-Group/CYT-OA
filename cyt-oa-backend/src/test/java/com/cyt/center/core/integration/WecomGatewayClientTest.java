package com.cyt.center.core.integration;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class WecomGatewayClientTest {
    @Test void rejectsMockSnapshotsForBothDirectoryAndHr() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/integrations/", exchange -> {
            byte[] body = "{\"ok\":true,\"meta\":{\"mock\":true},\"data\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var response = exchange.getResponseBody()) { response.write(body); }
        });
        server.start();
        try {
            CenterProperties properties = new CenterProperties();
            properties.getWecom().setGatewayUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getWecom().setAllowDevIdentity(true);
            WecomGatewayClient client = new WecomGatewayClient(properties, new ObjectMapper());
            assertTrue(assertThrows(BizException.class, client::directory).getMessage().contains("模拟模式"));
            assertTrue(assertThrows(BizException.class, client::hrFields).getMessage().contains("模拟模式"));
        } finally { server.stop(0); }
    }

    @Test void retainsSafeWecomCodeAndTraceWithoutExposingRawUpstreamMessage() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/integrations/",exchange->{
            byte[] body="{\"ok\":false,\"error\":{\"type\":\"upstream_error\",\"code\":\"WECOM_60111\",\"trace_id\":\"safe-trace-123\",\"message\":\"raw-private-data\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400,body.length);try(var output=exchange.getResponseBody()){output.write(body);}
        });server.start();
        try {
            var error=assertThrows(WecomGatewayException.class,()->client(server).hrFields());
            assertEquals("WECOM_60111",error.code());assertEquals("safe-trace-123",error.traceId());
            assertFalse(error.getMessage().contains("raw-private-data"));
        } finally {server.stop(0);}
    }

    @Test void sendsAllDiscoveredFieldIdsWithoutRepeatingOrAssumingDateOnlyFields() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicReference<com.fasterxml.jackson.databind.JsonNode> received=new AtomicReference<>();
        server.createContext("/integrations/",exchange->{
            received.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            byte[] body="{\"ok\":true,\"data\":{\"field_info\":[]}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);try(var output=exchange.getResponseBody()){output.write(body);}
        });server.start();
        try {
            client(server).hrStaff("employee",List.of(new WecomHrDates.Field(7,4),
                    new WecomHrDates.Field(904,3,2,Map.of("99","试用")),new WecomHrDates.Field(7,4)));
            assertEquals("employee",received.get().path("userid").asText());
            assertFalse(received.get().path("get_all").asBoolean(true));
            assertEquals(2,received.get().path("fieldids").size());
            assertEquals(904,received.get().path("fieldids").get(1).path("fieldid").asInt());
        } finally {server.stop(0);}
    }

    @Test void rejectsNonzeroHrErrcodeEvenWhenGatewayEnvelopeSaysOk() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/integrations/",exchange->{
            byte[] body="{\"ok\":true,\"data\":{\"errcode\":60111,\"errmsg\":\"raw-private-data\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,body.length);try(var output=exchange.getResponseBody()){output.write(body);}
        });server.start();
        try {
            var error=assertThrows(WecomGatewayException.class,()->client(server).hrFields());
            assertEquals("WECOM_60111",error.code());assertFalse(error.getMessage().contains("raw-private-data"));
        } finally {server.stop(0);}
    }

    private WecomGatewayClient client(HttpServer server) {
        var properties=new CenterProperties();properties.getWecom().setGatewayUrl("http://127.0.0.1:"+server.getAddress().getPort());
        properties.getWecom().setAllowDevIdentity(true);return new WecomGatewayClient(properties,new ObjectMapper());
    }
}
