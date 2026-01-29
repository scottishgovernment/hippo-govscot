package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class ContentSecurityPolicyValveTest {

    @Test
    public void setsCspPolicy() throws ContainerException {
        // ARRANGE
        ContentSecurityPolicyValve sut = new ContentSecurityPolicyValve();
        sut.nonceSource = () -> "nonce";
        sut.policySource = context -> "policyprefix-<nonce>-policypostfix-<nonce>";
        String expectedPolicy = "policyprefix-nonce-policypostfix-nonce";
        ValveContext valveContext = Mockito.mock(ValveContext.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(valveContext.getServletRequest()).thenReturn(request);
        when(valveContext.getServletResponse()).thenReturn(response);

        // ACT
        sut.invoke(valveContext);

        // ASSERT
        Mockito.verify(request).setAttribute(eq("nonce"), eq("nonce"));
        Mockito.verify(response).setHeader(eq("Content-Security-Policy"), eq(expectedPolicy));
    }

    @Test
    public void policyOnlyLodedOnce() throws ContainerException, IOException {
        // ARRANGE
        ContentSecurityPolicyValve sut = new ContentSecurityPolicyValve();
        sut.nonceSource = () -> "nonce";
        sut.policySource = Mockito.mock(CspPolicySource.class);
        when(sut.policySource.getCspPolicy(any())).thenReturn("policy");

        ValveContext valveContext = Mockito.mock(ValveContext.class);
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        when(valveContext.getServletRequest()).thenReturn(request);
        when(valveContext.getServletResponse()).thenReturn(response);

        // ACT
        sut.invoke(valveContext);
        sut.invoke(valveContext);

        // ASSERT
        Mockito.verify(sut.policySource, times(1)).getCspPolicy(any());
    }

    @Test(expected = ContainerException.class)
    public void containerExceptionThrowsIfPolicyNotLoaded() throws ContainerException {
        // ARRANGE
        ContentSecurityPolicyValve sut = new ContentSecurityPolicyValve();
        sut.policySource = context -> {
            throw new IOException("fail");
        };
        ValveContext valveContext = Mockito.mock(ValveContext.class);

        // ACT
        sut.invoke(valveContext);

        // ASSERT - expect exception

    }

}
