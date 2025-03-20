package scot.gov.publishing.feedback.cms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.jaxrs.api.ManagedUserSessionInvoker;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class FeedbackModule extends AbstractReconfigurableDaemonModule {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackModule.class);

    private static final String PATH = "/feedback";

    private String hostGroup;

    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
        // moduleConfig refers to relevant hippo:moduleconfig node under
        // /hippo:configuration/hippo:modules
        this.hostGroup = moduleConfig.getProperty("hostgroup").getString();
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        LOG.info("Initialising feedback API");
        FeedbackResource resource = new FeedbackResource(hostGroup);
        ManagedUserSessionInvoker invoker = new ManagedUserSessionInvoker(session);
        JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(new ObjectMapper());
        RepositoryJaxrsEndpoint endpoint  = new CXFRepositoryJaxrsEndpoint(PATH)
                .invoker(invoker)
                .singleton(resource)
                .singleton(jacksonJsonProvider);
        RepositoryJaxrsService.addEndpoint(endpoint);
    }

    @Override
    protected void doShutdown() {
        RepositoryJaxrsService.removeEndpoint(PATH);
    }

}
