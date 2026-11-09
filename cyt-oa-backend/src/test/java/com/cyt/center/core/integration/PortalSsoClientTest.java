package com.cyt.center.core.integration;

import com.cyt.center.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Portal 基址校验：SSO 走的是外跳，地址只能是我们自己控制的 HTTPS 源站。
 */
class PortalSsoClientTest {

    @Test
    void acceptsHttpsOriginAndTrimsTrailingSlashes() {
        assertEquals("https://portal.cyt.group", PortalSsoClient.checkedBase("https://portal.cyt.group"));
        assertEquals("https://portal.cyt.group", PortalSsoClient.checkedBase("  https://portal.cyt.group///  "));
    }

    @Test
    void acceptsPlainHttpOnlyOnLoopback() {
        assertEquals("http://localhost:8000", PortalSsoClient.checkedBase("http://localhost:8000"));
        assertEquals("http://127.0.0.1:8000", PortalSsoClient.checkedBase("http://127.0.0.1:8000"));
    }

    @Test
    void rejectsAnythingThatCouldSendUsersElsewhere() {
        assertRejected(null);
        assertRejected("");
        assertRejected("   ");
        // 明文 HTTP 指向真实主机：授权码和 code_verifier 会被中间人拿走
        assertRejected("http://portal.cyt.group");
        // 带路径/查询/片段说明这不是源站基址，拼出来的 callback_uri 会跑偏
        assertRejected("https://portal.cyt.group/backend");
        assertRejected("https://portal.cyt.group?next=evil");
        assertRejected("https://portal.cyt.group#token");
        // 内嵌凭据是 SSRF/凭据夹带的典型写法
        assertRejected("https://operator:pw@portal.cyt.group");
        // 无协议、伪协议
        assertRejected("portal.cyt.group");
        assertRejected("javascript:alert(1)");
        assertRejected("not a url at all");
    }

    private static void assertRejected(String value) {
        BizException error = assertThrows(BizException.class, () -> PortalSsoClient.checkedBase(value));
        assertEquals("统一登录暂不可用，请联系管理员确认本系统已在 Portal 登记接入", error.getMessage());
    }
}
