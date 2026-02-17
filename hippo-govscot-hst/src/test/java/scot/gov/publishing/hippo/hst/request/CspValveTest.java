package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class CspValveTest {

    CspValve sut = new CspValve();

    ValveContext valveContext = mock(ValveContext.class);

    HttpServletRequest request = mock(HttpServletRequest.class);

    HttpServletResponse response = mock(HttpServletResponse.class);

    Boolean noCSPAttribute = null;

    @Before
    public void setUp() {
        when(valveContext.getServletRequest()).thenReturn(request);
        when(valveContext.getServletResponse()).thenReturn(response);
        when(request.getAttribute("NO_CSP")).thenAnswer(x -> noCSPAttribute);
    }

    @Test
    public void setsCsp() throws ContainerException {
        // ARRANGE
        sut.nonceSource = () -> "nonce";
        sut.policySource = context -> "policyprefix-<nonce>-policypostfix-<nonce>";
        String expectedPolicy = "policyprefix-nonce-policypostfix-nonce";

        // ACT
        sut.invoke(valveContext);

        // ASSERT
        verify(request).setAttribute(eq("nonce"), eq("nonce"));
        verify(response).setHeader(eq("Content-Security-Policy"), eq(expectedPolicy));
    }

    @Test
    public void policyOnlyLoadedOnce() throws ContainerException, IOException {
        // ARRANGE
        sut.nonceSource = () -> "nonce";
        sut.policySource = mock(CspSource.class);
        when(sut.policySource.getCsp(any())).thenReturn("policy");

        // ACT
        sut.invoke(valveContext);
        sut.invoke(valveContext);

        // ASSERT
        verify(sut.policySource, times(1)).getCsp(any());
    }

    @Test(expected = ContainerException.class)
    public void containerExceptionThrowsIfPolicyNotLoaded() throws ContainerException {
        // ARRANGE
        sut.policySource = context -> {
            throw new IOException("fail");
        };

        // ACT
        sut.invoke(valveContext);

        // ASSERT - expect exception
    }

    @Test
    public void noCSPHeaderIfNoCSPAttributeSet() throws ContainerException {
        // ARRANGE
        noCSPAttribute = true;

        // ACT
        sut.invoke(valveContext);

        // ASSERT
        verify(response, never()).setHeader(eq("Content-Security-Policy"), anyString());
    }

}
