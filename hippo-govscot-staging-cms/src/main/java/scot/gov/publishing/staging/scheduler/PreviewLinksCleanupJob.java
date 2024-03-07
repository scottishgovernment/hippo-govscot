package scot.gov.publishing.staging.scheduler;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.Calendar;

public class PreviewLinksCleanupJob implements RepositoryJob {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewLinksCleanupJob.class);

    private static final String CONFIG_BATCH_SIZE = "batchsize";

    private static final String PREVIEW_LINKS_QUERY = "//element(*, staging:preview) order by @staging:expirationdate";

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {
        LOG.error("Running preview links cleanup job");
        Session session = context.createSystemSession();
        try {
            long batchSize = batchSize(context);
            removeExpiredPreviewLinks(batchSize, session);
        } finally {
            session.logout();
        }
    }

    long batchSize(RepositoryJobExecutionContext context) {
        long batchSize;
        try {
            batchSize = Long.parseLong(context.getAttribute(CONFIG_BATCH_SIZE));
        } catch (NumberFormatException e) {
            LOG.warn("Incorrect batch size '{}'. Setting default to 100", context.getAttribute(CONFIG_BATCH_SIZE));
            batchSize = 100;
        }
        return batchSize;
    }

    private void removeExpiredPreviewLinks(final long batchSize, final Session session) throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(PREVIEW_LINKS_QUERY, Query.XPATH);
        query.setLimit(10000);
        NodeIterator nodes = query.execute().getNodes();
        int count = 0;

        while (nodes.hasNext()) {
            Node node = nodes.nextNode();

            LOG.error("node {}", node.getPath());
            boolean expired = removeIfExpired(node);

            if (expired) {
                count++;
            }

            if (count++ % batchSize == 0) {
                session.save();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RepositoryException("Interrupted cleaning old preview links", e);
                }
            }
        }

        if (session.hasPendingChanges()) {
            session.save();
        }
        if (count > 0) {
            LOG.error("Done cleaning {} items", count);
        } else {
            LOG.error("No timed out items");
        }
    }

    boolean removeIfExpired(Node node) throws RepositoryException {
        Calendar expirationCalendar = JcrUtils.getDateProperty(node, "staging:expirationdate", null);
        if(expirationCalendar == null || Calendar.getInstance().before(expirationCalendar)){
            return false;
        }
        LOG.info("Removing preview node at {}", node.getPath());
        node.remove();
        return true;
    }

}
