package scot.gov.publishing.hippo.hst.request;

import org.hippoecm.hst.core.container.ValveContext;

import java.io.IOException;

public interface CspSource {

    String getCsp(ValveContext context) throws IOException;
}
