package scot.gov.publishing.hippo.hst.request;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.MINUTES;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;

/**
 * Redirects URLs with upper-case letters to lower case URLs.
 */
public class CaseRedirectValve extends AbstractOrderableValve {

    private static final Logger LOG = LoggerFactory.getLogger(CaseRedirectValve.class);

    private static final Pattern CAPS = Pattern.compile("\\p{Upper}");

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        if (shouldRedirect(context.getServletRequest())) {
            redirect(context);
        } else {
            context.invokeNext();
        }
    }

    private boolean shouldRedirect(HttpServletRequest req) {
        return CAPS.matcher(contentPath(req)).find();
    }

    private String contentPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        return uri.substring(contextPath.length());
    }

    void redirect(ValveContext context) {
        HstRequestContext requestContext = context.getRequestContext();
        HttpServletRequest request = context.getServletRequest();
        HttpServletResponse response = context.getServletResponse();

        String path = contentPath(request);
        String lower = path.toLowerCase(Locale.UK);

        Mount mount = requestContext.getResolvedMount().getMount();
        String mountUrl = HstRequestUtils.createURLForMount(mount, request);
        String query = request.getQueryString();

        String fromUrl = format(mountUrl, path, query);
        String toUrl = format(mountUrl, lower, query);

        LOG.info("Redirecting to lower case URL: {}", fromUrl);
        long expires = Instant.now().plus(1, MINUTES).toEpochMilli();
        response.setStatus(SC_MOVED_PERMANENTLY);
        response.setHeader("Location", toUrl);
        response.setDateHeader("Expires", expires);
    }

    static String format(String mount, String path, String query) {
        int length = mount.length()
                + path.length()
                + (query == null ? 0 : query.length() + 1);
        StringBuilder url = new StringBuilder(length);
        url.append(mount);
        url.append(path);
        if (query != null) {
            url.append('?').append(query);
        }
        return url.toString();
    }

}
