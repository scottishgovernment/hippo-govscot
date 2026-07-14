package scot.gov.publishing.hippo.sso.frontend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.logout.CmsLogoutService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.hippo.sso.SsoFilter;

/**
 * Sets a cookie when the user logs out, so that SsoRedirectFilter can prevent the user
 * from being automatically logged back in (sso.redirect=ONCE only).
 *
 * <p>A cookie is used rather than a session attribute because {@link #logoutSession()}
 * invalidates the current session via Wicket's {@code Session.invalidate()}, which is
 * deferred to the end of the request — so a "logged out" flag set on a session obtained
 * afterwards would be destroyed along with it before the browser sees the next request.
 *
 * <p>The cookie is added directly to the underlying servlet response rather than via
 * Wicket's {@code WebResponse}. {@link CmsLogoutService#logout()} calls this method and
 * then {@code redirectPage()}, which throws a {@code RestartResponseException}. Wicket
 * handles that by calling {@code response.reset()} before rendering the redirect, which
 * discards anything buffered on the Wicket-level response (including a cookie added via
 * {@code WebResponse.addCookie()}) — it deliberately never resets the real servlet
 * response, to avoid also wiping the JSESSIONID cookie, so writing to the servlet
 * response directly survives that reset.
 */
@SuppressWarnings("unused")
public class SsoLogoutService extends CmsLogoutService {

    private static final Logger LOG = LoggerFactory.getLogger(SsoLogoutService.class);

    public SsoLogoutService(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected void logoutSession() {
        super.logoutSession();

        RequestCycle requestCycle = RequestCycle.get();
        HttpServletRequest req = (HttpServletRequest) requestCycle.getRequest().getContainerRequest();
        HttpServletResponse res = (HttpServletResponse) requestCycle.getResponse().getContainerResponse();
        res.addCookie(SsoFilter.loggedOutCookie(req.isSecure()));
    }

}
