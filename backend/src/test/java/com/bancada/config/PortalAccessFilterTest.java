package com.bancada.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bancada.service.PortalCertificateService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** On the customer panel port, only the portal exists; the administrator API is not there. */
class PortalAccessFilterTest {

    private static final int PORTAL_PORT = 8748;

    private final PortalCertificateService certificates = mock(PortalCertificateService.class);
    private final PortalAccessFilter filter = new PortalAccessFilter(PORTAL_PORT, certificates);

    private MockHttpServletResponse call(int port, String path, boolean secure) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setLocalPort(port);
        request.setServerName("painel.exemplo.com");
        request.setSecure(secure);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        response.setHeader("X-Chain", chain.getRequest() == null ? "no" : "yes");
        return response;
    }

    @Test
    void administratorApiDoesNotExistOnThePortalPort() throws Exception {
        MockHttpServletResponse response = call(PORTAL_PORT, "/api/devices", false);

        assertEquals(404, response.getStatus());
        assertEquals("no", response.getHeader("X-Chain"));
    }

    @Test
    void portalApiAndBundlesPassAndOtherPathsBecomeThePanelPage() throws Exception {
        assertEquals("yes", call(PORTAL_PORT, "/api/portal/public/plans", false).getHeader("X-Chain"));
        assertEquals("yes", call(PORTAL_PORT, "/assets/portal-abc.js", false).getHeader("X-Chain"));
        assertEquals("/portal.html", call(PORTAL_PORT, "/painel/vps/3", false).getForwardedUrl());
    }

    @Test
    void administratorPortIsUntouched() throws Exception {
        MockHttpServletResponse response = call(8747, "/api/devices", false);

        assertEquals("yes", response.getHeader("X-Chain"));
        assertNull(response.getForwardedUrl());
    }

    @Test
    void withCertificatePlainHttpGoesToHttpsExceptTheAcmeChallenge() throws Exception {
        when(certificates.isActiveFor("painel.exemplo.com")).thenReturn(true);

        MockHttpServletResponse page = call(PORTAL_PORT, "/painel", false);
        assertEquals(301, page.getStatus());
        assertEquals("https://painel.exemplo.com/painel", page.getHeader("Location"));
        assertNotNull(call(PORTAL_PORT, "/.well-known/acme-challenge/abc", false).getHeader("X-Chain"));
        assertEquals("yes", call(PORTAL_PORT, "/.well-known/acme-challenge/abc", false).getHeader("X-Chain"));
        assertEquals("yes", call(PORTAL_PORT, "/api/portal/servers", true).getHeader("X-Chain"));
    }
}
