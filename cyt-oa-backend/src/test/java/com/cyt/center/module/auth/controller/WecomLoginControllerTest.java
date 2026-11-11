package com.cyt.center.module.auth.controller;

import com.cyt.center.core.exception.BizException;
import com.cyt.center.core.integration.PortalSsoClient;
import com.cyt.center.module.auth.model.vo.LoginVO;
import com.cyt.center.module.auth.service.AuthService;
import com.cyt.center.module.system.model.entity.SysUser;
import com.cyt.center.module.system.service.SysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WecomLoginControllerTest {
    private final PortalSsoClient portal = mock(PortalSsoClient.class);
    private final SysUserService users = mock(SysUserService.class);
    private final AuthService auth = mock(AuthService.class);
    private final WecomLoginController controller = new WecomLoginController(portal, users, auth);
    private final MockHttpSession session = new MockHttpSession();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final AtomicReference<String> nonce = new AtomicReference<>();
    private final String portalState = "oa." + "s".repeat(43);
    private final String portalCode = "oa_" + "c".repeat(43);
    private final SysUser employee = new SysUser();

    @BeforeEach
    void prepare() {
        request.setSession(session);
        employee.setId(2L);
        employee.setEnabled(true);
        employee.setWecomUserid("OriginalUserID");
        when(portal.portalBase()).thenReturn("https://portal.cyt.group");
        when(portal.returnBaseQuietly()).thenReturn("https://oa.cyt.group");
        when(portal.authorize(anyString(), anyString())).thenAnswer(call -> {
            nonce.set(call.getArgument(0));
            assertTrue(call.<String>getArgument(1).matches("[A-Za-z0-9_-]{43}"));
            return new PortalSsoClient.Authorization("https://oa.cyt.group", Map.of("state", portalState));
        });
        when(portal.matchesCodeFormat(portalCode)).thenReturn(true);
        when(portal.exchange(eq(portalCode), anyString(), anyString())).thenReturn("OriginalUserID");
        when(users.byWecomUserid("OriginalUserID")).thenReturn(employee);
        when(users.requireById(2L)).thenReturn(employee);
        when(auth.issue(2L, "auto")).thenReturn(new LoginVO("oa-session", "admin", 2L, "员工", List.of("hr"), List.of("can_enter_admin")));
    }

    @Test
    void portalStartBindsBrowserAndPreservesOnlyTheLocalDestination() {
        var response = controller.portalAuthorize("/portal/hr-documents?mode=new", session);
        assertEquals("https://portal.cyt.group/sso/oa?state=oa." + "s".repeat(43), response.getHeaders().getLocation().toString());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals(Map.of("returnTo", "/portal/hr-documents?mode=new"), controller.portalContext(request).data());
        assertEquals(Map.of("returnTo", "/"), controller.portalContext(new MockHttpServletRequest()).data());
    }

    @Test
    void exchangesIdentityOnceAndUsesOaPermissionsInsteadOfPortalDeclarations() {
        controller.portalAuthorize("/portal/hr-documents?mode=new", session);
        var callback = controller.callback(portalCode, nonce.get(), request, session);
        URI location = callback.getHeaders().getLocation();
        assertEquals("https://oa.cyt.group/login", location.getScheme() + "://" + location.getAuthority() + location.getPath());
        var query = query(location);
        assertEquals("/portal/hr-documents?mode=new", query.get("from"));
        assertTrue(query.get("wecom_code").matches("[a-f0-9]{32}"));
        assertFalse(location.toString().contains("oa-session"));
        assertEquals("oa-session", controller.exchange(new WecomLoginController.ExchangeRequest(query.get("wecom_code")), session).data().token());
        assertThrows(BizException.class, () -> controller.exchange(new WecomLoginController.ExchangeRequest(query.get("wecom_code")), session));
        verify(auth, times(1)).issue(2L, "auto");
    }

    @Test
    void otherBrowserCannotConsumeThePortalCallbackOrLocalHandoff() {
        controller.portalAuthorize("/portal", session);
        var other = new MockHttpSession();
        var otherRequest = new MockHttpServletRequest();
        otherRequest.setSession(other);
        assertTrue(controller.callback(portalCode, nonce.get(), otherRequest, other).getHeaders().getLocation().toString().contains("wecom_error=login_failed"));
        verify(portal, never()).exchange(anyString(), anyString(), anyString());
        var callback = controller.callback(portalCode, nonce.get(), request, session);
        String handoff = query(callback.getHeaders().getLocation()).get("wecom_code");
        assertThrows(BizException.class, () -> controller.exchange(new WecomLoginController.ExchangeRequest(handoff), other));
        assertNotNull(controller.exchange(new WecomLoginController.ExchangeRequest(handoff), session).data());
    }

    @Test
    void refusesUnmappedOrDisabledEmployees() {
        controller.portalAuthorize("/portal", session);
        employee.setEnabled(false);
        assertTrue(controller.callback(portalCode, nonce.get(), request, session).getHeaders().getLocation().toString().contains("wecom_error=login_failed"));
        verifyNoInteractions(auth);
    }

    @Test
    void refreshedLoginInvalidatesTheEarlierHandoff() {
        controller.portalAuthorize("/portal", session);
        String handoff = query(controller.callback(portalCode, nonce.get(), request, session).getHeaders().getLocation()).get("wecom_code");
        controller.portalAuthorize("/me/overview", session);
        assertThrows(BizException.class, () -> controller.exchange(new WecomLoginController.ExchangeRequest(handoff), session));
        verifyNoInteractions(auth);
    }

    @Test
    void unavailablePortalReturnsToTheOaLoginWithoutARedirectLoop() {
        doThrow(new BizException("not ready")).when(portal).requireEnabled();
        var location = controller.portalAuthorize("https://untrusted.example", session).getHeaders().getLocation();
        assertEquals("/", query(location).get("from"));
        assertEquals("portal_unavailable", query(location).get("sso_error"));
        assertEquals("/login", location.getPath());
    }

    @Test
    void returnDestinationCannotOverrideTheOriginOrEnterAnAuthenticationEndpoint() {
        for (String value : List.of("//evil.example", "/\\evil.example", "/%2f%2fevil.example", "/%252f%252fevil.example", "/api/wecom/login", "/login?from=/portal", "/a/../api/wecom/login", "/portal?x=%0aevil", "https://evil.example")) {
            assertEquals("/", LoginTarget.safe(value), value);
        }
        assertEquals("/portal/hr-documents?mode=preview&id=19", LoginTarget.safe("/portal/hr-documents?mode=preview&id=19"));
    }

    private static Map<String, String> query(URI uri) {
        return Stream.of(uri.getRawQuery().split("&")).map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }
}
