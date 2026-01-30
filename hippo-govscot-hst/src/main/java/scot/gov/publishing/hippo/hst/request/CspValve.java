package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

public class CspValve extends AbstractOrderableValve {

    private static final Logger LOG = LoggerFactory.getLogger(CspValve.class);

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public interface NonceSource {
        String getNonce();
    }

    String [] cspParts = null;

    NonceSource nonceSource = () -> {
        SecureRandom sr = new SecureRandom();
        byte[] nonceBytes = new byte[16];
        sr.nextBytes(nonceBytes);
        return base64Encoder.encodeToString(nonceBytes);
    };

    CspSource policySource = new ResourceCspSource();

    @Override
    public void invoke(ValveContext valveContext) throws ContainerException {
        initialize(valveContext);
        setCSP(valveContext);
        valveContext.invokeNext();
    }

    public void initialize(ValveContext valveContext) throws ContainerException {
        if (cspParts != null) {
            return;
        }

        // Gather CSP directives and entries from a template
        try {
            String csp = policySource.getCsp(valveContext);

            // We need to substitute the generated cryptographic nonce once it's generated
            cspParts = csp.split("<nonce>", -1);
        } catch (IOException e) {
            LOG.error("Failed to load CSP template", e);
            throw new ContainerException(e);
        }
    }

    private void setCSP(ValveContext context) {
        HttpServletRequest request = context.getServletRequest();
        HttpServletResponse response = context.getServletResponse();

        // Generate a cryptographic nonce, and set an attribute on the request so this is available for use
        // elsewhere, e.g. in freemarker templates with schema.org inline scripts that cannot be easily externalised
        String nonce = nonceSource.getNonce();
        request.setAttribute("nonce", nonce);

        // Add the cryptographic nonce to the CSP template and set header
        String csp = addNonceToPolicy(nonce);
        response.setHeader("Content-Security-Policy", csp);
    }

    String addNonceToPolicy(String nonce) {
        StringBuilder cspBuilder = new StringBuilder();
        for (int i = 0; i < cspParts.length; i++) {
            cspBuilder.append(cspParts[i]);
            if (i < cspParts.length - 1) {
                cspBuilder.append(nonce);
            }
        }
        return cspBuilder.toString();
    }
}
