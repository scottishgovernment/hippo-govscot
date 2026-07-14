package scot.gov.publishing.hippo.sso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
        if (SsoConfig.Mode.OFF == this.ssoConfig.mode()) {
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

        // Only redirect GET requests to the IdP. Redirecting other requests
        // would cause the request body to be lost.
        if (!"GET".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Always propagate credentials from session to request, regardless of redirect mode.
        // In REQUIRED+MANUAL and OPTIONAL+MANUAL the filter does not auto-redirect, but
        // CallbackHandler still stores credentials in a fresh session after IdP authentication.
        // Without this check those credentials would be silently dropped.
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            Object creds = existingSession.getAttribute(CREDENTIALS_ATTR_NAME);
            if (creds != null) {
                request.setAttribute(CREDENTIALS_ATTR_NAME, creds);
                chain.doFilter(request, response);
                return;
            }
        }

        if (!requiresIdpRedirect(request)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SsoSessionAttributes.RETURN_URL, requestUrl);
        String url = redirectHandler.buildRedirectUrl(session);
        LOG.info("Redirecting from {}", requestUrl);
        LOG.info("Redirecting to {}", url);
        response.sendRedirect(url);
    }

    private boolean requiresIdpRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean sessionAttr = session != null && session.getAttribute(SsoSessionAttributes.SSO) != null;
        boolean sso = switch (ssoConfig.mode()) {
            case OFF -> false;
            case REQUIRED -> ssoConfig.redirect() != SsoConfig.Redirect.MANUAL;
            case OPTIONAL -> sessionAttr || cookiePreference(request);
        };
        // The logged-out cookie only suppresses redirects for ONCE; AUTO redirects
        // unconditionally, ignoring it.
        boolean suppressedByLogout = ssoConfig.redirect() == SsoConfig.Redirect.ONCE && isLoggedOut(request);
        return sso && !isExcluded(request) && !suppressedByLogout;
    }

    private static boolean isLoggedOut(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isEmpty(cookies)) {
            return false;
        }
        return Arrays.stream(cookies)
                .filter(c -> SsoFilter.LOGGED_OUT_COOKIE_NAME.equalsIgnoreCase(c.getName()))
                .map(Cookie::getValue)
                .anyMatch(BooleanUtils::toBoolean);
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
        if (!isLoggedOut(request)) {
            return;
        }
        response.addCookie(SsoFilter.clearLoggedOutCookie(request.isSecure()));
    }

    private static boolean isExcluded(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith)
                || EXCLUDED_PATHS.contains(path);
    }

    private boolean cookiePreference(HttpServletRequest request) {
        boolean systemDefault = ssoConfig.redirect() != SsoConfig.Redirect.MANUAL;
        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isEmpty(cookies)) {
            return systemDefault;
        }
        return Arrays.stream(cookies)
                .filter(c -> SsoFilter.SSO_COOKIE_NAME.equalsIgnoreCase(c.getName()))
                .map(Cookie::getValue)
                .map(BooleanUtils::toBoolean)
                .findFirst()
                .orElse(systemDefault);
    }

}
