package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.MDC;

/**
 * Adds the request URL to the logging MDC.
 */
public class LoggingValve extends AbstractOrderableValve {

    public static final String URL_PROPERTY = "url";

    @Override
    public void invoke(ValveContext valveContext) throws ContainerException {
        try {
            setupMDC(valveContext);
            valveContext.invokeNext();
        } finally {
            MDC.remove(URL_PROPERTY);
        }
    }

    private static void setupMDC(ValveContext valveContext) {
        HttpServletRequest request = valveContext.getServletRequest();
        String url = HstRequestUtils.getExternalRequestUrl(request, true);
        MDC.put(URL_PROPERTY, url);
    }

}
