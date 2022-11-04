package scot.gov.publishing.hippo.hst.request;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static scot.gov.publishing.hippo.hst.request.CaseRedirectValve.format;

public class CaseRedirectValveTest {

    @Test
    public void delegatesLowerCaseUrlsToRequestProcessingPipeline() throws ContainerException {
        CaseRedirectValve valve = new CaseRedirectValve();
        MockHttpServletRequest request = new MockHttpServletRequest();
        ValveContext context = mock(ValveContext.class);
        when(context.getServletRequest()).thenReturn(request);
        request.setRequestURI("/fairstart");
        valve.invoke(context);
        verify(context).invokeNext();
    }

    @Test
    public void redirectsUpperCaseUrls() throws ContainerException {
        boolean[] redirected = new boolean[1];
        CaseRedirectValve valve = new CaseRedirectValve() {
            @Override
            void redirect(ValveContext context) {
                redirected[0] = true;
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        ValveContext context = mock(ValveContext.class);
        when(context.getServletRequest()).thenReturn(request);
        request.setRequestURI("/FairStart");
        valve.invoke(context);
        verify(context, never()).invokeNext();
        assertThat(redirected[0]).isTrue();
    }

    @Test
    public void formatsUrlWithoutQueryString() {
        String url = format("https://www.mygov.scot", "/FairStart", null);
        assertThat(url).isEqualTo("https://www.mygov.scot/FairStart");
    }

    @Test
    public void formatsUrlWithQueryString() {
        String url = format("https://www.mygov.scot", "/search", "q=FairStart");
        assertThat(url).isEqualTo("https://www.mygov.scot/search?q=FairStart");
    }

}
