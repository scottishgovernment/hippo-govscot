package scot.gov.publishing.hippo.sso;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Issues the redirect to the IdP once {@code SsoFilter} has already decided, via
 * {@link SsoRedirectPolicy}, that a request must be authenticated before it can proceed.
 */
public class RedirectHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RedirectHandler.class);

    transient OidcRedirectHandler oidcRedirectHandler;

    boolean configured = false;

    private synchronized void ensureConfigured() {
        if (configured) {
            return;
        }
        this.oidcRedirectHandler = new OidcRedirectHandler(OidcConfig.get());
        this.configured = true;
    }

    public void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ensureConfigured();

        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        String requestUrl = queryString == null ? requestUri : requestUri + "?" + queryString;

        HttpSession session = request.getSession(true);
        session.setAttribute(SsoSessionAttributes.RETURN_URL, requestUrl);
        String url = oidcRedirectHandler.buildRedirectUrl(request, session);
        LOG.info("Redirecting from {}", requestUrl);
        LOG.info("Redirecting to {}", url);
        response.sendRedirect(url);
    }

}
