package scot.gov.publishing.hippo.hst.request;

import org.hippoecm.hst.core.container.PageCacheContext;
import org.hippoecm.hst.core.container.PageCacheKey;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class UserTypeValveTest {

    private UserTypeValve valve = new UserTypeValve();

    private ValveContext context = mock(ValveContext.class);

    private PageCacheContext pageCacheContext= mock(PageCacheContext.class);

    private PageCacheKey pageCacheKey = mock(PageCacheKey.class);

    private MockHttpServletRequest req = new MockHttpServletRequest();

    @Before
    public void setUp() {
        when(context.getServletRequest()).thenReturn(req);
        when(context.getPageCacheContext()).thenReturn(pageCacheContext);
        when(pageCacheContext.getPageCacheKey()).thenReturn(pageCacheKey);
    }

    @Test
    public void handlesInternalTraffic() throws Exception {
        req.addHeader("X-User-Type", "internal");
        valve.invoke(context);
        InOrder inOrder = inOrder(pageCacheKey, context);
        // Sets cache key
        inOrder.verify(pageCacheKey).setAttribute(
                eq(UserTypeValve.USERTYPE_CACHE_ATTR_NAME),
                eq("internal"));
        inOrder.verify(context).invokeNext();
        // Sets request attribute
        assertThat(req.getAttribute(UserTypeValve.USERTYPE_REQUEST_ATTR_NAME)).isEqualTo("internal");
    }

    @Test
    public void handlesExternalTraffic() throws Exception {
        req.addHeader("X-User-Type", "external");
        valve.invoke(context);
        InOrder inOrder = inOrder(pageCacheKey, context);
        // Sets cache key
        inOrder.verify(pageCacheKey).setAttribute(
                eq(UserTypeValve.USERTYPE_CACHE_ATTR_NAME),
                eq("external"));
        inOrder.verify(context).invokeNext();
        // Sets request attribute
        assertThat(req.getAttribute(UserTypeValve.USERTYPE_REQUEST_ATTR_NAME)).isEqualTo("external");
    }

    @Test
    public void handlesLocalDevelopmentTrafficInCargo() throws Exception {
        valve.invoke(context);
        verify(pageCacheKey, never()).setAttribute(anyString(), any());
        verifyNoInteractions(pageCacheKey);
        assertThat(req.getAttribute(UserTypeValve.USERTYPE_REQUEST_ATTR_NAME)).isNull();
    }

}
