package scot.gov.publishing.hippo.sso;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Provides various endpoints for the SSO integration including the callback endpoint, and
 * otherwise defers to {@link RedirectHandler} to decide whether the request must first be
 * authenticated by the IdP.
 */
public class SsoFilter extends HttpFilter {

    private EndpointHandler endpointHandler = new EndpointHandler();

    private RedirectHandler redirectHandler = new RedirectHandler();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String contextPath = req.getContextPath();
        String requestURI = req.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        if (path.startsWith("/sso/") && "GET".equals(req.getMethod())) {
            endpointHandler.handle(req, res);
            return;
        }
        redirectHandler.handle(req, res, chain);
    }

}
