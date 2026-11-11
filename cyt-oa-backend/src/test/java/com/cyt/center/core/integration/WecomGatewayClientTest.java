package com.cyt.center.core.integration;

import com.cyt.center.core.config.CenterProperties;
import com.cyt.center.core.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
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
}
