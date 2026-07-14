package scot.gov.publishing.hippo.sso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.OFF;
import static scot.gov.publishing.hippo.sso.SsoConfig.Mode.REQUIRED;
import static scot.gov.publishing.hippo.sso.SsoConfig.Redirect.MANUAL;

public class SsoRedirectFilter extends HttpFilter {

    public static final String CREDENTIALS_ATTR_NAME = SsoSessionAttributes.CREDENTIALS;

    /**
     * Session attribute set on successful login by Bloomreach.
     * This is used to detect whether a user is already authenticated.
     */
    private static final String HIPPO_USERNAME_ATTR_NAME = "hippo:username";

    private static final Logger LOG = LoggerFactory.getLogger(SsoRedirectFilter.class);

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

    transient SsoConfig ssoConfig;
    transient RedirectHandler redirectHandler;
    boolean configured = false;

    private synchronized void ensureConfigured() {
        if (configured) {
            return;
        }
        this.ssoConfig = SsoConfig.get();

        // Early return if SSO is not enabled
        if (OFF == this.ssoConfig.mode()) {
            this.configured = true;
            return;
        }

        this.redirectHandler = new RedirectHandler(OidcConfig.get());
        this.configured = true;
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ensureConfigured();

        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        String requestUrl = queryString == null ? requestUri : requestUri + "?" + queryString;
        LOG.debug("SsoRedirectFilter - {} {}", request.getMethod(), requestUrl);

        HttpSession session = request.getSession(false);
        if (hasJustReauthenticated(request, session)) {
            clearLoggedOutCookie(request, response);
        }

        if (passThrough(request, session)) {
            copySessionCredentialsToRequestIfPresent(session, request);
            super.doFilter(request, response, chain);
            return;
        }

        session = request.getSession(true);
        session.setAttribute(SsoSessionAttributes.RETURN_URL, requestUrl);
        String url = redirectHandler.buildRedirectUrl(session);
        LOG.info("Redirecting from {}", requestUrl);
        LOG.info("Redirecting to {}", url);
        response.sendRedirect(url);
    }

    private boolean passThrough(HttpServletRequest request, HttpSession session) {
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
        if (isLogOutPermitted() && isLogoutRequested(request)) {
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
        return hasAttribute(session, CREDENTIALS_ATTR_NAME)
                || hasAttribute(session, HIPPO_USERNAME_ATTR_NAME);
    }

    private static boolean isSsoLoginRequested(HttpServletRequest request, HttpSession session) {
        return isSsoLoginRequestedByUI(session) || isSsoLoginRequestedByCookie(request);
    }

    private static boolean isSsoLoginRequestedByUI(HttpSession session) {
        return session != null && session.getAttribute(SsoSessionAttributes.SSO) != null;
    }

    private static boolean isSsoLoginRequestedByCookie(HttpServletRequest request) {
        return getBooleanCookie(request, SsoFilter.SSO_COOKIE_NAME).orElse(false);
    }

    private static boolean isSsoLoginSuppressedByCookie(HttpServletRequest request) {
        return !getBooleanCookie(request, SsoFilter.SSO_COOKIE_NAME).orElse(true);
    }

    private static boolean hasPendingError(HttpSession session) {
        return hasAttribute(session, SsoSessionAttributes.SSO_ERROR)
                || hasAttribute(session, SsoSessionAttributes.CALLBACK_ERROR);
    }

    private boolean isLogOutPermitted() {
        return ssoConfig.redirect() != SsoConfig.Redirect.AUTO;
    }

    private static boolean isLogoutRequested(HttpServletRequest request) {
        return getBooleanCookie(request, SsoFilter.LOGGED_OUT_COOKIE_NAME).orElse(false);
    }

    private boolean isSsoDisabledByCookie(HttpServletRequest request, HttpSession session) {
        return ssoConfig.mode() != REQUIRED
                && !isSsoLoginRequestedByUI(session)
                && isSsoLoginSuppressedByCookie(request);
    }

    /**
     * Returns true if the user is authenticated but has a stale logged_out cookie.
     */
    private static boolean hasJustReauthenticated(HttpServletRequest request, HttpSession session) {
        return hasAttribute(session, HIPPO_USERNAME_ATTR_NAME) && isLogoutRequested(request);
    }

    /**
     * Clears the logged-out cookie whenever. Without this, a stale logged-out cookie from a
     * previous session could suppress the IdP redirect.
     */
    private static void clearLoggedOutCookie(HttpServletRequest request, HttpServletResponse response) {
        response.addCookie(SsoFilter.clearLoggedOutCookie(request.isSecure()));
    }

    private static boolean isPublic(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith)
                || EXCLUDED_PATHS.contains(path);
    }

    private static Optional<Boolean> getBooleanCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isEmpty(cookies)) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .map(Cookie::getValue)
                .map(BooleanUtils::toBoolean)
                .findFirst();
    }

    /**
     * Copy credentials from session to request, if present.
     * CallbackHandler stores credentials in a fresh session after IdP authentication.
     * These need to be copied to a request attribute.
     */
    private void copySessionCredentialsToRequestIfPresent(HttpSession session, HttpServletRequest request) {
        if (session == null) {
            return;
        }
        Object credentials = session.getAttribute(CREDENTIALS_ATTR_NAME);
        if (credentials != null) {
            request.setAttribute(CREDENTIALS_ATTR_NAME, credentials);
        }
    }

    private static boolean hasAttribute(HttpSession session, String name) {
        return session != null && session.getAttribute(name) != null;
    }

}
