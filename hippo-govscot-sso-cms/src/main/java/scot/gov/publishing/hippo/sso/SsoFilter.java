package scot.gov.publishing.hippo.sso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Provides various endpoints for the SSO integration including the callback endpoint.
 */
public class SsoFilter extends HttpFilter {

    public static final String SSO_COOKIE_NAME = "sso";

    /**
     * Cookie name used to indicate that the user has logged out.
     * This allows the redirect filter to avoid immediately redirecting the user
     * back to the IdP when not required (sso.redirect=ONCE).
     */
    public static final String LOGGED_OUT_COOKIE_NAME = "logged_out";

    private final CallbackHandler callbackHandler = new CallbackHandler();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String contextPath = req.getContextPath();
        String requestURI = req.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        if (!path.startsWith("/sso") || !"GET".equals(req.getMethod())) {
            super.doFilter(req, res, chain);
            return;
        }
        String[] splits = path.split("/", 3);
        if (splits.length < 3) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String action = splits[2];
        switch (action) {
            case "callback":
                callbackHandler.handleRequest(req, res);
                break;
            case "enable":
                enableSSOCookie(req, res);
                break;
            case "disable":
                disableSSOCookie(req, res);
                break;
            case "reset":
                removeSSOCookie(req, res);
                break;
            case "jwks":
                serveJwks(res);
                break;
            case "login":
                performSSOLogin(req, res);
                break;
            default:
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }

    /**
     * Endpoint to return the public key if key-based authentication is enabled.
     * This can be copied into the IdP dashboard.
     */
    private void serveJwks(HttpServletResponse res) throws IOException, ServletException {
        try {
            OidcConfig oidcConfig = OidcConfig.get();
            res.setContentType("application/json");
            res.getWriter().write(oidcConfig.publicJwks().toString());
        } catch (Exception ex) {
            throw new ServletException("Failed to serve JWKS", ex);
        }
    }

    /**
     * Sets a cookie to enable SSO authentication when use of SSO is optional.
     */
    private void enableSSOCookie(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.addCookie(createSsoCookie(req.isSecure(), true));
        sendRedirect(req, res);
    }

    /**
     * Sets a cookie to disable SSO authentication when use of SSO is optional.
     */
    private void disableSSOCookie(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.addCookie(createSsoCookie(req.isSecure(), false));
        sendRedirect(req, res);
    }

    /**
     * Deletes any cookies used to control SSO authentication.
     */
    private void removeSSOCookie(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.addCookie(createCookie(SSO_COOKIE_NAME, "", req.isSecure(), 0));
        res.addCookie(clearLoggedOutCookie(req.isSecure()));
        sendRedirect(req, res);
    }

    private void performSSOLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession s = req.getSession(true);
        s.setAttribute(SsoSessionAttributes.SSO, true);
        // Clear stale credentials from a previous SSO attempt that ended with
        // "user not found". If left in the session, OidcLoginFilter would see
        // them and pass through rather than redirecting to the IdP.
        s.removeAttribute(SsoSessionAttributes.CREDENTIALS);
        res.addCookie(clearLoggedOutCookie(req.isSecure()));
        sendRedirect(req, res);
    }

    private static void sendRedirect(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setHeader("Cache-Control", "no-cache");
        res.sendRedirect("..");
    }

    private static Cookie createSsoCookie(boolean secure, boolean enable) {
        return createCookie(SSO_COOKIE_NAME, Boolean.toString(enable), secure, -1);
    }

    /**
     * Sets the logged-out cookie, read by SsoRedirectFilter to suppress an immediate
     * auto-redirect back to the IdP after logout (sso.redirect=ONCE only).
     */
    public static Cookie loggedOutCookie(boolean secure) {
        return createCookie(LOGGED_OUT_COOKIE_NAME, Boolean.toString(true), secure, -1);
    }

    /**
     * Clears the logged-out cookie, e.g. when the user initiates a fresh login.
     */
    public static Cookie clearLoggedOutCookie(boolean secure) {
        return createCookie(LOGGED_OUT_COOKIE_NAME, "", secure, 0);
    }

    private static Cookie createCookie(String name, String value, boolean secure, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(secure);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }

}
