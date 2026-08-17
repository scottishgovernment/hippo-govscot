package scot.gov.publishing.hippo.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Set;

import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.OFF;
import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.REQUIRED;
import static scot.gov.publishing.hippo.sso.SsoConfig.Redirect.MANUAL;

/**
 * Decides whether a request may pass straight through to the CMS webapp, or must first be
 * authenticated with the IdP. Purely a decision — {@link RedirectHandler} is responsible for
 * acting on it (issuing the redirect, or continuing the filter chain).
 */
public class SsoRedirectPolicy {

    /**
     * Session attribute set on successful login by Bloomreach.
     * This is used to detect whether a user is already authenticated.
     */
    static final String HIPPO_USERNAME_ATTR_NAME = "hippo:username";

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/angular/",
            "/ckeditor/",
            "/logging/",
            "/navapp-assets/",
            "/skin/",
            "/resetpassword",
            "/sso/",
            "/wicket/",
            "/ws/redirects"
    );

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/favicon.ico",
            "/ping/",
            "/ws/navigationitems",
            "/ws/indexexport"
    );

    private final SsoConfig ssoConfig;

    public SsoRedirectPolicy(SsoConfig ssoConfig) {
        this.ssoConfig = ssoConfig;
    }

    public boolean requiresRedirect(HttpServletRequest request, HttpSession session) {
        return !passThrough(request, session);
    }

    boolean passThrough(HttpServletRequest request, HttpSession session) {
        // Pass through requests without redirecting if ...

        // The request is not a GET request. Only redirect GET requests to the IdP.
        // Redirecting other requests would cause the request body to be lost.
        if (!"GET".equals(request.getMethod())) {
            return true;
        }

        // The SSO integration is disabled
        if (ssoConfig.mode() == OFF) {
            return true;
        }

        // The user is already authenticated by Bloomreach or the IdP
        if (isAuthenticated(session)) {
            return true;
        }

        // There are SSO errors that should be displayed by the application.
        // For example, if the user is authenticated with the IdP but is not
        // assigned to the application.
        if (hasPendingError(session)) {
            return true;
        }

        // The user logged out and should not be logged in automatically
        if (isLogOutPermitted() && SsoCookies.isLogoutRequested(request)) {
            return true;
        }

        // SSO login is manual and the user has not requested it
        if (ssoConfig.redirect() == MANUAL && !isSsoLoginRequested(request, session)) {
            return true;
        }

        // SSO is not required, not explicitly requested, and disabled by a cookie.
        if (isSsoDisabledByCookie(request, session)) {
            return true;
        }

        // The request isn't for a URL that requires authentication
        return isPublic(request);
    }

    /**
     * Returns true if the user is authenticated, either locally or with the IdP.
     * If the user is authenticated locally, there will be a username attribute
     * on the session. Otherwise, if they are authenticated with the IdP, their
     * credentials will be on the session.
     */
    private static boolean isAuthenticated(HttpSession session) {
        return hasAttribute(session, SsoSessionAttributes.CREDENTIALS)
                || hasAttribute(session, HIPPO_USERNAME_ATTR_NAME);
    }

    private static boolean isSsoLoginRequested(HttpServletRequest request, HttpSession session) {
        return isSsoLoginRequestedByUI(session) || SsoCookies.isSsoLoginRequested(request);
    }

    private static boolean isSsoLoginRequestedByUI(HttpSession session) {
        return session != null && session.getAttribute(SsoSessionAttributes.SSO) != null;
    }

    private static boolean hasPendingError(HttpSession session) {
        return hasAttribute(session, SsoSessionAttributes.SSO_ERROR)
                || hasAttribute(session, SsoSessionAttributes.CALLBACK_ERROR);
    }

    private boolean isLogOutPermitted() {
        return ssoConfig.redirect() != SsoConfig.Redirect.AUTO;
    }

    private boolean isSsoDisabledByCookie(HttpServletRequest request, HttpSession session) {
        return ssoConfig.mode() != REQUIRED
                && !isSsoLoginRequestedByUI(session)
                && SsoCookies.isSsoLoginSuppressed(request);
    }

    private static boolean isPublic(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith)
                || EXCLUDED_PATHS.contains(path);
    }

    private static boolean hasAttribute(HttpSession session, String name) {
        return session != null && session.getAttribute(name) != null;
    }

}
