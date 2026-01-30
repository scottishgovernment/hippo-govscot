package scot.gov.publishing.hippo.hst.request;

import jakarta.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.core.container.ValveContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class ResourceCspSource implements CspSource {

    @Override
    public String getCsp(ValveContext context) throws IOException {

        ServletContext servletContext = context.getServletRequest().getServletContext();
        try (InputStream inputStream = servletContext.getResourceAsStream("/WEB-INF/csp-template.txt")) {
            String policy = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return policy
                    .replaceAll("\\s+;", "; ")
                    .replaceAll("\\s+", " ");
        }
    }
}
