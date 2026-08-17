package scot.gov.publishing.hippo.sso;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static scot.gov.publishing.hippo.sso.SsoConfig.Form.NATIVE;
import static scot.gov.publishing.hippo.sso.SsoConfig.Form.REVEAL;
import static scot.gov.publishing.hippo.sso.SsoConfig.Form.SSO;
import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.OFF;
import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.OPTIONAL;
import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.REQUIRED;
import static scot.gov.publishing.hippo.sso.SsoConfig.Redirect.AUTO;
import static scot.gov.publishing.hippo.sso.SsoConfig.Redirect.MANUAL;
import static scot.gov.publishing.hippo.sso.SsoConfig.Redirect.ONCE;

/**
 * Unit tests for {@link SsoRedirectPolicy#passThrough}: whether a request may proceed as-is,
 * or must first be sent to the IdP to authenticate. Purely the decision — issuing the actual
 * redirect is {@link RedirectHandlerTest}'s concern, and dispatch/cookie/credential bookkeeping
 * around the decision is {@link SsoFilterTest}'s.
 */
class SsoRedirectPolicyTest {

    private HttpServletRequest req;

    @BeforeEach
    void setUp() {
        req = mock(HttpServletRequest.class);

        // Defaults: GET request with a non-excluded URI and no context-path prefix.
        when(req.getMethod()).thenReturn("GET");
        when(req.getContextPath()).thenReturn("");
        when(req.getRequestURI()).thenReturn("/cms/");
    }

    @Test
    void offModePassesThrough() {
        SsoConfig ssoConfig = new SsoConfig(OFF, MANUAL, NATIVE);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    @Test
    void postRequestPassesThrough() {
        when(req.getMethod()).thenReturn("POST");
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    @Test
    void excludedPrefixSkinPassesThrough() {
        when(req.getRequestURI()).thenReturn("/skin/logo.png");
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    @Test
    void excludedPrefixSsoCallbackPassesThrough() {
        when(req.getRequestURI()).thenReturn("/sso/callback");
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    @Test
    void excludedExactPathNavigationItemsPassesThrough() {
        when(req.getRequestURI()).thenReturn("/ws/navigationitems");
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    @Test
    void redirectOnceWithLoggedOutCookiePassesThrough() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.LOGGED_OUT_COOKIE_NAME, "true")});
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, ONCE, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    @Test
    void redirectOnceWithoutLoggedOutCookieRequiresRedirect() {
        when(req.getCookies()).thenReturn(null);
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, ONCE, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, null));
    }

    /**
     * AUTO ignores the logged-out cookie entirely — logging out and browsing again logs
     * the user straight back in.
     */
    @Test
    void redirectAutoWithLoggedOutCookieStillRequiresRedirect() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.LOGGED_OUT_COOKIE_NAME, "true")});
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, null));
    }

    @Test
    void optionalModeWithSsoCookieFalsePassesThrough() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "false")});
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, MANUAL, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    /**
     * A user who visited /sso/disable (cookie sso=false) must have auto-redirect
     * suppressed even in AUTO mode, not just MANUAL — the opt-out cookie is a general
     * override, not one scoped to MANUAL's opt-in behaviour.
     */
    @Test
    void optionalModeAutoWithSsoCookieFalsePassesThrough() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "false")});
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, AUTO, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    /**
     * Same opt-out cookie override as above, but for ONCE mode.
     */
    @Test
    void optionalModeOnceWithSsoCookieFalsePassesThrough() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "false")});
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, ONCE, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    /**
     * REQUIRED mode never consults the sso preference cookie — it is only meaningful
     * when SSO is OPTIONAL. An sso=false cookie must not suppress the mandatory redirect.
     */
    @Test
    void requiredModeWithSsoCookieFalseStillRequiresRedirect() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "false")});
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, null));
    }

    /**
     * An explicit SSO login request (SsoSessionAttributes.SSO set via the login button)
     * must win over a stale sso=false opt-out cookie — the cookie is only a fallback
     * default, not a veto over an explicit request. Otherwise a user who previously hit
     * /sso/disable, then clicks "log in with SSO", would be silently kept on the
     * password form instead of being sent to the IdP.
     */
    @Test
    void optionalModeWithSsoSessionAttrOverridesSsoCookieFalseRequiresRedirect() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "false")});
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SsoSessionAttributes.SSO)).thenReturn(true);
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, AUTO, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, session));
    }

    @Test
    void optionalModeManualNoCookiePassesThrough() {
        when(req.getCookies()).thenReturn(null);
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, MANUAL, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, null));
    }

    /**
     * REQUIRED+AUTO redirects unconditionally by default, but a pending SSO_ERROR from a
     * just-completed callback (e.g. the IdP rejected the user) must suppress it — otherwise
     * the browser bounces straight back to the IdP, gets the same error, and loops forever
     * without the login page ever rendering.
     */
    @Test
    void requiredModeWithPendingSsoErrorPassesThrough() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SsoSessionAttributes.SSO_ERROR)).thenReturn("access_denied");
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, session));
    }

    /**
     * Same as above but for CALLBACK_ERROR (internal callback failures), which must suppress
     * the redirect just as SSO_ERROR does.
     */
    @Test
    void requiredModeWithPendingCallbackErrorPassesThrough() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SsoSessionAttributes.CALLBACK_ERROR)).thenReturn(true);
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, session));
    }

    /**
     * OPTIONAL with an sso=true preference cookie also redirects unconditionally, so the
     * same pending-error suppression must apply there too, not just in REQUIRED mode.
     */
    @Test
    void optionalModeWithSsoCookieTrueAndPendingSsoErrorPassesThrough() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "true")});
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SsoSessionAttributes.SSO_ERROR)).thenReturn("access_denied");
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, MANUAL, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, session));
    }

    @Test
    void credentialsInSessionCausesPassThrough() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SsoSessionAttributes.CREDENTIALS)).thenReturn(mock(Object.class));
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, session));
    }

    /**
     * A password-authenticated session (hippo:username set, no SSO CREDENTIALS attribute)
     * must never be redirected to the IdP, even when OPTIONAL mode's cookie/session-attribute
     * preference would otherwise default to auto-redirecting (sso.redirect=AUTO, no sso
     * cookie). Without checking hippo:username here, an already-logged-in password user would
     * be bounced straight back to the IdP on their very next request.
     */
    @Test
    void passwordAuthenticatedSessionWithAutoRedirectPassesThrough() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("hippo:username")).thenReturn("localadmin");
        when(req.getCookies()).thenReturn(null);
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, AUTO, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertTrue(sut.passThrough(req, session));
    }

    @Test
    void requiredModeRequiresRedirect() {
        SsoConfig ssoConfig = new SsoConfig(REQUIRED, AUTO, SSO);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, null));
    }

    @Test
    void optionalModeWithSsoSessionAttrRequiresRedirect() {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SsoSessionAttributes.SSO)).thenReturn("true");
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, MANUAL, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, session));
    }

    @Test
    void optionalModeWithSsoCookieTrueRequiresRedirect() {
        when(req.getCookies()).thenReturn(new Cookie[]{new Cookie(SsoCookies.SSO_COOKIE_NAME, "true")});
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, MANUAL, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, null));
    }

    @Test
    void optionalModeAutoNoCookieRequiresRedirect() {
        when(req.getCookies()).thenReturn(null);
        SsoConfig ssoConfig = new SsoConfig(OPTIONAL, AUTO, REVEAL);
        SsoRedirectPolicy sut = new SsoRedirectPolicy(ssoConfig);

        assertFalse(sut.passThrough(req, null));
    }

}
