package scot.gov.publishing.hippo.funnelback.scheduler;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Calendar;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static scot.gov.publishing.hippo.funnelback.HippoUtils.findPublished;

public class PollFunnelbackCurator implements RepositoryJob {

    private static final Logger LOG = LoggerFactory.getLogger(PollFunnelbackCurator.class);

    public static final String FUNNELBACK = "funnelback";

    private static final String HASH = "search:hash";

    private static final String CURATOR_PATH = "/content/documents/administration/funnelback-curator-changes";

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {

        if (!isComponentManagerReady()) {
            return;
        }

        Session session = context.createSystemSession();
        try {
            doExecute(context, session);
        } finally {
            session.logout();
        }
    }


    void doExecute(RepositoryJobExecutionContext context, Session session) throws RepositoryException {
        LOG.info("PollFunnelbackCurator");
        String token = HstServices.getComponentManager().getContainerConfiguration().getString("squiz.admin.token");
        if (isBlank(token)) {
            LOG.info("no token, skipping");
            return;
        }
        String collections = context.getAttribute("collections");
        LOG.info("PollFunnelbackCurator collections {}", collections);
        String storedHash = getStoredHash(session);
        String newhash = getPageHash(collections, token);
        if (!storedHash.equals(newhash)) {
            touchFunnelbackCacheFile(session, newhash);
        }
    }

    boolean isComponentManagerReady() {
        ComponentManager componentManager = HstServices.getComponentManager();
        if (componentManager == null) {
            return false;
        }

        return componentManager.getContainerConfiguration() != null;
    }

    String getStoredHash(Session session) throws RepositoryException {
        Node cacheHandle = session.getNode(CURATOR_PATH);
        Node published = findPublished(cacheHandle);
        return published != null && published.hasProperty(HASH)
                ? published.getProperty(HASH).getString()
                : "";
    }

    String getPageHash(String collections, String token) throws RepositoryException {
        try {
            StringBuilder allContent = new StringBuilder();
            for (String collection : collections.split(",")) {
                String hash = doGetPageContentHash(collection, token);
                allContent.append(hash);
            }
            return allContent.toString();
        } catch (IOException | URISyntaxException e) {
            LOG.error("arg", e);
            throw new RepositoryException(e);
        }
    }

    String doGetPageContentHash(String collection, String token) throws IOException, URISyntaxException {
        URI uri = curatorURI(collection);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(uri);
            request.addHeader("X-Security-Token", token);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                InputStream pageInputStream = response.getEntity().getContent();
                String tmp = IOUtils.toString(pageInputStream, StandardCharsets.UTF_8);
                return DigestUtils.sha1Hex(tmp);
            }
        }
    }

    URI curatorURI(String collection) throws URISyntaxException {
        String baseUrl = HstServices.getComponentManager().getContainerConfiguration().getString("squiz.admin.url");
        String clientId = HstServices.getComponentManager().getContainerConfiguration().getString("squiz.clientId");
        return new URI(String.format("%s/admin-api/curator/v2/collections/%s~sp-%s/profiles/search/curator/", baseUrl, clientId, collection));
    }

    void touchFunnelbackCacheFile(Session session, String hash) throws RepositoryException {
        Node cacheHandle = session.getNode(CURATOR_PATH);
        WorkflowManager wflManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        DocumentWorkflow workflow = (DocumentWorkflow) wflManager.getWorkflow("editing", cacheHandle);
        try {
            Document document = workflow.obtainEditableInstance();
            Node node = document.getNode(session);

            recordChange(node, hash);
            session.save();

            workflow.commitEditableInstance();
            session.save();

            workflow.publish();
            session.save();
        }  catch (RemoteException | WorkflowException e) {
            LOG.error("Failed to update funnelback-curator-changes", e);
            throw new RepositoryException(e);
        }
    }

    void recordChange(Node node, String hash) throws RepositoryException {
        node.setProperty(HASH, hash);
        node.setProperty("search:lastchangedate", Calendar.getInstance());
    }
}
