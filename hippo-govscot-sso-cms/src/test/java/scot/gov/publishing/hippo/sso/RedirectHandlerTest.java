package scot.gov.publishing.hippo.sso;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RedirectHandler}: issuing the redirect to the IdP once
 * {@code SsoFilter} has already decided one is required. Whether a redirect is required at
 * all is {@link SsoRedirectPolicyTest}'s concern.
 *
 * <p>{@code configured} is set to {@code true} in {@code setUp()} so that
 * {@code ensureConfigured()} is a no-op; {@code oidcRedirectHandler} is assigned directly,
 * avoiding any HST/Hippo infrastructure.
 */
class RedirectHandlerTest {

    private RedirectHandler sut;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        sut = new RedirectHandler();
        sut.configured = true;
        sut.oidcRedirectHandler = newOidcRedirectHandler();

        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        when(req.getContextPath()).thenReturn("");
        when(req.getRequestURI()).thenReturn("/cms/");
        when(req.getQueryString()).thenReturn(null);
        when(req.getSession(true)).thenReturn(session);
    }

    /**
     * Minimal {@link OidcConfig} suitable for redirect-path tests; the only URI that matters
     * for assertions is {@code authorizationEndpoint}.
     */
    private OidcConfig testOidcConfig() {
        return new OidcConfig(
                "https://idp.example.com",
                URI.create("https://idp.example.com/auth"),
                URI.create("https://idp.example.com/jwks"),
                URI.create("https://idp.example.com/token"),
                URI.create("https://idp.example.com/userinfo"),
                new ClientID("test-client"),
                () -> new ClientSecretBasic(new ClientID("test-client"), new Secret("secret")),
                new JWKSet()
        );
    }

    /**
     * A {@link OidcRedirectHandler} with its {@code getCmsBaseUrl} stubbed, avoiding a dependency
     * on the hst:platform model that {@code HstRequestUtils.getCmsBaseURL} needs.
     */
    private OidcRedirectHandler newOidcRedirectHandler() {
        OidcRedirectHandler oidcRedirectHandler = new OidcRedirectHandler(testOidcConfig());
        oidcRedirectHandler.getCmsBaseUrl = request -> "https://cms.publishing.gov.scot/";
        return oidcRedirectHandler;
    }

    @Test
    void redirectsToIdPAuthorizationEndpoint() throws Exception {
        sut.redirect(req, resp);

        verify(resp).sendRedirect(argThat((String url) -> url.startsWith("https://idp.example.com/auth")));
    }

    @Test
    void setsOidcSessionAttributesForCallbackValidation() throws Exception {
        sut.redirect(req, resp);

        verify(session).setAttribute(eq(SsoSessionAttributes.STATE), any());
        verify(session).setAttribute(eq(SsoSessionAttributes.NONCE), any());
        verify(session).setAttribute(eq(SsoSessionAttributes.CODE_VERIFIER), any());
    }

    @Test
    void setsReturnUrlOnSessionFromRequestUriAndQueryString() throws Exception {
        when(req.getRequestURI()).thenReturn("/cms/some/path");
        when(req.getQueryString()).thenReturn("0");

        sut.redirect(req, resp);

        verify(session).setAttribute(SsoSessionAttributes.RETURN_URL, "/cms/some/path?0");
    }

    @Test
    void redirectUrlContainsResponseTypeCodeAndStateParam() throws Exception {
        sut.redirect(req, resp);

        verify(resp).sendRedirect(argThat((String url) ->
                url.contains("response_type=code") && url.contains("state=")));
    }

}
