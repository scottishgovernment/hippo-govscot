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

    SsoConfig ssoConfig;
    RedirectHandler redirectHandler;
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

        clearLoggedOutCookieIfAuthenticated(request, response);

        if (passThrough(request)) {
            super.doFilter(request, response, chain);
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SsoSessionAttributes.RETURN_URL, requestUrl);
        String url = redirectHandler.buildRedirectUrl(session);
        LOG.info("Redirecting from {}", requestUrl);
        LOG.info("Redirecting to {}", url);
        response.sendRedirect(url);
    }

    private boolean passThrough(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

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
        if (isAuthenticated(request)) {
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
        if (ssoConfig.redirect() == MANUAL && !isSSOLoginRequested(request)) {
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
     * Copies IdP credentials to the request if present.
     */
    private static boolean isAuthenticated(HttpServletRequest request) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession == null) {
            return false;
        }

        // Propagate credentials from session to request.
        // CallbackHandler stores credentials in a fresh session after IdP authentication.
        // These need to be copied to a request attribute.
        Object creds = existingSession.getAttribute(CREDENTIALS_ATTR_NAME);
        if (creds != null) {
            request.setAttribute(CREDENTIALS_ATTR_NAME, creds);
            return true;
        }

        // A user who has already authenticated never needs an IdP redirect.
        // Only SSO-authenticated sessions carry CREDENTIALS above - this avoids redirecting
        // users who logged in using a password.
        return existingSession.getAttribute(HIPPO_USERNAME_ATTR_NAME) != null;
    }

    private static boolean isSSOLoginRequested(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return isSsoLoginRequestedByUI(session) || isSSOLoginRequestedByCookie(request);
    }

    private static boolean isSsoLoginRequestedByUI(HttpSession session) {
        return session != null && session.getAttribute(SsoSessionAttributes.SSO) != null;
    }

    private static boolean isSSOLoginRequestedByCookie(HttpServletRequest request) {
        return getBooleanCookie(request, SsoFilter.SSO_COOKIE_NAME).orElse(false);
    }

    private static boolean isSsoLoginSuppressedByCookie(HttpServletRequest request) {
        return !getBooleanCookie(request, SsoFilter.SSO_COOKIE_NAME).orElse(true);
    }

    private static boolean hasPendingError(HttpSession session) {
        return session != null
                && (session.getAttribute(SsoSessionAttributes.SSO_ERROR) != null
                || session.getAttribute(SsoSessionAttributes.CALLBACK_ERROR) != null);
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
     * Clears the logged-out cookie whenever the request carries an authenticated CMS
     * session, regardless of how the user logged in (password or SSO) or the current
     * sso.redirect value. Without this, a stale logged-out cookie from a previous ONCE
     * session (or a manual visit) would keep suppressing auto-redirect after the user has
     * since logged in again.
     */
    private static void clearLoggedOutCookieIfAuthenticated(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(HIPPO_USERNAME_ATTR_NAME) == null) {
            return;
        }
        if (!isLogoutRequested(request)) {
            return;
        }
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

}
