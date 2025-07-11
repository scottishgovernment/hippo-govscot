package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.PageCacheKey;
import org.hippoecm.hst.core.container.ValveContext;

/**
 * Adds the value of the X-User-Type header to the page cache key.
 * This valve also makes the value available as a request attribute.
 */
public class UserTypeValve extends AbstractOrderableValve {

    public static final String USERTYPE_REQUEST_ATTR_NAME =
            UserTypeValve.class.getName() + ".userType";

    static final String USERTYPE_CACHE_ATTR_NAME =
            UserTypeValve.class.getName() + ".userType";

    private static final String USERTYPE_HEADER = "X-User-Type";

    @Override
    public void invoke(ValveContext valveContext) throws ContainerException {
        addUserTypeToCacheKey(valveContext);
        valveContext.invokeNext();
    }

    private void addUserTypeToCacheKey(ValveContext valveContext) {
        PageCacheKey pageCacheKey = valveContext
                .getPageCacheContext()
                .getPageCacheKey();

        HttpServletRequest request = valveContext.getServletRequest();
        String userType = request.getHeader(USERTYPE_HEADER);
        if (userType != null) {
            request.setAttribute(USERTYPE_REQUEST_ATTR_NAME, userType);
            pageCacheKey.setAttribute(USERTYPE_CACHE_ATTR_NAME, userType);
        }
    }

}
