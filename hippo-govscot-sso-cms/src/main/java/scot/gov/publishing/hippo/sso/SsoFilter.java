package scot.gov.publishing.hippo.sso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Serves /sso/* endpoints and redirects other requests to an IdP if required.
 *
 * <p>Routes {@code /sso/*} GET requests to {@link EndpointHandler}. For every other request,
 * decides via {@link SsoRedirectPolicy} whether it must first be authenticated by the IdP —
 * either continuing the filter chain, or issuing the redirect via {@link RedirectHandler}.
 */
public class SsoFilter extends HttpFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SsoFilter.class);

    EndpointHandler endpointHandler = new EndpointHandler();

    RedirectHandler redirectHandler = new RedirectHandler();

    transient SsoRedirectPolicy ssoRedirectPolicy;

    boolean configured = false;

    private synchronized void ensureConfigured() {
        if (configured) {
            return;
        }
        this.ssoRedirectPolicy = new SsoRedirectPolicy(SsoConfig.get());
        this.configured = true;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        logRequest(req);

        String path = pathInContext(req);
        if (path.startsWith("/sso/") && "GET".equals(req.getMethod())) {
            endpointHandler.handle(req, res);
            return;
        }

        ensureConfigured();

        HttpSession session = req.getSession(false);
        if (hasJustReauthenticated(req, session)) {
            clearLoggedOutCookie(req, res);
        }

        if (ssoRedirectPolicy.requiresRedirect(req, session)) {
            redirectHandler.redirect(req, res);
            return;
        }

        copySessionCredentialsToRequestIfPresent(session, req);
        chain.doFilter(req, res);
    }

    private static String pathInContext(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        String requestURI = req.getRequestURI();
        return requestURI.substring(contextPath.length());
    }

    private static void logRequest(HttpServletRequest req) {
        if (LOG.isDebugEnabled()) {
            String requestURI = req.getRequestURI();
            String queryString = req.getQueryString();
            String requestUrl = queryString == null ? requestURI : requestURI + "?" + queryString;
            LOG.debug("SsoFilter - {} {}", req.getMethod(), requestUrl);
        }
    }

    /**
     * Returns true if the user is authenticated but has a stale logged_out cookie.
     */
    private static boolean hasJustReauthenticated(HttpServletRequest request, HttpSession session) {
        return hasAttribute(session, SsoRedirectPolicy.HIPPO_USERNAME_ATTR_NAME)
                && SsoCookies.isLogoutRequested(request);
    }

    /**
     * Clears the logged-out cookie whenever. Without this, a stale logged-out cookie from a
     * previous session could suppress the IdP redirect.
     */
    private static void clearLoggedOutCookie(HttpServletRequest request, HttpServletResponse response) {
        response.addCookie(EndpointHandler.clearLoggedOutCookie(request.isSecure()));
    }

    /**
     * Copy credentials from session to request, if present.
     * CallbackHandler stores credentials in a fresh session after IdP authentication.
     * These need to be copied to a request attribute.
     */
    private static void copySessionCredentialsToRequestIfPresent(HttpSession session, HttpServletRequest request) {
        if (session == null) {
            return;
        }
        Object credentials = session.getAttribute(SsoSessionAttributes.CREDENTIALS);
        if (credentials != null) {
            request.setAttribute(SsoSessionAttributes.CREDENTIALS, credentials);
        }
    }

    private static boolean hasAttribute(HttpSession session, String name) {
        return session != null && session.getAttribute(name) != null;
    }

}
