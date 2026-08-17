package scot.gov.publishing.hippo.sso;

import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.hippoecm.hst.util.HstRequestUtils;

import java.net.URI;
import java.util.function.Function;

public class OidcRedirectHandler {

    /**
     * The OIDC callback path routed to {@link CallbackHandler} below, relative to the
     * context path.
     */
    public static final String CALLBACK_PATH = "/sso/callback";

    private static final Scope SCOPE = new Scope("openid", "profile", "email");

    private final OidcConfig oidcConfig;

    // Package-private so tests can substitute a stub, avoiding a dependency on the
    // hst:platform model that HstRequestUtils.getCmsBaseURL needs.
    Function<HttpServletRequest, String> getCmsBaseUrl = HstRequestUtils::getCmsBaseURL;

    public OidcRedirectHandler(OidcConfig oidcConfig) {
        this.oidcConfig = oidcConfig;
    }

    public String buildRedirectUrl(HttpServletRequest request, HttpSession session) {
        State state = new State();
        Nonce nonce = new Nonce();
        CodeVerifier codeVerifier = new CodeVerifier();
        URI redirectUri = resolveRedirectUri(request);
        session.setAttribute(SsoSessionAttributes.CODE_VERIFIER, codeVerifier);
        session.setAttribute(SsoSessionAttributes.STATE, state);
        session.setAttribute(SsoSessionAttributes.NONCE, nonce);
        session.setAttribute(SsoSessionAttributes.REDIRECT_URI, redirectUri);

        AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                SCOPE,
                oidcConfig.clientId(),
                redirectUri)
                .endpointURI(oidcConfig.authorizationEndpoint())
                .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
                .state(state)
                .responseType(ResponseType.CODE)
                .nonce(nonce)
                .build();

        return authRequest.toURI().toString();
    }

    /**
     * Resolves the OIDC redirect_uri to use for this request: the CMS's external base URL
     * plus CALLBACK_PATH, the fixed path {@code SsoFilter} routes to
     * {@link CallbackHandler}, via {@link EndpointHandler}.
     *
     * <p>Production is reachable via both a stable public hostname and an environment-specific
     * hostname (e.g. before an environment goes live), so a fixed redirect_uri can't serve both —
     * it must always point back to whichever host the login started on.
     *
     * <p>{@code getCmsBaseURL} (rather than the request's scheme/host directly) is used because
     * the reverse proxy in front of the CMS strips the context path, and this correctly
     * determines from the hst:platform model whether the context path belongs in the URL — the
     * same approach the resetpassword-cms forge plugin already relies on for its callback links.
     */
    private URI resolveRedirectUri(HttpServletRequest request) {
        return URI.create(getCmsBaseUrl.apply(request) + CALLBACK_PATH);
    }
}
