package scot.gov.publishing.hippo.useradmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.jaxrs.AuthorizingRepositoryJaxrsInvoker;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.onehippo.repository.jaxrs.RepositoryJaxrsService.HIPPO_REST_PERMISSION;

public class UserAdminModule extends AbstractReconfigurableDaemonModule {

    private static final String PATH = "/internal/useradmin";

    private static final String DEFAULT_ADMIN_GROUP = "cmsadmin";

    private String modulePath;

    private String adminGroup;

    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
        this.modulePath = moduleConfig.getParent().getPath();
        this.adminGroup = JcrUtils.getStringProperty(moduleConfig, "admingroup", DEFAULT_ADMIN_GROUP);
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        JAXRSInvoker invoker = new AuthorizingRepositoryJaxrsInvoker(modulePath, HIPPO_REST_PERMISSION);
        JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider(new ObjectMapper());
        RepositoryJaxrsEndpoint endpoint = new CXFRepositoryJaxrsEndpoint(PATH)
                .invoker(invoker)
                .singleton(new UserRenameResource(session, adminGroup))
                .singleton(jacksonJsonProvider);
        RepositoryJaxrsService.addEndpoint(endpoint);
    }

    @Override
    protected void doShutdown() {
        RepositoryJaxrsService.removeEndpoint(PATH);
    }
}
