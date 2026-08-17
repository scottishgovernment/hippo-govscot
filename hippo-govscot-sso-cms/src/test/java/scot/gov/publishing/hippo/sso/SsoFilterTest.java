package scot.gov.publishing.hippo.sso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SsoFilter}: routing {@code /sso/*} GETs to {@link EndpointHandler},
 * and otherwise dispatching to the filter chain or {@link RedirectHandler} based on
 * {@link SsoRedirectPolicy}'s decision, plus the cookie/credential bookkeeping around that
 * decision. The decision logic itself is {@link SsoRedirectPolicyTest}'s concern, and the
 * mechanics of the redirect are {@link RedirectHandlerTest}'s.
 *
 * <p>{@code configured} is set to {@code true} in {@code setUp()} so that
 * {@code ensureConfigured()} is a no-op; {@code ssoRedirectPolicy} is assigned directly.
 */
class SsoFilterTest {

    private SsoFilter sut;
    private EndpointHandler endpointHandler;
    private RedirectHandler redirectHandler;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private HttpSession session;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        sut = new SsoFilter();
        sut.configured = true;
        endpointHandler = mock(EndpointHandler.class);
        redirectHandler = mock(RedirectHandler.class);
        sut.endpointHandler = endpointHandler;
        sut.redirectHandler = redirectHandler;

        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        chain = mock(FilterChain.class);

        when(req.getMethod()).thenReturn("GET");
        when(req.getContextPath()).thenReturn("");
        when(req.getRequestURI()).thenReturn("/cms/");
        when(req.getQueryString()).thenReturn(null);
    }

    private void ssoConfig(SsoConfig.Mode mode, SsoConfig.Redirect redirect, SsoConfig.Form form) {
        sut.ssoRedirectPolicy = new SsoRedirectPolicy(new SsoConfig(mode, redirect, form));
    }

    @Test
    void ssoPathRoutesToEndpointHandler() throws Exception {
        when(req.getRequestURI()).thenReturn("/sso/enable");

        sut.doFilter(req, resp, chain);

        verify(endpointHandler).handle(req, resp);
        verify(chain, never()).doFilter(any(), any());
        verifyNoInteractions(redirectHandler);
    }

    /**
     * Regression test: only GET requests to /sso/* are treated as endpoint requests — a
     * POST must fall through to the redirect decision instead of being routed (and
     * mis-parsed) by EndpointHandler.
     */
    @Test
    void nonGetSsoPathIsNotTreatedAsEndpoint() throws Exception {
        when(req.getRequestURI()).thenReturn("/sso/callback");
        when(req.getMethod()).thenReturn("POST");
        ssoConfig(SsoConfig.Mode.OFF, SsoConfig.Redirect.MANUAL, SsoConfig.Form.NATIVE);

        sut.doFilter(req, resp, chain);

        verifyNoInteractions(endpointHandler);
        verify(chain).doFilter(req, resp);
    }

    @Test
    void passThroughCallsChain() throws Exception {
        ssoConfig(SsoConfig.Mode.OFF, SsoConfig.Redirect.MANUAL, SsoConfig.Form.NATIVE);

        sut.doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
        verifyNoInteractions(redirectHandler);
    }

    @Test
    void redirectRequiredCallsRedirectHandler() throws Exception {
        ssoConfig(SsoConfig.Mode.REQUIRED, SsoConfig.Redirect.AUTO, SsoConfig.Form.SSO);

        sut.doFilter(req, resp, chain);

        verify(redirectHandler).redirect(req, resp);
        verify(chain, never()).doFilter(any(), any());
    }

    /**
     * CallbackHandler stores credentials in a fresh session after IdP authentication; once
     * the request is allowed to pass through, they must be copied to a request attribute so
     * Wicket can pick them up.
     */
    @Test
    void credentialsInSessionCopiedToRequestOnPassThrough() throws Exception {
        ssoConfig(SsoConfig.Mode.REQUIRED, SsoConfig.Redirect.AUTO, SsoConfig.Form.SSO);
        Object mockCreds = mock(Object.class);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute(SsoSessionAttributes.CREDENTIALS)).thenReturn(mockCreds);

        sut.doFilter(req, resp, chain);

        verify(req).setAttribute(SsoSessionAttributes.CREDENTIALS, mockCreds);
        verify(chain).doFilter(req, resp);
    }

    /**
     * An authenticated CMS session (hippo:username set) always clears a stale logged-out
     * cookie, regardless of sso.redirect, so a later logout is not immediately suppressed
     * by a leftover cookie from a previous session.
     */
    @Test
    void authenticatedSessionClearsLoggedOutCookie() throws Exception {
        ssoConfig(SsoConfig.Mode.REQUIRED, SsoConfig.Redirect.ONCE, SsoConfig.Form.SSO);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("hippo:username")).thenReturn("someuser");
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.LOGGED_OUT_COOKIE_NAME, "true")});

        sut.doFilter(req, resp, chain);

        verify(resp).addCookie(argThat(c ->
                c.getName().equals(SsoCookies.LOGGED_OUT_COOKIE_NAME) && c.getMaxAge() == 0));
        verify(chain).doFilter(req, resp);
    }

}
