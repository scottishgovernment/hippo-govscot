package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;

import java.security.SecureRandom;
import java.util.Base64;

public class NonceValve extends AbstractOrderableValve {

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HttpServletRequest request = context.getServletRequest();
        request.setAttribute("nonce", generateNonce());
        context.invokeNext();
    }

    String generateNonce() {
        SecureRandom sr = new SecureRandom();
        byte[] nonceBytes = new byte[16];
        sr.nextBytes(nonceBytes);
        return base64Encoder.encodeToString(nonceBytes);
    }
}
