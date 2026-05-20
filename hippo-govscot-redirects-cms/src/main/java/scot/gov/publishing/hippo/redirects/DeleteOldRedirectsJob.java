package scot.gov.publishing.hippo.redirects;

import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.gov.publishing.jcr.FeatureFlag;
import scot.gov.publishing.jcr.ScheduledJobUtils;
import scot.gov.publishing.jcr.SessionSaver;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled job that conservatively deletes the legacy redirect trees once migration to the
 * hash-bucketed layout is complete.
 *
 * <p>Three trees are removed:
 * <ul>
 *   <li>{@code /content/redirects/prgloo} — character-trie of prgloo news-archive slugs</li>
 *   <li>{@code /content/redirects/Aliases} — legacy path-mirror alias redirects</li>
 *   <li>{@code /content/redirects/HistoricalUrls} — legacy path-mirror historical URLs</li>
 * </ul>
 *
 * <p>Deletion is post-order and batched via {@link SessionSaver}: nodes are removed leaf-first
 * and saved in batches of {@value SAVE_BATCH_SIZE} with a {@value SAVE_DELAY_MS} ms pause
 * between each batch, so the repository is never overwhelmed regardless of tree size.
 *
 * <p>The job is controlled by the {@code DeleteOldRedirectsJob} feature flag. It can be enabled
 * independently of {@link MigrateAliasRedirectsJob}, allowing deletion to be scheduled at a
 * quiet time after migration has completed.
 */
public class DeleteOldRedirectsJob implements RepositoryJob {

    private static final Logger LOG = LoggerFactory.getLogger(DeleteOldRedirectsJob.class);

    static final int    SAVE_BATCH_SIZE    = 100;
    static final long   SAVE_DELAY_MS      = 1_000L;

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {
        Session session = context.createSystemSession();
        try {
            FeatureFlag flag = new FeatureFlag(session, DeleteOldRedirectsJob.class.getSimpleName());
            if (!flag.isEnabled()) {
                LOG.info("DeleteOldRedirectsJob is not enabled, skipping");
                return;
            }
            doExecute(session, flag);
        } finally {
            session.logout();
        }
    }

    void doExecute(Session session, FeatureFlag flag) throws RepositoryException {
        SessionSaver saver = new SessionSaver(session, SAVE_BATCH_SIZE, SAVE_DELAY_MS);
        try {
            deleteTree(session, MigrateAliasRedirectsJob.PRGLOO_ROOT, saver, flag);
            deleteTree(session, MigrateAliasRedirectsJob.ALIASES_ROOT, saver, flag);
            deleteTree(session, MigrateAliasRedirectsJob.HISTORICAL_ROOT, saver, flag);
        } catch (DeletionStoppedException e) {
            saver.forceSave();
            LOG.error("DeleteOldRedirectsJob: stopped early — feature flag was disabled mid-run", e);
            return;
        }
        saver.forceSave();
        completeJob(session);
        LOG.info("DeleteOldRedirectsJob: complete");
    }

    private void completeJob(Session session) throws RepositoryException {
        new FeatureFlag(session, DeleteOldRedirectsJob.class.getSimpleName()).setEnabled(false);
        ScheduledJobUtils.unscheduleJob(session, DeleteOldRedirectsJob.class.getSimpleName());
        session.save();
    }

    private void deleteTree(Session session, String path, SessionSaver saver, FeatureFlag flag) throws RepositoryException {
        if (!session.nodeExists(path)) {
            LOG.info("DeleteOldRedirectsJob: tree not present at {}, nothing to delete", path);
            return;
        }
        LOG.info("DeleteOldRedirectsJob: starting conservative deletion of tree at {}", path);
        Node root = session.getNode(path);
        int[] counter = {0};
        deleteDescendants(root, saver, flag, counter);
        root.remove();
        saver.save();
        LOG.info("DeleteOldRedirectsJob: deleted tree at {}: {} nodes removed", path, counter[0] + 1);
    }

    /**
     * Post-order removal of all descendants of {@code parent}.  Children are collected into a
     * list before removal so that the {@link NodeIterator} is not invalidated mid-walk.
     * Checks the feature flag after every batch save and throws {@link DeletionStoppedException}
     * if the flag has been disabled.
     */
    private void deleteDescendants(Node parent, SessionSaver saver, FeatureFlag flag, int[] counter) throws RepositoryException {
        List<Node> children = new ArrayList<>();
        NodeIterator it = parent.getNodes();
        while (it.hasNext()) {
            children.add(it.nextNode());
        }
        for (Node child : children) {
            deleteDescendants(child, saver, flag, counter);
            child.remove();
            boolean saved = saver.save();
            counter[0]++;
            if (saved && !flag.isEnabled()) {
                throw new DeletionStoppedException();
            }
            if (counter[0] % 500 == 0) {
                LOG.info("DeleteOldRedirectsJob: deleted {} nodes so far", counter[0]);
            }
        }
    }

    private static class DeletionStoppedException extends RuntimeException {
        DeletionStoppedException() {
            super("DeleteOldRedirectsJob stopped — feature flag disabled mid-run");
        }
    }
}
