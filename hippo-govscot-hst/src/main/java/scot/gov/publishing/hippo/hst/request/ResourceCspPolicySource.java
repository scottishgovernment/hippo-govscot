package scot.gov.publishing.hippo.hst.request;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.core.container.ValveContext;


public class ResourceCspPolicySource implements CspPolicySource {

    @Override
    public String getCspPolicy(ValveContext context) throws IOException {

        ServletContext servletContext = context.getServletRequest().getServletContext();
        try (InputStream inputStream = servletContext.getResourceAsStream("/WEB-INF/csp-policy.txt")) {
            String policy = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return policy
                    .replaceAll("\\s+;", "; ")
                    .replaceAll("\\s+", " ");
        }
    }
}
