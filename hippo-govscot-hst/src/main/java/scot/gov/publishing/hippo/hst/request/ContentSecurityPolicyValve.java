package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class ContentSecurityPolicyValve extends AbstractOrderableValve {

    private static final Logger LOG = LoggerFactory.getLogger(ContentSecurityPolicyValve.class);

    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public void invoke(ValveContext valveContext) throws ContainerException {
        setCSP(valveContext);
        valveContext.invokeNext();
    }

    private void setCSP(ValveContext context) {
        HttpServletRequest request = context.getServletRequest();
        HttpServletResponse response = context.getServletResponse();

        // Generate the cryptographic nonce, and set an attribute on the request so this is available for use
        // elsewhere, e.g. in freemarker templates with schema.org inline scripts that cannot be easily externalised
        String nonce = generateNonce();
        request.setAttribute("nonce", nonce);

        // Build a CSP policy template using a text file in the resources directory
        String cspPolicyTemplate = prepareTemplate();

        // Replace the placeholder nonce with the generated one and set the CSP header
        if (cspPolicyTemplate != null) {
            String cspPolicy = cspPolicyTemplate.replace("<nonce>", nonce);
            response.setHeader("Content-Security-Policy", cspPolicy);
        }
    }

    private String prepareTemplate() {
        try (InputStream inputStream = ContentSecurityPolicyValve.class.getResourceAsStream("/cspPolicy.txt")) {
            String policy = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return policy
                    .replaceAll("\\s+;", "; ")
                    .replaceAll("\\s+", " ");
        } catch (IOException e) {
            LOG.error("Could not read CSP policy", e);
        }
        LOG.error("Could not prepare CSP template");
        return null;
    }

    private String generateNonce() {
        SecureRandom sr = new SecureRandom();
        byte[] nonceBytes = new byte[16];
        sr.nextBytes(nonceBytes);
        return base64Encoder.encodeToString(nonceBytes);
    }
}
